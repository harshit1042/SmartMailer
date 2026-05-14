package com.smartmailer.recipient;

import com.smartmailer.campaign.Campaign;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recipients")
public class Recipient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Column(length = 100)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(length = 150)
    private String company;

    @Column(length = 150)
    private String role;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_data_json", columnDefinition = "json")
    private Map<String, String> customDataJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RecipientStatus status;

    @Lob
    @Column(name = "rendered_subject")
    private String renderedSubject;

    @Lob
    @Column(name = "rendered_body")
    private String renderedBody;

    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "sent_at")
    private Instant sentAt;

    @PrePersist
    void onCreate() {
        if (status == null) {
            status = RecipientStatus.PENDING;
        }
    }
}
