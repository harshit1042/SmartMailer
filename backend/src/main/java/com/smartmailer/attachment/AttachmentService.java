package com.smartmailer.attachment;

import com.smartmailer.campaign.Campaign;
import com.smartmailer.campaign.CampaignService;
import com.smartmailer.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.smartmailer.attachment.AttachmentDtos.AttachmentResponse;

@Service
public class AttachmentService {
    private final CampaignService campaigns;
    private final AttachmentRepository attachments;
    private final Path uploadDir;

    public AttachmentService(CampaignService campaigns, AttachmentRepository attachments, @Value("${app.uploads.directory}") String uploadDir) {
        this.campaigns = campaigns;
        this.attachments = attachments;
        this.uploadDir = Path.of(uploadDir);
    }

    @Transactional
    public List<AttachmentResponse> upload(Long campaignId, MultipartFile[] files, Authentication auth) {
        Campaign campaign = campaigns.requireCampaign(campaignId, auth);
        if (files == null || files.length == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select at least one attachment");
        }
        if (attachments.countByCampaign(campaign) + files.length > 5) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A campaign can have at most 5 attachments");
        }
        return List.of(files).stream().map(file -> save(campaign, file)).toList();
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(Long campaignId, Authentication auth) {
        Campaign campaign = campaigns.requireCampaign(campaignId, auth);
        return attachments.findByCampaign(campaign).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AttachmentDownload download(Long campaignId, Long attachmentId, Authentication auth) {
        Campaign campaign = campaigns.requireCampaign(campaignId, auth);
        Attachment attachment = attachments.findById(attachmentId)
                .filter(item -> item.getCampaign().getId().equals(campaign.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attachment not found"));
        Path path = Path.of(attachment.getFileUrl());
        if (!Files.isRegularFile(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Attachment file not found");
        }
        try {
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = attachment.getFileName().toLowerCase(Locale.ROOT).endsWith(".pdf")
                        ? "application/pdf"
                        : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            }
            return new AttachmentDownload(attachment.getFileName(), contentType, Files.size(path), new FileSystemResource(path));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read attachment");
        }
    }

    @Transactional
    public void delete(Long campaignId, Long attachmentId, Authentication auth) {
        Campaign campaign = campaigns.requireCampaign(campaignId, auth);
        Attachment attachment = attachments.findById(attachmentId)
                .filter(item -> item.getCampaign().getId().equals(campaign.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attachment not found"));
        Path path = Path.of(attachment.getFileUrl());
        attachments.delete(attachment);
        try {
            Files.deleteIfExists(path);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to delete attachment file");
        }
    }

    private AttachmentResponse save(Campaign campaign, MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "attachment" : Path.of(file.getOriginalFilename()).getFileName().toString();
        String lower = filename.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".pdf") && !lower.endsWith(".docx")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only PDF and DOCX attachments are allowed");
        }
        try {
            Path campaignDir = uploadDir.resolve(campaign.getId().toString()).toAbsolutePath().normalize();
            Files.createDirectories(campaignDir);
            Path target = campaignDir.resolve(UUID.randomUUID() + "-" + filename).normalize();
            try (var input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return toResponse(attachments.save(Attachment.builder()
                    .campaign(campaign)
                    .fileName(filename)
                    .fileUrl(target.toString())
                    .build()));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store attachment: " + ex.getMessage());
        }
    }

    private AttachmentResponse toResponse(Attachment attachment) {
        return new AttachmentResponse(attachment.getId(), attachment.getFileName(), attachment.getFileUrl());
    }

    public record AttachmentDownload(String fileName, String contentType, long size, Resource resource) {
    }
}
