package com.smartmailer.attachment;

import com.smartmailer.campaign.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByCampaign(Campaign campaign);
    long countByCampaign(Campaign campaign);
}
