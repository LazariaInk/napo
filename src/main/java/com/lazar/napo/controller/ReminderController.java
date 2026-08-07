package com.lazar.napo.controller;

import com.lazar.napo.dto.CreateReminderRequest;
import com.lazar.napo.dto.ReminderResponse;
import com.lazar.napo.dto.UpdateReminderRequest;
import com.lazar.napo.service.ReminderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @PostMapping
    public ResponseEntity<ReminderResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateReminderRequest request
    ) {
        ReminderResponse response = reminderService.create(authentication.getName(), request);
        return ResponseEntity
                .created(URI.create("/api/reminders/" + response.id()))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<ReminderResponse>> findAll(Authentication authentication) {
        return ResponseEntity.ok(reminderService.findAll(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReminderResponse> findById(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(reminderService.findById(authentication.getName(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReminderResponse> update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateReminderRequest request
    ) {
        return ResponseEntity.ok(reminderService.update(authentication.getName(), id, request));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ReminderResponse> cancel(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(reminderService.cancel(authentication.getName(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        reminderService.delete(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
