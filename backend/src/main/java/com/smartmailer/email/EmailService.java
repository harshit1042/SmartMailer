package com.smartmailer.email;

import com.smartmailer.campaign.Campaign;
import com.smartmailer.campaign.CampaignService;
import com.smartmailer.campaign.CampaignStatus;
import com.smartmailer.common.ApiException;
import com.smartmailer.auth.User;
import com.smartmailer.attachment.Attachment;
import com.smartmailer.attachment.AttachmentRepository;
import com.smartmailer.recipient.Recipient;
import com.smartmailer.recipient.RecipientRepository;
import com.smartmailer.recipient.RecipientService;
import com.smartmailer.recipient.RecipientStatus;
import jakarta.mail.internet.MimeMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailService {
    private final CampaignService campaigns;
    private final RecipientService recipientService;
    private final RecipientRepository recipients;
    private final AttachmentRepository attachments;
    private final EmailLogRepository logs;
    private final RabbitTemplate rabbitTemplate;
    private final JavaMailSender mailSender;
    private final String queueName;
    private final String from;
    private final boolean queueEnabled;
    private final Set<String> idempotencyKeys = ConcurrentHashMap.newKeySet();

    public EmailService(
            CampaignService campaigns,
            RecipientService recipientService,
            RecipientRepository recipients,
            AttachmentRepository attachments,
            EmailLogRepository logs,
            RabbitTemplate rabbitTemplate,
            JavaMailSender mailSender,
            @Value("${app.email.queue}") String queueName,
            @Value("${app.email.from}") String from,
            @Value("${app.email.queue-enabled:false}") boolean queueEnabled
    ) {
        this.campaigns = campaigns;
        this.recipientService = recipientService;
        this.recipients = recipients;
        this.attachments = attachments;
        this.logs = logs;
        this.rabbitTemplate = rabbitTemplate;
        this.mailSender = mailSender;
        this.queueName = queueName;
        this.from = from;
        this.queueEnabled = queueEnabled;
    }

    @Transactional
    public int sendApproved(Long campaignId, Authentication auth) {
        Campaign campaign = campaigns.requireCampaign(campaignId, auth);
        List<Recipient> approved = recipients.findByCampaignAndStatus(campaign, RecipientStatus.APPROVED);
        approved.forEach(this::queue);
        campaign.setStatus(CampaignStatus.ACTIVE);
        return approved.size();
    }

    @Transactional
    public void sendOne(Long recipientId, Authentication auth) {
        Recipient recipient = recipientService.requireOwnedRecipient(recipientId, auth);
        if (recipient.getStatus() == RecipientStatus.DELETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Deleted recipients cannot be sent");
        }
        queue(recipient);
    }

    @Transactional
    public int retryFailed(Long campaignId, Authentication auth) {
        Campaign campaign = campaigns.requireCampaign(campaignId, auth);
        List<Recipient> failed = recipients.findByCampaignAndStatus(campaign, RecipientStatus.FAILED);
        failed.forEach(this::queue);
        return failed.size();
    }

    public void queue(Recipient recipient) {
        recipient.setStatus(RecipientStatus.SENDING);
        recipient.getCampaign().setStatus(CampaignStatus.ACTIVE);
        String key = "recipient-send-" + recipient.getId() + "-" + Instant.now().toEpochMilli();
        if (!queueEnabled) {
            process(new EmailJob(recipient.getId(), key));
            return;
        }
        try {
            rabbitTemplate.convertAndSend(queueName, new EmailJob(recipient.getId(), key));
        } catch (RuntimeException ex) {
            process(new EmailJob(recipient.getId(), key));
        }
    }

    @Transactional
    public void process(EmailJob job) {
        if (!idempotencyKeys.add(job.idempotencyKey())) {
            return;
        }
        Recipient recipient = recipients.findById(job.recipientId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipient not found"));
        try {
            User owner = recipient.getCampaign().getUser();
            JavaMailSender ownerMailSender = mailSenderFor(owner);
            MimeMessage message = ownerMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            String senderEmail = owner.getEmail() == null || owner.getEmail().isBlank() ? from : owner.getEmail();
            String senderName = owner.getName() == null || owner.getName().isBlank() ? senderEmail : owner.getName();
            helper.setFrom(senderEmail, senderName);
            helper.setReplyTo(senderEmail);
            helper.setTo(recipient.getEmail());
            helper.setSubject(recipient.getRenderedSubject());
            helper.setText(recipient.getRenderedBody(), false);
            addAttachments(helper, recipient);
            ownerMailSender.send(message);
            recipient.setStatus(RecipientStatus.SENT);
            recipient.setSentAt(Instant.now());
            recipient.setErrorMessage(null);
            logs.save(EmailLog.builder()
                    .recipient(recipient)
                    .status(EmailLogStatus.SENT)
                    .providerResponse("Sent via configured JavaMailSender")
                    .build());
        } catch (Exception ex) {
            recipient.setStatus(RecipientStatus.FAILED);
            recipient.setErrorMessage(ex.getMessage());
            logs.save(EmailLog.builder()
                    .recipient(recipient)
                    .status(EmailLogStatus.FAILED)
                    .providerResponse(ex.getMessage())
                    .build());
        } finally {
            refreshCampaignStatus(recipient.getCampaign());
        }
    }

    private void refreshCampaignStatus(Campaign campaign) {
        long waiting = recipients.countByCampaignAndStatus(campaign, RecipientStatus.PENDING)
                + recipients.countByCampaignAndStatus(campaign, RecipientStatus.APPROVED)
                + recipients.countByCampaignAndStatus(campaign, RecipientStatus.SENDING)
                + recipients.countByCampaignAndStatus(campaign, RecipientStatus.SCHEDULED);
        long sent = recipients.countByCampaignAndStatus(campaign, RecipientStatus.SENT);
        long failed = recipients.countByCampaignAndStatus(campaign, RecipientStatus.FAILED);

        if (waiting == 0 && sent > 0 && failed == 0) {
            campaign.setStatus(CampaignStatus.COMPLETED);
            return;
        }
        if (sent > 0 || failed > 0 || waiting > 0) {
            campaign.setStatus(CampaignStatus.ACTIVE);
        }
    }

    private void addAttachments(MimeMessageHelper helper, Recipient recipient) throws Exception {
        for (Attachment attachment : attachments.findByCampaign(recipient.getCampaign())) {
            Path path = Path.of(attachment.getFileUrl());
            if (!Files.isRegularFile(path)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Attachment file not found: " + attachment.getFileName());
            }
            helper.addAttachment(attachment.getFileName(), new FileSystemResource(path));
        }
    }

    private JavaMailSender mailSenderFor(User user) {
        if (user.getSmtpUsername() == null || user.getSmtpUsername().isBlank()
                || user.getSmtpPassword() == null || user.getSmtpPassword().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Gmail is not connected for this user");
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(user.getSmtpHost());
        sender.setPort(user.getSmtpPort());
        sender.setUsername(user.getSmtpUsername());
        sender.setPassword(user.getSmtpPassword());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(user.getSmtpAuth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(user.getSmtpStarttls()));
        props.put("mail.smtp.starttls.required", String.valueOf(user.getSmtpStarttls()));
        return sender;
    }
}
