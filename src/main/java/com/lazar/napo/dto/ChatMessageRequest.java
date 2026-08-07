package com.lazar.napo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        Long conversationId,

        @NotBlank(message = "Message is required")
        @Size(max = 4000, message = "Message must have at most 4000 characters")
        String message
) {
}
