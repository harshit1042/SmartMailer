package com.smartmailer.campaign;

import com.smartmailer.recipient.RecipientRepository;
import com.smartmailer.recipient.RecipientStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

import static com.smartmailer.campaign.CampaignDtos.CampaignResponse;

@Component
public class CampaignMapper {
    private final RecipientRepository recipients;

    public CampaignMapper(RecipientRepository recipients) {
        this.recipients = recipients;
    }

    public CampaignResponse toResponse(Campaign campaign) {
        Map<RecipientStatus, Long> counts = new EnumMap<>(RecipientStatus.class);
        for (RecipientStatus status : RecipientStatus.values()) {
            counts.put(status, recipients.countByCampaignAndStatus(campaign, status));
        }
        return new CampaignResponse(
                campaign.getId(),
                campaign.getCampaignName(),
                campaign.getSubjectTemplate(),
                campaign.getBodyTemplate(),
                campaign.getStatus(),
                campaign.getCreatedAt(),
                counts
        );
    }
}
