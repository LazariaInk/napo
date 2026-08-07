package com.lazar.napo.service;

import com.lazar.napo.dto.ProcessDueRemindersResponse;
import com.lazar.napo.dto.ProcessedReminderResponse;
import com.lazar.napo.entity.NotificationAttemptStatus;
import com.lazar.napo.entity.Reminder;
import com.lazar.napo.entity.ReminderNotificationAttempt;
import com.lazar.napo.entity.ReminderStatus;
import com.lazar.napo.repository.ReminderNotificationAttemptRepository;
import com.lazar.napo.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ReminderProcessingService.class);

    private final ReminderRepository reminderRepository;
    private final ReminderNotificationAttemptRepository attemptRepository;

    @Transactional
    public ProcessDueRemindersResponse processDueReminders(OffsetDateTime processUntil) {
        OffsetDateTime effectiveProcessUntil = processUntil == null ? OffsetDateTime.now() : processUntil;
        List<Reminder> dueReminders = reminderRepository
                .findByStatusAndRemindAtLessThanEqualOrderByRemindAtAsc(
                        ReminderStatus.PENDING,
                        effectiveProcessUntil
                );

        List<ProcessedReminderResponse> processedReminders = new ArrayList<>();

        for (Reminder reminder : dueReminders) {
            ReminderNotificationAttempt attempt = notifyThroughMockChannel(reminder);
            reminder.setStatus(ReminderStatus.COMPLETED);
            reminderRepository.save(reminder);

            processedReminders.add(new ProcessedReminderResponse(
                    reminder.getId(),
                    reminder.getTitle(),
                    reminder.getRemindAt(),
                    reminder.getStatus(),
                    attempt.getChannel(),
                    attempt.getId()
            ));
        }

        return new ProcessDueRemindersResponse(
                effectiveProcessUntil,
                processedReminders.size(),
                processedReminders
        );
    }

    private ReminderNotificationAttempt notifyThroughMockChannel(Reminder reminder) {
        String recipient = reminder.getUser() == null ? "unknown-user" : reminder.getUser().getEmail();
        String message = reminder.getMessageToSend() == null || reminder.getMessageToSend().isBlank()
                ? reminder.getTitle()
                : reminder.getMessageToSend();

        log.info(
                "NAPO reminder notification mock | reminderId={} | user={} | channel={} | title={} | message={}",
                reminder.getId(),
                recipient,
                reminder.getPreferredChannel(),
                reminder.getTitle(),
                message
        );

        ReminderNotificationAttempt attempt = new ReminderNotificationAttempt();
        attempt.setReminder(reminder);
        attempt.setChannel(reminder.getPreferredChannel());
        attempt.setStatus(NotificationAttemptStatus.SUCCESS);
        attempt.setAttemptedAt(OffsetDateTime.now());
        attempt.setProviderMessageId("mock-" + reminder.getId() + "-" + System.currentTimeMillis());

        return attemptRepository.save(attempt);
    }
}
