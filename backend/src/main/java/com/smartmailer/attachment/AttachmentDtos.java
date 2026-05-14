package com.smartmailer.attachment;

public final class AttachmentDtos {
    private AttachmentDtos() {
    }

    public record AttachmentResponse(Long id, String fileName, String fileUrl) {
    }
}
