package com.lazar.napo.service;

import com.lazar.napo.dto.ReminderResponse;
import com.lazar.napo.entity.NotificationChannel;
import com.lazar.napo.entity.RecurrenceType;
import com.lazar.napo.entity.Reminder;
import com.lazar.napo.entity.ReminderStatus;
import com.lazar.napo.entity.UserAccount;
import com.lazar.napo.exception.ResourceNotFoundException;
import com.lazar.napo.mapper.ReminderMapper;
import com.lazar.napo.repository.ReminderRepository;
import com.lazar.napo.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    private ReminderService reminderService;

    @BeforeEach
    void setUp() {
        reminderService = new ReminderService(
                reminderRepository,
                userAccountRepository,
                new ReminderMapper()
        );
    }

    @Test
    void findsReminderOnlyForAuthenticatedUser() {
        Reminder reminder = reminder(10L, "owner@napo.local");
        when(reminderRepository.findByIdAndUserEmailIgnoreCase(10L, "owner@napo.local"))
                .thenReturn(Optional.of(reminder));

        ReminderResponse response = reminderService.findById("owner@napo.local", 10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Private reminder");
    }

    @Test
    void hidesReminderFromAnotherUser() {
        when(reminderRepository.findByIdAndUserEmailIgnoreCase(10L, "other@napo.local"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.findById("other@napo.local", 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Reminder with id 10 was not found");
    }

    private Reminder reminder(Long id, String email) {
        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setEmail(email);

        Reminder reminder = new Reminder();
        reminder.setId(id);
        reminder.setUser(user);
        reminder.setTitle("Private reminder");
        reminder.setDescription("Only owner can see this");
        reminder.setRemindAt(OffsetDateTime.parse("2026-08-23T10:00:00+03:00"));
        reminder.setTimezone("Europe/Bucharest");
        reminder.setStatus(ReminderStatus.PENDING);
        reminder.setRecurrenceType(RecurrenceType.NONE);
        reminder.setPreferredChannel(NotificationChannel.LOG);
        reminder.setCreatedAt(OffsetDateTime.parse("2026-08-01T10:00:00+03:00"));
        reminder.setUpdatedAt(OffsetDateTime.parse("2026-08-01T10:00:00+03:00"));
        return reminder;
    }
}
