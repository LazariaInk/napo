package com.lazar.napo.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ProcessDueRemindersResponse(
        OffsetDateTime processedUntil,
        int processedCount,
        List<ProcessedReminderResponse> reminders
) {
}
