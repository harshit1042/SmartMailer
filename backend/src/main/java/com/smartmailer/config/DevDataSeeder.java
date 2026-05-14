package com.smartmailer.config;

import com.smartmailer.auth.User;
import com.smartmailer.auth.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DevDataSeeder {
    @Bean
    CommandLineRunner seedUser(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (!users.existsByEmail("harshit@example.com")) {
                users.save(User.builder()
                        .name("Harshit Chodvadiya")
                        .email("harshit@example.com")
                        .passwordHash(encoder.encode("password123"))
                        .smtpHost("smtp.gmail.com")
                        .smtpPort(587)
                        .smtpUsername("harshit@example.com")
                        .smtpPassword("")
                        .smtpAuth(true)
                        .smtpStarttls(true)
                        .build());
            }
        };
    }
}
