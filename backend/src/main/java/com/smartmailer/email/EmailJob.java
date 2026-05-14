package com.smartmailer.email;

public record EmailJob(Long recipientId, String idempotencyKey) {
}
