package com.lazar.napo.service;

import com.lazar.napo.dto.CreateReminderRequest;
import com.lazar.napo.dto.ReminderResponse;
import com.lazar.napo.dto.UpdateReminderRequest;
import com.lazar.napo.entity.Reminder;
import com.lazar.napo.entity.ReminderStatus;
import com.lazar.napo.entity.UserAccount;
import com.lazar.napo.exception.InvalidReminderStateException;
import com.lazar.napo.exception.ResourceNotFoundException;
import com.lazar.napo.mapper.ReminderMapper;
import com.lazar.napo.repository.ReminderRepository;
import com.lazar.napo.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final UserAccountRepository userAccountRepository;
    private final ReminderMapper reminderMapper;

    @Transactional
    public ReminderResponse create(String userEmail, CreateReminderRequest request) {
        UserAccount user = getUser(userEmail);
        Reminder reminder = reminderMapper.toEntity(request);
        reminder.setUser(user);
        return reminderMapper.toResponse(reminderRepository.save(reminder));
    }

    @Transactional(readOnly = true)
    public List<ReminderResponse> findAll(String userEmail) {
        return reminderRepository.findByUserEmailIgnoreCaseOrderByRemindAtAsc(userEmail)
                .stream()
                .map(reminderMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReminderResponse findById(String userEmail, Long id) {
        return reminderMapper.toResponse(getReminder(userEmail, id));
    }

    @Transactional
    public ReminderResponse update(String userEmail, Long id, UpdateReminderRequest request) {
        Reminder reminder = getReminder(userEmail, id);
        ensureReminderCanBeEdited(reminder);
        reminderMapper.updateEntity(reminder, request);
        return reminderMapper.toResponse(reminderRepository.save(reminder));
    }

    @Transactional
    public ReminderResponse cancel(String userEmail, Long id) {
        Reminder reminder = getReminder(userEmail, id);

        if (reminder.getStatus() == ReminderStatus.COMPLETED) {
            throw new InvalidReminderStateException("Completed reminders cannot be cancelled");
        }

        reminder.setStatus(ReminderStatus.CANCELLED);
        return reminderMapper.toResponse(reminderRepository.save(reminder));
    }

    @Transactional
    public void delete(String userEmail, Long id) {
        Reminder reminder = getReminder(userEmail, id);
        reminderRepository.delete(reminder);
    }

    private Reminder getReminder(String userEmail, Long id) {
        return reminderRepository.findByIdAndUserEmailIgnoreCase(id, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder with id %d was not found".formatted(id)));
    }

    private UserAccount getUser(String email) {
        return userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
    }

    private void ensureReminderCanBeEdited(Reminder reminder) {
        if (reminder.getStatus() == ReminderStatus.COMPLETED || reminder.getStatus() == ReminderStatus.CANCELLED) {
            throw new InvalidReminderStateException("Only active reminders can be edited");
        }
    }
}
