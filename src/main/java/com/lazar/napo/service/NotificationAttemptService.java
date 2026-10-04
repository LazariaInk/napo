package com.lazar.napo.service;

import com.lazar.napo.dto.NotificationAttemptResponse;
import com.lazar.napo.entity.ReminderNotificationAttempt;
import com.lazar.napo.repository.ReminderNotificationAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationAttemptService {

    private final ReminderNotificationAttemptRepository attemptRepository;

    @Transactional(readOnly = true)
    public List<NotificationAttemptResponse> findAllForUser(String userEmail) {
        return attemptRepository.findByReminderUserEmailIgnoreCaseOrderByAttemptedAtDesc(userEmail)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationAttemptResponse> findByReminderForUser(String userEmail, Long reminderId) {
        return attemptRepository
                .findByReminderIdAndReminderUserEmailIgnoreCaseOrderByAttemptedAtDesc(reminderId, userEmail)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private NotificationAttemptResponse toResponse(ReminderNotificationAttempt attempt) {
        return new NotificationAttemptResponse(
                attempt.getId(),
                attempt.getReminder().getId(),
                attempt.getReminder().getTitle(),
                attempt.getChannel(),
                attempt.getStatus(),
                attempt.getAttemptedAt(),
                attempt.getProviderMessageId(),
                attempt.getErrorMessage()
        );
    }
}
