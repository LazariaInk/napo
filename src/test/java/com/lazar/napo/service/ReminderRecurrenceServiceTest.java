package com.lazar.napo.service;

import com.lazar.napo.entity.RecurrenceType;
import com.lazar.napo.entity.Reminder;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReminderRecurrenceServiceTest {

    private final ReminderRecurrenceService recurrenceService = new ReminderRecurrenceService();

    @Test
    void returnsFalseForNonRecurringReminder() {
        Reminder reminder = reminder(RecurrenceType.NONE, "2026-08-23T10:00:00+03:00");

        assertThat(recurrenceService.isRecurring(reminder)).isFalse();
    }

    @Test
    void calculatesNextYearlyExecution() {
        Reminder reminder = reminder(RecurrenceType.YEARLY, "2026-08-23T10:00:00+03:00");

        OffsetDateTime next = recurrenceService.calculateNextReminderTime(
                reminder,
                OffsetDateTime.parse("2026-08-23T10:01:00+03:00")
        );

        assertThat(next).isEqualTo(OffsetDateTime.parse("2027-08-23T10:00:00+03:00"));
    }

    @Test
    void skipsDailyExecutionsUntilNextFutureSlot() {
        Reminder reminder = reminder(RecurrenceType.DAILY, "2026-08-01T09:00:00+03:00");

        OffsetDateTime next = recurrenceService.calculateNextReminderTime(
                reminder,
                OffsetDateTime.parse("2026-08-05T09:30:00+03:00")
        );

        assertThat(next).isEqualTo(OffsetDateTime.parse("2026-08-06T09:00:00+03:00"));
    }

    @Test
    void rejectsNonRecurringReminderForNextExecutionCalculation() {
        Reminder reminder = reminder(RecurrenceType.NONE, "2026-08-23T10:00:00+03:00");

        assertThatThrownBy(() -> recurrenceService.calculateNextReminderTime(
                reminder,
                OffsetDateTime.parse("2026-08-23T10:01:00+03:00")
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private Reminder reminder(RecurrenceType recurrenceType, String remindAt) {
        Reminder reminder = new Reminder();
        reminder.setRecurrenceType(recurrenceType);
        reminder.setRemindAt(OffsetDateTime.parse(remindAt));
        return reminder;
    }
}
