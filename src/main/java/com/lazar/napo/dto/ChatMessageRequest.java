package com.lazar.napo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        Long conversationId,

        @NotBlank(message = "Message is required")
        @Size(max = 700, message = "Message must have at most 700 characters")
        String message
) {
}
