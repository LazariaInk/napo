package com.lazar.napo.dto;

import com.lazar.napo.entity.NotificationChannel;
import com.lazar.napo.entity.ReminderStatus;

import java.time.OffsetDateTime;

public record ProcessedReminderResponse(
        Long reminderId,
        String title,
        OffsetDateTime remindAt,
        OffsetDateTime nextRemindAt,
        ReminderStatus status,
        NotificationChannel channel,
        Long attemptId
) {
}
