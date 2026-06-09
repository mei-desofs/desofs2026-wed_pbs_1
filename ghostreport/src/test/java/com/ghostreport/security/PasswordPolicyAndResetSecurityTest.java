package com.ghostreport.security;

import com.ghostreport.model.PasswordHistory;
import com.ghostreport.model.PasswordResetToken;
import com.ghostreport.model.User;
import com.ghostreport.model.UserRole;
import com.ghostreport.repository.AuditLogRepository;
import com.ghostreport.repository.PasswordHistoryRepository;
import com.ghostreport.repository.PasswordResetTokenRepository;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:password-policy-reset-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "ghostreport.backup-dir=target/test-backups/password-policy-reset",
        "app.upload-dir=target/test-uploads/password-policy-reset",
        "ghostreport.backup-enabled=true",
        "ghostreport.password-reset.expose-token=true",
        "ghostreport.password-reset.token-ttl-minutes=30"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PasswordPolicyAndResetSecurityTest {

    private static final String CURRENT_PASSWORD = "CurrentPassword123!";
    private static final String NEW_PASSWORD = "NewPassword123!";
    private static final String SECOND_NEW_PASSWORD = "SecondNewPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String username;
    private String email;

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
        passwordHistoryRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();

        username = "password_user_" + UUID.randomUUID();
        email = username + "@ghostreport.test";
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(CURRENT_PASSWORD));
        user.setRole(UserRole.ANALYST);
        user.setActive(true);
        User saved = userRepository.save(user);

        PasswordHistory history = new PasswordHistory();
        history.setUser(saved);
        history.setPasswordHash(saved.getPasswordHash());
        passwordHistoryRepository.save(history);
    }

    @Test
    void compromisedPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/auth/password/change")
                        .with(csrf())
                        .header("Authorization", bearerToken(username, CURRENT_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "%s",
                                  "newPassword": "P@ssw0rd1234!"
                                }
                                """.formatted(CURRENT_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Password is compromised"));
    }

    @Test
    void reusedPasswordIsRejected() throws Exception {
        changePassword(CURRENT_PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/password/change")
                        .with(csrf())
                        .header("Authorization", bearerToken(username, NEW_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "%s",
                                  "newPassword": "%s"
                                }
                                """.formatted(NEW_PASSWORD, CURRENT_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Password was already used"));
    }

    @Test
    void authenticatedPasswordChangeRequiresCurrentPassword() throws Exception {
        mockMvc.perform(post("/auth/password/change")
                        .with(csrf())
                        .header("Authorization", bearerToken(username, CURRENT_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "WrongPassword123!",
                                  "newPassword": "%s"
                                }
                                """.formatted(NEW_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Current password is invalid"));
    }

    @Test
    void changedPasswordIsStoredHashed() throws Exception {
        changePassword(CURRENT_PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

        User updated = userRepository.findByUsername(username).orElseThrow();
        assertThat(updated.getPasswordHash()).isNotEqualTo(NEW_PASSWORD);
        assertThat(passwordEncoder.matches(NEW_PASSWORD, updated.getPasswordHash())).isTrue();
    }

    @Test
    void expiredResetTokenIsRejected() throws Exception {
        String token = requestResetToken();
        PasswordResetToken storedToken = passwordResetTokenRepository.findAll().get(0);
        storedToken.setExpiresAt(LocalDateTime.now().minusHours(2));
        passwordResetTokenRepository.save(storedToken);

        confirmReset(token, NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid or expired reset token"));
    }

    @Test
    void reusedResetTokenIsRejected() throws Exception {
        String token = requestResetToken();

        confirmReset(token, NEW_PASSWORD).andExpect(status().isNoContent());

        confirmReset(token, SECOND_NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid or expired reset token"));
    }

    private org.springframework.test.web.servlet.ResultActions changePassword(
            String currentPassword,
            String newPassword
    ) throws Exception {
        return mockMvc.perform(post("/auth/password/change")
                .with(csrf())
                .header("Authorization", bearerToken(username, currentPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "currentPassword": "%s",
                          "newPassword": "%s"
                        }
                        """.formatted(currentPassword, newPassword)));
    }

    private String requestResetToken() throws Exception {
        String response = mockMvc.perform(post("/auth/password-reset/request")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "%s"
                                }
                                """.formatted(email)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.resetToken").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return response.replaceAll(".*\\\"resetToken\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }

    private org.springframework.test.web.servlet.ResultActions confirmReset(
            String token,
            String newPassword
    ) throws Exception {
        return mockMvc.perform(post("/auth/password-reset/confirm")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "token": "%s",
                          "newPassword": "%s"
                        }
                        """.formatted(token, newPassword)));
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
