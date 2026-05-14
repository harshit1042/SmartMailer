package com.smartmailer.campaign;

import com.smartmailer.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "campaigns")
public class Campaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "campaign_name", nullable = false, length = 200)
    private String campaignName;

    @Lob
    @Column(name = "subject_template", nullable = false)
    private String subjectTemplate;

    @Lob
    @Column(name = "body_template", nullable = false)
    private String bodyTemplate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CampaignStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = CampaignStatus.DRAFT;
        }
    }
}
