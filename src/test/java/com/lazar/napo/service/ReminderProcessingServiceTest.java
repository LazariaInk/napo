package com.lazar.napo.service;

import com.lazar.napo.dto.ProcessDueRemindersResponse;
import com.lazar.napo.entity.NotificationAttemptStatus;
import com.lazar.napo.entity.NotificationChannel;
import com.lazar.napo.entity.RecurrenceType;
import com.lazar.napo.entity.Reminder;
import com.lazar.napo.entity.ReminderNotificationAttempt;
import com.lazar.napo.entity.ReminderStatus;
import com.lazar.napo.entity.UserAccount;
import com.lazar.napo.repository.ReminderNotificationAttemptRepository;
import com.lazar.napo.repository.ReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderProcessingServiceTest {

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private ReminderNotificationAttemptRepository attemptRepository;

    private ReminderProcessingService processingService;

    @BeforeEach
    void setUp() {
        processingService = new ReminderProcessingService(
                reminderRepository,
                attemptRepository,
                new ReminderRecurrenceService()
        );
    }

    @Test
    void completesNonRecurringReminderAfterMockNotification() {
        OffsetDateTime processUntil = OffsetDateTime.parse("2026-08-23T10:01:00+03:00");
        Reminder reminder = reminder(1L, RecurrenceType.NONE, "2026-08-23T10:00:00+03:00");
        ReminderNotificationAttempt savedAttempt = attempt(99L, reminder);

        when(reminderRepository.findByStatusAndRemindAtLessThanEqualOrderByRemindAtAsc(
                ReminderStatus.PENDING,
                processUntil
        )).thenReturn(List.of(reminder));
        when(attemptRepository.save(any(ReminderNotificationAttempt.class))).thenReturn(savedAttempt);
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessDueRemindersResponse response = processingService.processDueReminders(processUntil);

        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.COMPLETED);
        assertThat(reminder.getRemindAt()).isEqualTo(OffsetDateTime.parse("2026-08-23T10:00:00+03:00"));
        assertThat(response.processedCount()).isEqualTo(1);
        assertThat(response.reminders().getFirst().nextRemindAt()).isNull();
        assertThat(response.reminders().getFirst().status()).isEqualTo(ReminderStatus.COMPLETED);

        ArgumentCaptor<ReminderNotificationAttempt> attemptCaptor =
                ArgumentCaptor.forClass(ReminderNotificationAttempt.class);
        verify(attemptRepository).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().getStatus()).isEqualTo(NotificationAttemptStatus.SUCCESS);
        assertThat(attemptCaptor.getValue().getChannel()).isEqualTo(NotificationChannel.LOG);
    }

    @Test
    void reschedulesRecurringReminderAndKeepsItPending() {
        OffsetDateTime processUntil = OffsetDateTime.parse("2026-08-23T10:01:00+03:00");
        Reminder reminder = reminder(2L, RecurrenceType.YEARLY, "2026-08-23T10:00:00+03:00");
        ReminderNotificationAttempt savedAttempt = attempt(100L, reminder);

        when(reminderRepository.findByStatusAndRemindAtLessThanEqualOrderByRemindAtAsc(
                ReminderStatus.PENDING,
                processUntil
        )).thenReturn(List.of(reminder));
        when(attemptRepository.save(any(ReminderNotificationAttempt.class))).thenReturn(savedAttempt);
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessDueRemindersResponse response = processingService.processDueReminders(processUntil);

        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(reminder.getRemindAt()).isEqualTo(OffsetDateTime.parse("2027-08-23T10:00:00+03:00"));
        assertThat(response.processedCount()).isEqualTo(1);
        assertThat(response.reminders().getFirst().remindAt())
                .isEqualTo(OffsetDateTime.parse("2026-08-23T10:00:00+03:00"));
        assertThat(response.reminders().getFirst().nextRemindAt())
                .isEqualTo(OffsetDateTime.parse("2027-08-23T10:00:00+03:00"));
        assertThat(response.reminders().getFirst().status()).isEqualTo(ReminderStatus.PENDING);
    }

    private Reminder reminder(Long id, RecurrenceType recurrenceType, String remindAt) {
        UserAccount user = new UserAccount();
        user.setEmail("test@napo.local");

        Reminder reminder = new Reminder();
        reminder.setId(id);
        reminder.setUser(user);
        reminder.setTitle("Test reminder");
        reminder.setMessageToSend("Test message");
        reminder.setPreferredChannel(NotificationChannel.LOG);
        reminder.setRecurrenceType(recurrenceType);
        reminder.setRemindAt(OffsetDateTime.parse(remindAt));
        reminder.setStatus(ReminderStatus.PENDING);
        return reminder;
    }

    private ReminderNotificationAttempt attempt(Long id, Reminder reminder) {
        ReminderNotificationAttempt attempt = new ReminderNotificationAttempt();
        attempt.setId(id);
        attempt.setReminder(reminder);
        attempt.setChannel(NotificationChannel.LOG);
        attempt.setStatus(NotificationAttemptStatus.SUCCESS);
        attempt.setAttemptedAt(OffsetDateTime.now());
        return attempt;
    }
}
