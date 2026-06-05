package com.ghostreport.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostreport.model.AuditLog;
import com.ghostreport.model.SecurityAlert;
import com.ghostreport.model.User;
import com.ghostreport.model.UserRole;
import com.ghostreport.repository.AuditLogRepository;
import com.ghostreport.repository.SecurityAlertRepository;
import com.ghostreport.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:runtime-security-event-logging-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "ghostreport.backup-dir=target/test-backups/runtime-security-event-logging",
        "app.upload-dir=target/test-uploads/runtime-security-event-logging",
        "ghostreport.backup-enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RuntimeSecurityEventLoggingTest {

    private static final String PASSWORD = "Password123!";
    private static final String WRONG_PASSWORD = "WrongPassword123!";
    private static final String INVALID_TOKEN = "invalid.jwt.token-value";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private SecurityAlertRepository securityAlertRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String username;

    @BeforeEach
    void setUp() {
        securityAlertRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();

        username = "runtime_events_" + UUID.randomUUID();

        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@ghostreport.test");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(UserRole.ANALYST);
        user.setActive(true);
        userRepository.save(user);
    }

    @Test
    void successfulLoginCreatesAuditLogWithoutTokenOrPassword() throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<AuditLog> logs = auditLogRepository.findAll();

        assertThat(logs)
                .anyMatch(log -> "LOGIN_SUCCESS".equals(log.getAction()))
                .allMatch(log -> doesNotContainSensitiveValues(
                        log.getDetails(),
                        response,
                        PASSWORD
                ));
    }

    @Test
    void failedLoginCreatesAuditLogWithoutPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(WRONG_PASSWORD)))
                .andExpect(status().isUnauthorized());

        List<AuditLog> logs = auditLogRepository.findAll();

        assertThat(logs)
                .anyMatch(log -> "LOGIN_FAILED".equals(log.getAction()))
                .allMatch(log -> doesNotContainSensitiveValues(
                        log.getDetails(),
                        WRONG_PASSWORD,
                        PASSWORD
                ));
    }

    @Test
    void invalidJwtCreatesSecurityAlertWithoutStoringToken() throws Exception {
        mockMvc.perform(get("/analyst/panel")
                        .header("Authorization", "Bearer " + INVALID_TOKEN))
                .andExpect(status().isUnauthorized());

        List<SecurityAlert> alerts = securityAlertRepository.findAll();

        assertThat(alerts)
                .anyMatch(alert -> "INVALID_JWT_TOKEN".equals(alert.getAlertType())
                        && alert.getDescription() != null
                        && alert.getDescription().contains("/analyst/panel"))
                .allMatch(alert -> doesNotContainSensitiveValues(
                        alert.getDescription(),
                        INVALID_TOKEN
                ));
    }

    @Test
    void logoutRevokesTokenAndCreatesAuditLogWithoutStoringToken() throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(response)
                .path("token")
                .asText();

        mockMvc.perform(post("/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/analyst/panel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        List<AuditLog> logs = auditLogRepository.findAll();

        assertThat(logs)
                .anyMatch(log -> "LOGOUT".equals(log.getAction()))
                .allMatch(log -> doesNotContainSensitiveValues(
                        log.getDetails(),
                        token,
                        PASSWORD
                ));
    }

    private String loginBody(String password) {
        return """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);
    }

    private boolean doesNotContainSensitiveValues(String value, String... sensitiveValues) {
        if (value == null) {
            return true;
        }

        for (String sensitiveValue : sensitiveValues) {
            if (sensitiveValue != null && !sensitiveValue.isBlank() && value.contains(sensitiveValue)) {
                return false;
            }
        }

        return true;
    }
}
