package com.smartmailer.recipient;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.smartmailer.recipient.RecipientDtos.*;

@RestController
public class RecipientController {
    private final RecipientService service;

    public RecipientController(RecipientService service) {
        this.service = service;
    }

    @GetMapping("/api/campaigns/{campaignId}/recipients")
    PageResponse<RecipientResponse> page(
            @PathVariable Long campaignId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth
    ) {
        return service.page(campaignId, page, size, auth);
    }

    @PutMapping("/api/recipients/{id}")
    RecipientResponse update(@PathVariable Long id, @Valid @RequestBody RecipientUpdateRequest request, Authentication auth) {
        return service.update(id, request, auth);
    }

    @DeleteMapping("/api/recipients/{id}")
    void delete(@PathVariable Long id, Authentication auth) {
        service.delete(id, auth);
    }

    @PostMapping("/api/recipients/{id}/restore")
    RecipientResponse restore(@PathVariable Long id, Authentication auth) {
        return service.restore(id, auth);
    }

    @PostMapping("/api/recipients/{id}/approve")
    RecipientResponse approve(@PathVariable Long id, Authentication auth) {
        return service.approve(id, auth);
    }
}
