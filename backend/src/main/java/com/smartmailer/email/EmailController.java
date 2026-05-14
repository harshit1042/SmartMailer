package com.smartmailer.email;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class EmailController {
    private final EmailService service;

    public EmailController(EmailService service) {
        this.service = service;
    }

    @PostMapping("/api/campaigns/{campaignId}/send-approved")
    Map<String, Integer> sendApproved(@PathVariable Long campaignId, Authentication auth) {
        return Map.of("queued", service.sendApproved(campaignId, auth));
    }

    @PostMapping("/api/recipients/{recipientId}/send")
    void sendOne(@PathVariable Long recipientId, Authentication auth) {
        service.sendOne(recipientId, auth);
    }

    @PostMapping("/api/campaigns/{campaignId}/retry-failed")
    Map<String, Integer> retryFailed(@PathVariable Long campaignId, Authentication auth) {
        return Map.of("queued", service.retryFailed(campaignId, auth));
    }
}
