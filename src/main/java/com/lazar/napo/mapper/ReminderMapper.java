package com.lazar.napo.mapper;

import com.lazar.napo.dto.CreateReminderRequest;
import com.lazar.napo.dto.ReminderResponse;
import com.lazar.napo.dto.UpdateReminderRequest;
import com.lazar.napo.entity.NotificationChannel;
import com.lazar.napo.entity.RecurrenceType;
import com.lazar.napo.entity.Reminder;
import com.lazar.napo.entity.ReminderStatus;
import org.springframework.stereotype.Component;

@Component
public class ReminderMapper {

    private static final String DEFAULT_TIMEZONE = "Europe/Bucharest";

    public Reminder toEntity(CreateReminderRequest request) {
        Reminder reminder = new Reminder();
        reminder.setTitle(request.title());
        reminder.setDescription(request.description());
        reminder.setRemindAt(request.remindAt());
        reminder.setTimezone(defaultTimezone(request.timezone()));
        reminder.setStatus(ReminderStatus.PENDING);
        reminder.setRecurrenceType(defaultRecurrenceType(request.recurrenceType()));
        reminder.setRecurrenceRule(request.recurrenceRule());
        reminder.setPreferredChannel(defaultChannel(request.preferredChannel()));
        reminder.setMessageToSend(request.messageToSend());
        return reminder;
    }

    public void updateEntity(Reminder reminder, UpdateReminderRequest request) {
        reminder.setTitle(request.title());
        reminder.setDescription(request.description());
        reminder.setRemindAt(request.remindAt());
        reminder.setTimezone(defaultTimezone(request.timezone()));
        reminder.setStatus(request.status() == null ? reminder.getStatus() : request.status());
        reminder.setRecurrenceType(defaultRecurrenceType(request.recurrenceType()));
        reminder.setRecurrenceRule(request.recurrenceRule());
        reminder.setPreferredChannel(defaultChannel(request.preferredChannel()));
        reminder.setMessageToSend(request.messageToSend());
    }

    public ReminderResponse toResponse(Reminder reminder) {
        return new ReminderResponse(
                reminder.getId(),
                reminder.getUser() == null ? null : reminder.getUser().getId(),
                reminder.getTitle(),
                reminder.getDescription(),
                reminder.getRemindAt(),
                reminder.getTimezone(),
                reminder.getStatus(),
                reminder.getRecurrenceType(),
                reminder.getRecurrenceRule(),
                reminder.getPreferredChannel(),
                reminder.getMessageToSend(),
                reminder.getCreatedAt(),
                reminder.getUpdatedAt()
        );
    }

    private String defaultTimezone(String timezone) {
        return timezone == null || timezone.isBlank() ? DEFAULT_TIMEZONE : timezone.trim();
    }

    private RecurrenceType defaultRecurrenceType(RecurrenceType recurrenceType) {
        return recurrenceType == null ? RecurrenceType.NONE : recurrenceType;
    }

    private NotificationChannel defaultChannel(NotificationChannel channel) {
        return channel == null ? NotificationChannel.LOG : channel;
    }
}
