package com.smartmailer.attachment;

import org.springframework.security.core.Authentication;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.smartmailer.attachment.AttachmentDtos.AttachmentResponse;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/attachments")
public class AttachmentController {
    private final AttachmentService service;

    public AttachmentController(AttachmentService service) {
        this.service = service;
    }

    @PostMapping
    List<AttachmentResponse> upload(@PathVariable Long campaignId, @RequestParam("files") MultipartFile[] files, Authentication auth) {
        return service.upload(campaignId, files, auth);
    }

    @GetMapping
    List<AttachmentResponse> list(@PathVariable Long campaignId, Authentication auth) {
        return service.list(campaignId, auth);
    }

    @GetMapping("/{attachmentId}/download")
    ResponseEntity<Resource> download(@PathVariable Long campaignId, @PathVariable Long attachmentId, Authentication auth) {
        var download = service.download(campaignId, attachmentId, auth);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + download.fileName() + "\"")
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.size())
                .body(download.resource());
    }

    @DeleteMapping("/{attachmentId}")
    void delete(@PathVariable Long campaignId, @PathVariable Long attachmentId, Authentication auth) {
        service.delete(campaignId, attachmentId, auth);
    }
}
