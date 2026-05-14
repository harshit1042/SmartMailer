package com.smartmailer.recipient;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class RecipientDtos {
    private RecipientDtos() {
    }

    public record RecipientResponse(
            Long id,
            String name,
            String email,
            String company,
            String role,
            Map<String, String> customDataJson,
            RecipientStatus status,
            String renderedSubject,
            String renderedBody,
            String errorMessage,
            Instant sentAt
    ) {
    }

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }

    public record RecipientUpdateRequest(
            String name,
            @NotBlank @Email String email,
            String company,
            String role,
            Map<String, String> customDataJson,
            String renderedSubject,
            String renderedBody
    ) {
    }

    public record UploadError(int rowNumber, String email, String message) {
    }

    public record UploadSummary(int totalParsed, int totalErrors, List<UploadError> errors) {
    }
}
