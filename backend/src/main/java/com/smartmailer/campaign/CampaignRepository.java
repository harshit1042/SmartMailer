package com.smartmailer.campaign;

import com.smartmailer.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByUserAndStatusNotOrderByCreatedAtDesc(User user, CampaignStatus status);
    Optional<Campaign> findByIdAndUser(Long id, User user);
}
