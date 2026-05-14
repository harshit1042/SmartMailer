package com.smartmailer.email;

import com.smartmailer.attachment.AttachmentRepository;
import com.smartmailer.auth.User;
import com.smartmailer.campaign.Campaign;
import com.smartmailer.recipient.Recipient;
import com.smartmailer.recipient.RecipientRepository;
import com.smartmailer.recipient.RecipientStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailServiceTest {
    @Test
    void queueMarksRecipientFailedWhenOwnerHasNotConnectedGmail() {
        RecipientRepository recipients = mock(RecipientRepository.class);
        AttachmentRepository attachments = mock(AttachmentRepository.class);
        EmailLogRepository logs = mock(EmailLogRepository.class);

        Recipient recipient = Recipient.builder()
                .id(10L)
                .campaign(Campaign.builder()
                        .user(User.builder()
                                .name("Harshit")
                                .email("harshit@example.com")
                                .build())
                        .build())
                .email("john@example.com")
                .renderedSubject("Hello")
                .renderedBody("Body")
                .status(RecipientStatus.APPROVED)
                .build();

        when(recipients.findById(10L)).thenReturn(Optional.of(recipient));

        EmailService service = new EmailService(
                null,
                null,
                recipients,
                attachments,
                logs,
                null,
                null,
                "smartmailer.email.jobs",
                "no-reply@example.com",
                false
        );

        service.queue(recipient);

        assertThat(recipient.getStatus()).isEqualTo(RecipientStatus.FAILED);
        assertThat(recipient.getErrorMessage()).isEqualTo("Gmail is not connected for this user");
        verify(logs).save(any(EmailLog.class));
    }
}
