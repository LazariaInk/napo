package com.lazar.napo.controller;

import com.lazar.napo.dto.ChatMessageRequest;
import com.lazar.napo.dto.ChatMessageResponse;
import com.lazar.napo.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            Authentication authentication,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        return ResponseEntity.ok(chatService.handleMessage(authentication.getName(), request));
    }
}
