package com.lazar.napo.dto;

import com.lazar.napo.entity.NotificationChannel;
import com.lazar.napo.entity.RecurrenceType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateReminderRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must have at most 150 characters")
        String title,

        @Size(max = 2000, message = "Description must have at most 2000 characters")
        String description,

        @NotNull(message = "Reminder date and time is required")
        @Future(message = "Reminder date and time must be in the future")
        OffsetDateTime remindAt,

        @Size(max = 64, message = "Timezone must have at most 64 characters")
        String timezone,

        RecurrenceType recurrenceType,

        @Size(max = 500, message = "Recurrence rule must have at most 500 characters")
        String recurrenceRule,

        NotificationChannel preferredChannel,

        @Size(max = 2000, message = "Message must have at most 2000 characters")
        String messageToSend
) {
}
