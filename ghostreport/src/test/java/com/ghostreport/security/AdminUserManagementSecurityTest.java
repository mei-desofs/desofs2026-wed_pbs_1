package com.ghostreport.security;

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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-user-management-security-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "ghostreport.backup-dir=target/test-backups/admin-user-management-security",
        "app.upload-dir=target/test-uploads/admin-user-management-security",
        "ghostreport.backup-enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminUserManagementSecurityTest {

    private static final String PASSWORD = "Password123!";

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

    private String adminUsername;
    private String analystUsername;
    private Long analystId;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        securityAlertRepository.deleteAll();
        userRepository.deleteAll();

        adminUsername = createUser(UserRole.ADMIN, true).getUsername();
        User analyst = createUser(UserRole.ANALYST, true);
        analystUsername = analyst.getUsername();
        analystId = analyst.getId();
    }

    @Test
    void adminCanDeactivateAndReactivateUserWithAuditLog() throws Exception {
        mockMvc.perform(patch("/admin/users/{id}/deactivate", analystId)
                        .with(csrf())
                        .header("Authorization", bearerToken(adminUsername, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(analystId))
                .andExpect(jsonPath("$.active").value(false));

        assertThat(userRepository.findById(analystId).orElseThrow().isActive()).isFalse();
        assertThat(auditLogRepository.findAll().stream().map(AuditLog::getAction))
                .contains("USER_DEACTIVATED");

        mockMvc.perform(patch("/admin/users/{id}/activate", analystId)
                        .with(csrf())
                        .header("Authorization", bearerToken(adminUsername, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(analystId))
                .andExpect(jsonPath("$.active").value(true));

        assertThat(userRepository.findById(analystId).orElseThrow().isActive()).isTrue();
        assertThat(auditLogRepository.findAll().stream().map(AuditLog::getAction))
                .contains("USER_ACTIVATED");
    }

    @Test
    void nonAdminCannotActivateOrDeactivateUsers() throws Exception {
        mockMvc.perform(patch("/admin/users/{id}/deactivate", analystId)
                        .with(csrf())
                        .header("Authorization", bearerToken(analystUsername, PASSWORD)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/admin/users/{id}/activate", analystId)
                        .with(csrf())
                        .header("Authorization", bearerToken(analystUsername, PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void lastActiveAdminCannotBeDeactivated() throws Exception {
        Long adminId = userRepository.findByUsername(adminUsername).orElseThrow().getId();

        mockMvc.perform(patch("/admin/users/{id}/deactivate", adminId)
                        .with(csrf())
                        .header("Authorization", bearerToken(adminUsername, PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("At least one active administrator is required"));

        assertThat(userRepository.findById(adminId).orElseThrow().isActive()).isTrue();
    }

    @Test
    void inactiveUserCannotLoginAndCreatesAuditLog() throws Exception {
        mockMvc.perform(patch("/admin/users/{id}/deactivate", analystId)
                        .with(csrf())
                        .header("Authorization", bearerToken(adminUsername, PASSWORD)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(analystUsername, PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));

        assertThat(auditLogRepository.findAll().stream().map(AuditLog::getAction))
                .contains("LOGIN_BLOCKED_INACTIVE_USER");
    }

    @Test
    void adminCanReadAuditLogsAndSecurityAlertsAsDtos() throws Exception {
        AuditLog auditLog = new AuditLog();
        auditLog.setActor("security-test");
        auditLog.setAction("TEST_AUDIT_EVENT");
        auditLog.setTargetType("AUTHENTICATION");
        auditLog.setTargetId(42L);
        auditLog.setDetails("Synthetic audit event for admin evidence endpoint");
        auditLogRepository.save(auditLog);

        SecurityAlert alert = new SecurityAlert();
        alert.setAlertType("TEST_SECURITY_ALERT");
        alert.setSeverity("HIGH");
        alert.setActor("security-test");
        alert.setTargetType("AUTHENTICATION");
        alert.setTargetId(43L);
        alert.setDescription("Synthetic security alert for admin evidence endpoint");
        securityAlertRepository.save(alert);

        mockMvc.perform(get("/admin/audit-logs")
                        .header("Authorization", bearerToken(adminUsername, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action == 'TEST_AUDIT_EVENT')]").exists())
                .andExpect(jsonPath("$[?(@.targetType == 'AUTHENTICATION')]").exists());

        mockMvc.perform(get("/admin/security-alerts")
                        .header("Authorization", bearerToken(adminUsername, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.alertType == 'TEST_SECURITY_ALERT')]").exists())
                .andExpect(jsonPath("$[?(@.severity == 'HIGH')]").exists());
    }

    private User createUser(UserRole role, boolean active) {
        String username = role.name().toLowerCase() + "_" + UUID.randomUUID();
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@ghostreport.test");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setActive(active);
        return userRepository.save(user);
    }

    private String bearerToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = response.replaceAll(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
        return "Bearer " + token;
    }
}
