package com.smartmailer.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "smtp_host", nullable = false)
    private String smtpHost;

    @Column(name = "smtp_port", nullable = false)
    private Integer smtpPort;

    @Column(name = "smtp_username")
    private String smtpUsername;

    @Column(name = "smtp_password")
    private String smtpPassword;

    @Column(name = "smtp_auth", nullable = false)
    private Boolean smtpAuth;

    @Column(name = "smtp_starttls", nullable = false)
    private Boolean smtpStarttls;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (smtpHost == null || smtpHost.isBlank()) {
            smtpHost = "smtp.gmail.com";
        }
        if (smtpPort == null) {
            smtpPort = 587;
        }
        if (smtpAuth == null) {
            smtpAuth = true;
        }
        if (smtpStarttls == null) {
            smtpStarttls = true;
        }
    }
}
