package com.lazar.napo.dto;

import com.lazar.napo.entity.NotificationAttemptStatus;
import com.lazar.napo.entity.NotificationChannel;

import java.time.OffsetDateTime;

public record NotificationAttemptResponse(
        Long id,
        Long reminderId,
        String reminderTitle,
        NotificationChannel channel,
        NotificationAttemptStatus status,
        OffsetDateTime attemptedAt,
        String providerMessageId,
        String errorMessage
) {
}
