package com.lazar.napo.service;

import com.lazar.napo.entity.ChatUsageDaily;
import com.lazar.napo.entity.RecurrenceType;
import com.lazar.napo.entity.ReminderStatus;
import com.lazar.napo.entity.UserAccount;
import com.lazar.napo.entity.UserPlan;
import com.lazar.napo.exception.PlanLimitExceededException;
import com.lazar.napo.exception.UsageLimitExceededException;
import com.lazar.napo.repository.ChatUsageDailyRepository;
import com.lazar.napo.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class UsageLimitService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Bucharest");

    private final ChatUsageDailyRepository chatUsageDailyRepository;
    private final ReminderRepository reminderRepository;

    @Transactional
    public void consumeChatMessage(UserAccount user) {
        PlanLimits limits = limitsFor(user);
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        ChatUsageDaily usage = chatUsageDailyRepository
                .findByUserIdAndUsageDate(user.getId(), today)
                .orElseGet(() -> createUsage(user, today));

        if (usage.getMessageCount() >= limits.chatMessagesPerDay()) {
            throw new UsageLimitExceededException(
                    "Daily chat limit reached for %s plan. Limit: %d messages per day."
                            .formatted(resolvePlan(user), limits.chatMessagesPerDay())
            );
        }

        usage.setMessageCount(usage.getMessageCount() + 1);
        chatUsageDailyRepository.save(usage);
    }

    @Transactional(readOnly = true)
    public void ensureCanCreateReminder(UserAccount user, RecurrenceType recurrenceType) {
        PlanLimits limits = limitsFor(user);
        long activeReminders = reminderRepository.countByUserEmailIgnoreCaseAndStatus(
                user.getEmail(),
                ReminderStatus.PENDING
        );

        if (activeReminders >= limits.activeReminders()) {
            throw new PlanLimitExceededException(
                    "Active reminder limit reached for %s plan. Limit: %d active reminders."
                            .formatted(resolvePlan(user), limits.activeReminders())
            );
        }

        if (recurrenceType != null && recurrenceType != RecurrenceType.NONE) {
            long recurringReminders = reminderRepository.countByUserEmailIgnoreCaseAndStatusAndRecurrenceTypeNot(
                    user.getEmail(),
                    ReminderStatus.PENDING,
                    RecurrenceType.NONE
            );

            if (recurringReminders >= limits.activeRecurringReminders()) {
                throw new PlanLimitExceededException(
                        "Active recurring reminder limit reached for %s plan. Limit: %d active recurring reminders."
                                .formatted(resolvePlan(user), limits.activeRecurringReminders())
                );
            }
        }
    }

    private ChatUsageDaily createUsage(UserAccount user, LocalDate today) {
        ChatUsageDaily usage = new ChatUsageDaily();
        usage.setUser(user);
        usage.setUsageDate(today);
        usage.setMessageCount(0);
        return usage;
    }

    private PlanLimits limitsFor(UserAccount user) {
        return switch (resolvePlan(user)) {
            case FREE -> new PlanLimits(20, 30, 10);
            case PLUS -> new PlanLimits(200, 500, 100);
            case BUSINESS -> new PlanLimits(1000, 5000, 1000);
        };
    }

    private UserPlan resolvePlan(UserAccount user) {
        return user.getPlan() == null ? UserPlan.FREE : user.getPlan();
    }

    private record PlanLimits(
            int chatMessagesPerDay,
            int activeReminders,
            int activeRecurringReminders
    ) {
    }
}