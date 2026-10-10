package com.lazar.napo.service;

import com.lazar.napo.entity.RecurrenceType;
import com.lazar.napo.entity.ReminderStatus;
import com.lazar.napo.entity.UserAccount;
import com.lazar.napo.entity.UserPlan;
import com.lazar.napo.exception.PlanLimitExceededException;
import com.lazar.napo.repository.ChatUsageDailyRepository;
import com.lazar.napo.repository.ReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsageLimitServiceTest {

    @Mock
    private ChatUsageDailyRepository chatUsageDailyRepository;

    @Mock
    private ReminderRepository reminderRepository;

    private UsageLimitService usageLimitService;

    @BeforeEach
    void setUp() {
        usageLimitService = new UsageLimitService(chatUsageDailyRepository, reminderRepository);
    }

    @Test
    void rejectsFreeUserWhenActiveReminderLimitIsReached() {
        UserAccount user = user();
        when(reminderRepository.countByUserEmailIgnoreCaseAndStatus(
                "test@napo.local",
                ReminderStatus.PENDING
        )).thenReturn(30L);

        assertThatThrownBy(() -> usageLimitService.ensureCanCreateReminder(user, RecurrenceType.NONE))
                .isInstanceOf(PlanLimitExceededException.class)
                .hasMessage("Active reminder limit reached for FREE plan. Limit: 30 active reminders.");
    }

    @Test
    void rejectsFreeUserWhenRecurringReminderLimitIsReached() {
        UserAccount user = user();
        when(reminderRepository.countByUserEmailIgnoreCaseAndStatus(
                "test@napo.local",
                ReminderStatus.PENDING
        )).thenReturn(5L);
        when(reminderRepository.countByUserEmailIgnoreCaseAndStatusAndRecurrenceTypeNot(
                "test@napo.local",
                ReminderStatus.PENDING,
                RecurrenceType.NONE
        )).thenReturn(10L);

        assertThatThrownBy(() -> usageLimitService.ensureCanCreateReminder(user, RecurrenceType.WEEKLY))
                .isInstanceOf(PlanLimitExceededException.class)
                .hasMessage("Active recurring reminder limit reached for FREE plan. Limit: 10 active recurring reminders.");
    }

    private UserAccount user() {
        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setEmail("test@napo.local");
        user.setPlan(UserPlan.FREE);
        return user;
    }
}