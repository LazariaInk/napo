package com.lazar.napo.repository;

import com.lazar.napo.entity.ReminderNotificationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReminderNotificationAttemptRepository extends JpaRepository<ReminderNotificationAttempt, Long> {

    List<ReminderNotificationAttempt> findByReminderIdOrderByAttemptedAtDesc(Long reminderId);
}
