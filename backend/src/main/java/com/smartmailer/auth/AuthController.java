package com.smartmailer.auth;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.smartmailer.auth.AuthDtos.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/gmail-settings")
    void updateGmailSettings(@Valid @RequestBody GmailSettingsRequest request, Authentication auth) {
        authService.updateGmailSettings(request, auth);
    }

    @GetMapping("/gmail-settings")
    GmailSettingsResponse gmailSettings(Authentication auth) {
        return authService.gmailSettings(auth);
    }

    @PostMapping("/gmail-settings/reveal")
    RevealGmailPasswordResponse revealGmailPassword(@Valid @RequestBody RevealGmailPasswordRequest request, Authentication auth) {
        return authService.revealGmailPassword(request, auth);
    }
}
