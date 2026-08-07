package com.lazar.napo.dto;

public record ChatMessageResponse(
        Long conversationId,
        ChatResponseType type,
        String assistantMessage,
        ReminderResponse reminder
) {
}
