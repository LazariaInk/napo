package com.lazar.napo.controller;

import com.lazar.napo.dto.NotificationAttemptResponse;
import com.lazar.napo.service.NotificationAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notification-attempts")
@RequiredArgsConstructor
public class NotificationAttemptController {

    private final NotificationAttemptService notificationAttemptService;

    @GetMapping
    public ResponseEntity<List<NotificationAttemptResponse>> findAll(Authentication authentication) {
        return ResponseEntity.ok(notificationAttemptService.findAllForUser(authentication.getName()));
    }

    @GetMapping("/reminders/{reminderId}")
    public ResponseEntity<List<NotificationAttemptResponse>> findByReminder(
            Authentication authentication,
            @PathVariable Long reminderId
    ) {
        return ResponseEntity.ok(notificationAttemptService.findByReminderForUser(
                authentication.getName(),
                reminderId
        ));
    }
}
