package com.lazar.napo.service;

import com.lazar.napo.entity.RecurrenceType;
import com.lazar.napo.entity.Reminder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class ReminderRecurrenceService {

    public boolean isRecurring(Reminder reminder) {
        return reminder.getRecurrenceType() != null && reminder.getRecurrenceType() != RecurrenceType.NONE;
    }

    public OffsetDateTime calculateNextReminderTime(Reminder reminder, OffsetDateTime processedUntil) {
        OffsetDateTime next = reminder.getRemindAt();

        do {
            next = switch (reminder.getRecurrenceType()) {
                case DAILY -> next.plusDays(1);
                case WEEKLY -> next.plusWeeks(1);
                case MONTHLY -> next.plusMonths(1);
                case YEARLY -> next.plusYears(1);
                case NONE -> throw new IllegalArgumentException("Reminder is not recurring");
            };
        } while (!next.isAfter(processedUntil));

        return next;
    }
}
