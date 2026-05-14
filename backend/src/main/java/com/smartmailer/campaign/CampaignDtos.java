package com.smartmailer.campaign;

import com.smartmailer.recipient.RecipientStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.Map;

public final class CampaignDtos {
    private CampaignDtos() {
    }

    public record CampaignRequest(
            @NotBlank String campaignName,
            @NotBlank String subjectTemplate,
            @NotBlank String bodyTemplate
    ) {
    }

    public record CampaignResponse(
            Long id,
            String campaignName,
            String subjectTemplate,
            String bodyTemplate,
            CampaignStatus status,
            Instant createdAt,
            Map<RecipientStatus, Long> counts
    ) {
    }

    public record DashboardStats(
            long campaigns,
            long recipients,
            long sent,
            long failed,
            long pending,
            long deleted,
            double successRate
    ) {
    }
}
