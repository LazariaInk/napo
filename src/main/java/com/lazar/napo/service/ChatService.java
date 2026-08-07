package com.lazar.napo.service;

import com.lazar.napo.dto.AiReminderExtraction;
import com.lazar.napo.dto.ChatMessageRequest;
import com.lazar.napo.dto.ChatMessageResponse;
import com.lazar.napo.dto.ChatResponseType;
import com.lazar.napo.dto.CreateReminderRequest;
import com.lazar.napo.dto.ReminderResponse;
import com.lazar.napo.entity.ChatConversation;
import com.lazar.napo.entity.ChatConversationStatus;
import com.lazar.napo.entity.ChatMessage;
import com.lazar.napo.entity.ChatMessageSender;
import com.lazar.napo.entity.NotificationChannel;
import com.lazar.napo.entity.RecurrenceType;
import com.lazar.napo.entity.UserAccount;
import com.lazar.napo.exception.AiIntegrationException;
import com.lazar.napo.exception.ResourceNotFoundException;
import com.lazar.napo.repository.ChatConversationRepository;
import com.lazar.napo.repository.ChatMessageRepository;
import com.lazar.napo.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final String DEFAULT_TIMEZONE = "Europe/Bucharest";

    private final ChatClient.Builder chatClientBuilder;
    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final UserAccountRepository userAccountRepository;
    private final ReminderService reminderService;

    @Transactional
    public ChatMessageResponse handleMessage(String userEmail, ChatMessageRequest request) {
        long startedAt = System.nanoTime();
        ChatConversation conversation = getOrCreateConversation(userEmail, request.conversationId());
        saveMessage(conversation, ChatMessageSender.USER, request.message());
        long persistenceReadyAt = System.nanoTime();

        AiReminderExtraction extraction = extractReminderIntent(conversation, request.message());
        long aiReadyAt = System.nanoTime();

        if (!"CREATE_REMINDER".equalsIgnoreCase(extraction.action())) {
            String assistantMessage = fallbackAssistantMessage(extraction);
            saveMessage(conversation, ChatMessageSender.ASSISTANT, assistantMessage);
            logChatTiming(userEmail, conversation.getId(), extraction.action(), startedAt, persistenceReadyAt, aiReadyAt);
            return new ChatMessageResponse(
                    conversation.getId(),
                    ChatResponseType.CLARIFICATION_NEEDED,
                    assistantMessage,
                    null
            );
        }

        ReminderResponse reminder = createReminderFromExtraction(userEmail, extraction);
        long reminderReadyAt = System.nanoTime();
        conversation.setStatus(ChatConversationStatus.COMPLETED);

        String assistantMessage = extraction.assistantMessage() == null || extraction.assistantMessage().isBlank()
                ? "Am creat reminderul."
                : extraction.assistantMessage();
        saveMessage(conversation, ChatMessageSender.ASSISTANT, assistantMessage);
        logChatTiming(
                userEmail,
                conversation.getId(),
                extraction.action(),
                startedAt,
                persistenceReadyAt,
                aiReadyAt,
                reminderReadyAt
        );

        return new ChatMessageResponse(
                conversation.getId(),
                ChatResponseType.REMINDER_CREATED,
                assistantMessage,
                reminder
        );
    }

    private ChatConversation getOrCreateConversation(String userEmail, Long conversationId) {
        if (conversationId != null) {
            return conversationRepository.findByIdAndUserEmailIgnoreCase(conversationId, userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Conversation with id %d was not found".formatted(conversationId)
                    ));
        }

        UserAccount user = userAccountRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));

        ChatConversation conversation = new ChatConversation();
        conversation.setUser(user);
        conversation.setStatus(ChatConversationStatus.OPEN);
        return conversationRepository.save(conversation);
    }

    private AiReminderExtraction extractReminderIntent(ChatConversation conversation, String userMessage) {
        try {
            long promptStartedAt = System.nanoTime();
            String userPrompt = buildUserPrompt(conversation.getId(), userMessage);
            long promptReadyAt = System.nanoTime();
            log.info(
                    "NAPO chat prompt built | conversationId={} | promptChars={} | durationMs={}",
                    conversation.getId(),
                    userPrompt.length(),
                    elapsedMs(promptStartedAt, promptReadyAt)
            );

            return chatClientBuilder.build()
                    .prompt()
                    .system(systemPrompt())
                    .user(userPrompt)
                    .call()
                    .entity(AiReminderExtraction.class);
        } catch (Exception exception) {
            throw new AiIntegrationException("Could not process the message with AI", exception);
        }
    }

    private ReminderResponse createReminderFromExtraction(String userEmail, AiReminderExtraction extraction) {
        OffsetDateTime remindAt = parseRequiredDateTime(extraction.remindAt());
        RecurrenceType recurrenceType = parseEnum(
                extraction.recurrenceType(),
                RecurrenceType.class,
                RecurrenceType.NONE
        );
        NotificationChannel channel = parseEnum(
                extraction.preferredChannel(),
                NotificationChannel.class,
                NotificationChannel.LOG
        );

        CreateReminderRequest createRequest = new CreateReminderRequest(
                requiredText(extraction.title(), "title"),
                extraction.description(),
                remindAt,
                extraction.timezone() == null || extraction.timezone().isBlank()
                        ? DEFAULT_TIMEZONE
                        : extraction.timezone(),
                recurrenceType,
                null,
                channel,
                extraction.messageToSend()
        );

        return reminderService.create(userEmail, createRequest);
    }

    private OffsetDateTime parseRequiredDateTime(String value) {
        if (value == null || value.isBlank()) {
            throw new AiIntegrationException("AI response did not include a reminder date and time", null);
        }

        try {
            return OffsetDateTime.parse(value);
        } catch (Exception exception) {
            throw new AiIntegrationException("AI response included an invalid reminder date and time", exception);
        }
    }

    private String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new AiIntegrationException("AI response did not include required field: " + fieldName, null);
        }
        return value.trim();
    }

    private <T extends Enum<T>> T parseEnum(String value, Class<T> enumType, T defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
    }

    private void saveMessage(ChatConversation conversation, ChatMessageSender sender, String content) {
        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);
        messageRepository.save(message);
    }

    private String fallbackAssistantMessage(AiReminderExtraction extraction) {
        if (extraction.assistantMessage() != null && !extraction.assistantMessage().isBlank()) {
            return extraction.assistantMessage();
        }
        if (extraction.missingInformation() != null && !extraction.missingInformation().isBlank()) {
            return "Am nevoie de inca un detaliu: " + extraction.missingInformation();
        }
        return "Am nevoie de mai multe detalii ca sa creez reminderul.";
    }

    private String buildUserPrompt(Long conversationId, String userMessage) {
        List<ChatMessage> recentMessages = messageRepository
                .findTop10ByConversationIdOrderByCreatedAtDesc(conversationId)
                .reversed();

        StringBuilder prompt = new StringBuilder();
        prompt.append("Current server time: ").append(OffsetDateTime.now()).append("\n");
        prompt.append("Default timezone: ").append(DEFAULT_TIMEZONE).append("\n");
        prompt.append("Recent conversation:\n");

        for (ChatMessage message : recentMessages) {
            prompt.append(message.getSender()).append(": ").append(message.getContent()).append("\n");
        }

        prompt.append("Latest user message: ").append(userMessage);
        return prompt.toString();
    }

    private String systemPrompt() {
        return """
                You are NAPO, a Romanian reminder assistant.
                Your job is to transform natural language into a reminder or ask one concise clarification question.

                Return only a structured object matching these fields:
                action: CREATE_REMINDER or ASK_CLARIFICATION
                assistantMessage: Romanian response for the user
                title: short reminder title, max 150 chars
                description: optional details
                remindAt: ISO-8601 OffsetDateTime, for example 2026-08-23T10:00:00+03:00
                timezone: IANA timezone, default Europe/Bucharest
                recurrenceType: NONE, DAILY, WEEKLY, MONTHLY, or YEARLY
                preferredChannel: LOG or IN_APP
                messageToSend: optional message that should be delivered when the reminder fires
                missingInformation: what is missing if clarification is needed

                Rules:
                - If date, month, year, time, or recurrence is ambiguous, use ASK_CLARIFICATION.
                - If the user says "pe data de 23" without a month, ask which month.
                - If time is missing, ask what time unless the user clearly implies a part of day.
                - Do not invent phone numbers, external channels, or contact details.
                - Use CREATE_REMINDER only when remindAt can be represented as a future ISO-8601 OffsetDateTime.
                - For birthdays, recurrenceType should be YEARLY if the user wants yearly recurrence or clearly says it is a birthday reminder.
                - For MVP, choose LOG unless the user explicitly asks for IN_APP.
                """;
    }

    private void logChatTiming(
            String userEmail,
            Long conversationId,
            String aiAction,
            long startedAt,
            long persistenceReadyAt,
            long aiReadyAt
    ) {
        log.info(
                "NAPO chat timing | user={} | conversationId={} | action={} | persistenceMs={} | aiMs={} | totalMs={}",
                userEmail,
                conversationId,
                aiAction,
                elapsedMs(startedAt, persistenceReadyAt),
                elapsedMs(persistenceReadyAt, aiReadyAt),
                elapsedMs(startedAt, aiReadyAt)
        );
    }

    private void logChatTiming(
            String userEmail,
            Long conversationId,
            String aiAction,
            long startedAt,
            long persistenceReadyAt,
            long aiReadyAt,
            long reminderReadyAt
    ) {
        log.info(
                "NAPO chat timing | user={} | conversationId={} | action={} | persistenceMs={} | aiMs={} | reminderCreateMs={} | totalMs={}",
                userEmail,
                conversationId,
                aiAction,
                elapsedMs(startedAt, persistenceReadyAt),
                elapsedMs(persistenceReadyAt, aiReadyAt),
                elapsedMs(aiReadyAt, reminderReadyAt),
                elapsedMs(startedAt, reminderReadyAt)
        );
    }

    private long elapsedMs(long start, long end) {
        return (end - start) / 1_000_000;
    }
}
