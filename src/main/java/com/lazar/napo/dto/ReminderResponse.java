package com.lazar.napo.dto;

import com.lazar.napo.entity.NotificationChannel;
import com.lazar.napo.entity.RecurrenceType;
import com.lazar.napo.entity.ReminderStatus;

import java.time.OffsetDateTime;

public record ReminderResponse(
        Long id,
        Long userId,
        String title,
        String description,
        OffsetDateTime remindAt,
        String timezone,
        ReminderStatus status,
        RecurrenceType recurrenceType,
        String recurrenceRule,
        NotificationChannel preferredChannel,
        String messageToSend,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
