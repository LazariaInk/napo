package com.lazar.napo.repository;

import com.lazar.napo.entity.ChatUsageDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ChatUsageDailyRepository extends JpaRepository<ChatUsageDaily, Long> {

    Optional<ChatUsageDaily> findByUserIdAndUsageDate(Long userId, LocalDate usageDate);
}