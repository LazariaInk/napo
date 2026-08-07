package com.lazar.napo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderProcessingService reminderProcessingService;

    @Scheduled(fixedDelay = 60_000)
    public void processDueReminders() {
        reminderProcessingService.processDueReminders(null);
    }
}
