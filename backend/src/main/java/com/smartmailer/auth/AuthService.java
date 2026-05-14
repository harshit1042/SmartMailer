package com.smartmailer.auth;

import com.smartmailer.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.smartmailer.auth.AuthDtos.*;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = users.save(User.builder()
                .name(request.name().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .smtpHost("smtp.gmail.com")
                .smtpPort(587)
                .smtpUsername(email)
                .smtpPassword("")
                .smtpAuth(true)
                .smtpStarttls(true)
                .build());
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return response(user);
    }

    @Transactional
    public void updateGmailSettings(GmailSettingsRequest request, Authentication auth) {
        User user = managedCurrentUser(auth);
        user.setSmtpHost("smtp.gmail.com");
        user.setSmtpPort(587);
        user.setSmtpUsername(user.getEmail());
        user.setSmtpPassword(request.gmailAppPassword().replaceAll("\\s+", ""));
        user.setSmtpAuth(true);
        user.setSmtpStarttls(true);
    }

    @Transactional(readOnly = true)
    public GmailSettingsResponse gmailSettings(Authentication auth) {
        User user = managedCurrentUser(auth);
        boolean connected = user.getSmtpPassword() != null && !user.getSmtpPassword().isBlank();
        return new GmailSettingsResponse(
                connected,
                user.getSmtpUsername(),
                connected ? "****-****-****-****" : ""
        );
    }

    @Transactional(readOnly = true)
    public RevealGmailPasswordResponse revealGmailPassword(RevealGmailPasswordRequest request, Authentication auth) {
        User user = managedCurrentUser(auth);
        if (!passwordEncoder.matches(request.loginPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid login password");
        }
        if (user.getSmtpPassword() == null || user.getSmtpPassword().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Gmail App Password is not saved");
        }
        return new RevealGmailPasswordResponse(user.getSmtpPassword());
    }

    private AuthResponse response(User user) {
        return new AuthResponse(jwtService.createToken(user), user.getId(), user.getName(), user.getEmail());
    }

    private User currentUser(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return principal.user();
    }

    private User managedCurrentUser(Authentication auth) {
        User authenticated = currentUser(auth);
        return users.findByEmail(authenticated.getEmail())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }
}
