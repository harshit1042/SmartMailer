package com.smartmailer.recipient;

import com.smartmailer.campaign.Campaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipientRepository extends JpaRepository<Recipient, Long> {
    Page<Recipient> findByCampaignOrderByIdAsc(Campaign campaign, Pageable pageable);
    long countByCampaignAndStatus(Campaign campaign, RecipientStatus status);
    List<Recipient> findByCampaignAndStatus(Campaign campaign, RecipientStatus status);
    boolean existsByCampaignAndEmail(Campaign campaign, String email);
}
