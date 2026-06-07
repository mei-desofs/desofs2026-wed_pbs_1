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
import java.time.LocalDateTime;
import java.time.ZoneOffset;

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
        LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1);
        String correlationId = "runtime-login-success";

        String response = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .header("X-Correlation-ID", correlationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getHeader("X-Correlation-ID"))
                        .isEqualTo(correlationId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        LocalDateTime after = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(1);

        List<AuditLog> logs = auditLogRepository.findAll();

        assertThat(logs)
                .anyMatch(log -> "LOGIN_SUCCESS".equals(log.getAction()))
                .allMatch(log -> doesNotContainSensitiveValues(
                        log.getDetails(),
                        response,
                        PASSWORD
                ));
        assertThat(logs)
                .filteredOn(log -> "LOGIN_SUCCESS".equals(log.getAction()))
                .allSatisfy(log -> {
                    assertThat(log.getCorrelationId()).isEqualTo(correlationId);
                    assertThat(log.getIntegrityHash()).hasSize(64);
                    assertThat(log.getTimestamp()).isBetween(before, after);
                });
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
        String correlationId = "runtime-invalid-jwt";

        mockMvc.perform(get("/analyst/panel")
                        .header("X-Correlation-ID", correlationId)
                        .header("Authorization", "Bearer " + INVALID_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertThat(result.getResponse().getHeader("X-Correlation-ID"))
                        .isEqualTo(correlationId));

        List<SecurityAlert> alerts = securityAlertRepository.findAll();

        assertThat(alerts)
                .anyMatch(alert -> "INVALID_JWT_TOKEN".equals(alert.getAlertType())
                        && alert.getDescription() != null
                        && alert.getDescription().contains("/analyst/panel"))
                .allMatch(alert -> doesNotContainSensitiveValues(
                        alert.getDescription(),
                        INVALID_TOKEN
                ));
        assertThat(alerts)
                .filteredOn(alert -> "INVALID_JWT_TOKEN".equals(alert.getAlertType()))
                .allSatisfy(alert -> {
                    assertThat(alert.getCorrelationId()).isEqualTo(correlationId);
                    assertThat(alert.getIntegrityHash()).hasSize(64);
                });
    }

    @Test
    void forbiddenAccessCreatesSecurityAlertWithCorrelationId() throws Exception {
        String token = bearerToken();
        String correlationId = "runtime-forbidden-access";

        mockMvc.perform(get("/admin/panel")
                        .header("X-Correlation-ID", correlationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(securityAlertRepository.findAll())
                .anySatisfy(alert -> {
                    assertThat(alert.getAlertType()).isEqualTo("FORBIDDEN_ACCESS_ATTEMPT");
                    assertThat(alert.getCorrelationId()).isEqualTo(correlationId);
                    assertThat(alert.getIntegrityHash()).hasSize(64);
                    assertThat(alert.getDescription()).doesNotContain(token);
                });
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

    @Test
    void auditLogRedactsPasswordsTokensAuthorizationHeadersAndTrackingCodes() {
        String trackingCode = "GR-abcdefghijklmnopqrst";
        auditLogService().log(
                "SYNTHETIC_REDACTION_TEST",
                "AUTHENTICATION",
                null,
                """
                        password=Password123! Authorization: Bearer %s {"token":"%s","trackingCode":"%s"}
                        """.formatted(INVALID_TOKEN, INVALID_TOKEN, trackingCode)
        );

        AuditLog log = auditLogRepository.findAll()
                .stream()
                .filter(candidate -> "SYNTHETIC_REDACTION_TEST".equals(candidate.getAction()))
                .findFirst()
                .orElseThrow();

        assertThat(log.getDetails())
                .doesNotContain("Password123!")
                .doesNotContain(INVALID_TOKEN)
                .doesNotContain(trackingCode)
                .contains("[REDACTED]");
    }

    @Autowired
    private com.ghostreport.service.AuditLogService auditLogService;

    private com.ghostreport.service.AuditLogService auditLogService() {
        return auditLogService;
    }

    private String loginBody(String password) {
        return """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);
    }

    private String bearerToken() throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response)
                .path("token")
                .asText();
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
