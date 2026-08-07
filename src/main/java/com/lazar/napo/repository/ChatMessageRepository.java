package com.lazar.napo.repository;

import com.lazar.napo.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findTop10ByConversationIdOrderByCreatedAtDesc(Long conversationId);
}
