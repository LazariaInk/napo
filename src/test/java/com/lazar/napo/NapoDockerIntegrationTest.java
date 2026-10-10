package com.lazar.napo;

import com.lazar.napo.dto.AuthResponse;
import com.lazar.napo.dto.CreateReminderRequest;
import com.lazar.napo.dto.NotificationAttemptResponse;
import com.lazar.napo.dto.ProcessDueRemindersRequest;
import com.lazar.napo.dto.ProcessDueRemindersResponse;
import com.lazar.napo.dto.RegisterRequest;
import com.lazar.napo.dto.ReminderResponse;
import com.lazar.napo.entity.NotificationAttemptStatus;
import com.lazar.napo.entity.NotificationChannel;
import com.lazar.napo.entity.RecurrenceType;
import com.lazar.napo.entity.ReminderStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:postgresql://localhost:55432/napo",
                "spring.datasource.username=postgres",
                "spring.datasource.password=postgres",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.show-sql=false",
                "spring.flyway.enabled=true",
                "spring.ai.openai.api-key=test-key"
        }
)
@EnabledIfEnvironmentVariable(named = "NAPO_RUN_INTEGRATION_TESTS", matches = "true")
class NapoDockerIntegrationTest {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @LocalServerPort
    private int port;

    @Test
    void createsReminderProcessesItAndStoresNotificationAttempt() throws Exception {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String email = "it-" + uniqueId + "@napo.local";
        String password = "password123";

        HttpResponse<String> registerResponse = post(
                "/api/auth/register",
                new RegisterRequest(email, password, "Integration Test User"),
                null,
                null
        );

        assertThat(registerResponse.statusCode()).isEqualTo(201);
        AuthResponse registeredUser = read(registerResponse, AuthResponse.class);
        assertThat(registeredUser.email()).isEqualTo(email);
        assertThat(registeredUser.accessToken()).isNotBlank();

        HttpResponse<String> meResponse = getWithBearer("/api/auth/me", registeredUser.accessToken());
        assertThat(meResponse.statusCode()).isBetween(200, 299);
        AuthResponse authenticatedUser = read(meResponse, AuthResponse.class);
        assertThat(authenticatedUser.email()).isEqualTo(email);

        OffsetDateTime remindAt = OffsetDateTime.now().plusMinutes(5).withNano(0);

        HttpResponse<String> createReminderResponse = post(
                "/api/reminders",
                new CreateReminderRequest(
                        "Integration reminder " + uniqueId,
                        "Created by Docker integration test",
                        remindAt,
                        "Europe/Bucharest",
                        RecurrenceType.NONE,
                        null,
                        NotificationChannel.LOG,
                        "Integration notification message"
                ),
                email,
                password
        );

        assertThat(createReminderResponse.statusCode()).isEqualTo(201);
        ReminderResponse createdReminder = read(createReminderResponse, ReminderResponse.class);
        Long reminderId = createdReminder.id();
        assertThat(createdReminder.status()).isEqualTo(ReminderStatus.PENDING);

        HttpResponse<String> processResponse = post(
                "/api/internal/reminders/process-due",
                new ProcessDueRemindersRequest(remindAt.plusMinutes(1)),
                email,
                password
        );

        assertThat(processResponse.statusCode()).isBetween(200, 299);
        ProcessDueRemindersResponse processedReminders = read(processResponse, ProcessDueRemindersResponse.class);
        assertThat(processedReminders.reminders())
                .anySatisfy(processedReminder -> {
                    assertThat(processedReminder.reminderId()).isEqualTo(reminderId);
                    assertThat(processedReminder.status()).isEqualTo(ReminderStatus.COMPLETED);
                    assertThat(processedReminder.attemptId()).isNotNull();
                });

        HttpResponse<String> completedReminderResponse = get(
                "/api/reminders/" + reminderId,
                email,
                password
        );

        assertThat(completedReminderResponse.statusCode()).isBetween(200, 299);
        ReminderResponse completedReminder = read(completedReminderResponse, ReminderResponse.class);
        assertThat(completedReminder.status()).isEqualTo(ReminderStatus.COMPLETED);

        HttpResponse<String> attemptsResponse = get(
                "/api/notification-attempts/reminders/" + reminderId,
                email,
                password
        );

        assertThat(attemptsResponse.statusCode()).isBetween(200, 299);
        List<NotificationAttemptResponse> attempts = OBJECT_MAPPER.readValue(
                attemptsResponse.body(),
                new TypeReference<>() {
                }
        );
        assertThat(attempts)
                .anySatisfy(attempt -> {
                    assertThat(attempt.reminderId()).isEqualTo(reminderId);
                    assertThat(attempt.channel()).isEqualTo(NotificationChannel.LOG);
                    assertThat(attempt.status()).isEqualTo(NotificationAttemptStatus.SUCCESS);
                });
    }

    private HttpResponse<String> post(String path, Object body, String email, String password) throws Exception {
        HttpRequest.Builder builder = request(path, email, password)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body)));

        return HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String email, String password) throws Exception {
        HttpRequest.Builder builder = request(path, email, password).GET();
        return HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getWithBearer(String path, String accessToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + accessToken)
                .GET();
        return HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String path, String email, String password) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path));

        if (email != null && password != null) {
            builder.header("Authorization", basicAuth(email, password));
        }

        return builder;
    }

    private String basicAuth(String email, String password) {
        String credentials = email + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    private <T> T read(HttpResponse<String> response, Class<T> type) throws Exception {
        return OBJECT_MAPPER.readValue(response.body(), type);
    }
}
