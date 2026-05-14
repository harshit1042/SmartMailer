package com.smartmailer.campaign;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.smartmailer.campaign.CampaignDtos.*;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {
    private final CampaignService service;

    public CampaignController(CampaignService service) {
        this.service = service;
    }

    @PostMapping
    CampaignResponse create(@Valid @RequestBody CampaignRequest request, Authentication auth) {
        return service.create(request, auth);
    }

    @GetMapping
    List<CampaignResponse> list(Authentication auth) {
        return service.list(auth);
    }

    @GetMapping("/{id}")
    CampaignResponse detail(@PathVariable Long id, Authentication auth) {
        return service.detail(id, auth);
    }

    @PutMapping("/{id}")
    CampaignResponse update(@PathVariable Long id, @Valid @RequestBody CampaignRequest request, Authentication auth) {
        return service.update(id, request, auth);
    }

    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id, Authentication auth) {
        service.delete(id, auth);
    }

    @GetMapping("/dashboard")
    DashboardStats dashboard(Authentication auth) {
        return service.dashboard(auth);
    }
}
