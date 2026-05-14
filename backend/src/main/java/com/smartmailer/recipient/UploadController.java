package com.smartmailer.recipient;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static com.smartmailer.recipient.RecipientDtos.UploadSummary;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/upload")
public class UploadController {
    private final RecipientService service;

    public UploadController(RecipientService service) {
        this.service = service;
    }

    @PostMapping
    UploadSummary upload(@PathVariable Long campaignId, @RequestParam("file") MultipartFile file, Authentication auth) {
        return service.upload(campaignId, file, auth);
    }
}
