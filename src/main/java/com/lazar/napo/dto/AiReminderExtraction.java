package com.lazar.napo.dto;

public record AiReminderExtraction(
        String action,
        String assistantMessage,
        String title,
        String description,
        String remindAt,
        String timezone,
        String recurrenceType,
        String preferredChannel,
        String messageToSend,
        String missingInformation
) {
}
