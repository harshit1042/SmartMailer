package com.smartmailer.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record AuthResponse(
            String token,
            Long userId,
            String name,
            String email
    ) {
    }

    public record GmailSettingsRequest(
            @NotBlank @Size(min = 8, max = 100) String gmailAppPassword
    ) {
    }

    public record GmailSettingsResponse(
            boolean connected,
            String smtpUsername,
            String maskedAppPassword
    ) {
    }

    public record RevealGmailPasswordRequest(
            @NotBlank String loginPassword
    ) {
    }

    public record RevealGmailPasswordResponse(
            String gmailAppPassword
    ) {
    }
}
