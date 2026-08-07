package com.lazar.napo.repository;

import com.lazar.napo.entity.Reminder;
import com.lazar.napo.entity.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByStatusOrderByRemindAtAsc(ReminderStatus status);

    List<Reminder> findByStatusAndRemindAtLessThanEqualOrderByRemindAtAsc(
            ReminderStatus status,
            OffsetDateTime remindAt
    );

    List<Reminder> findByUserEmailIgnoreCaseOrderByRemindAtAsc(String email);

    Optional<Reminder> findByIdAndUserEmailIgnoreCase(Long id, String email);
}
