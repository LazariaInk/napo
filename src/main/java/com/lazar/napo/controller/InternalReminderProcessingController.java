package com.lazar.napo.controller;

import com.lazar.napo.dto.ProcessDueRemindersRequest;
import com.lazar.napo.dto.ProcessDueRemindersResponse;
import com.lazar.napo.service.ReminderProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/reminders")
@RequiredArgsConstructor
public class InternalReminderProcessingController {

    private final ReminderProcessingService reminderProcessingService;

    @PostMapping("/process-due")
    public ResponseEntity<ProcessDueRemindersResponse> processDue(
            @RequestBody(required = false) ProcessDueRemindersRequest request
    ) {
        return ResponseEntity.ok(reminderProcessingService.processDueReminders(
                request == null ? null : request.processUntil()
        ));
    }
}
