package com.smartmailer.campaign;

import com.smartmailer.auth.User;
import com.smartmailer.auth.UserPrincipal;
import com.smartmailer.common.ApiException;
import com.smartmailer.recipient.RecipientRepository;
import com.smartmailer.recipient.RecipientStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.smartmailer.campaign.CampaignDtos.*;

@Service
public class CampaignService {
    private final CampaignRepository campaigns;
    private final RecipientRepository recipients;
    private final CampaignMapper mapper;

    public CampaignService(CampaignRepository campaigns, RecipientRepository recipients, CampaignMapper mapper) {
        this.campaigns = campaigns;
        this.recipients = recipients;
        this.mapper = mapper;
    }

    @Transactional
    public CampaignResponse create(CampaignRequest request, Authentication auth) {
        Campaign campaign = campaigns.save(Campaign.builder()
                .user(currentUser(auth))
                .campaignName(request.campaignName().trim())
                .subjectTemplate(request.subjectTemplate())
                .bodyTemplate(request.bodyTemplate())
                .status(CampaignStatus.DRAFT)
                .build());
        return mapper.toResponse(campaign);
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> list(Authentication auth) {
        return campaigns.findByUserAndStatusNotOrderByCreatedAtDesc(currentUser(auth), CampaignStatus.DELETED)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CampaignResponse detail(Long id, Authentication auth) {
        return mapper.toResponse(requireCampaign(id, auth));
    }

    @Transactional
    public CampaignResponse update(Long id, CampaignRequest request, Authentication auth) {
        Campaign campaign = requireCampaign(id, auth);
        campaign.setCampaignName(request.campaignName().trim());
        campaign.setSubjectTemplate(request.subjectTemplate());
        campaign.setBodyTemplate(request.bodyTemplate());
        return mapper.toResponse(campaign);
    }

    @Transactional
    public void delete(Long id, Authentication auth) {
        Campaign campaign = requireCampaign(id, auth);
        campaign.setStatus(CampaignStatus.DELETED);
    }

    @Transactional(readOnly = true)
    public DashboardStats dashboard(Authentication auth) {
        List<Campaign> owned = campaigns.findByUserAndStatusNotOrderByCreatedAtDesc(currentUser(auth), CampaignStatus.DELETED);
        long totalRecipients = 0;
        long sent = 0;
        long failed = 0;
        long pending = 0;
        long deleted = 0;
        for (Campaign campaign : owned) {
            sent += recipients.countByCampaignAndStatus(campaign, RecipientStatus.SENT);
            failed += recipients.countByCampaignAndStatus(campaign, RecipientStatus.FAILED);
            pending += recipients.countByCampaignAndStatus(campaign, RecipientStatus.PENDING);
            deleted += recipients.countByCampaignAndStatus(campaign, RecipientStatus.DELETED);
        }
        totalRecipients = sent + failed + pending + deleted;
        double successRate = sent + failed == 0 ? 0 : (sent * 100.0) / (sent + failed);
        return new DashboardStats(owned.size(), totalRecipients, sent, failed, pending, deleted, successRate);
    }

    public Campaign requireCampaign(Long id, Authentication auth) {
        return campaigns.findByIdAndUser(id, currentUser(auth))
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Campaign not found for current user"));
    }

    public User currentUser(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return principal.user();
    }
}
