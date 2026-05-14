package com.smartmailer.recipient;

import org.springframework.stereotype.Component;

import static com.smartmailer.recipient.RecipientDtos.RecipientResponse;

@Component
public class RecipientMapper {
    public RecipientResponse toResponse(Recipient recipient) {
        return new RecipientResponse(
                recipient.getId(),
                recipient.getName(),
                recipient.getEmail(),
                recipient.getCompany(),
                recipient.getRole(),
                recipient.getCustomDataJson(),
                recipient.getStatus(),
                recipient.getRenderedSubject(),
                recipient.getRenderedBody(),
                recipient.getErrorMessage(),
                recipient.getSentAt()
        );
    }
}
