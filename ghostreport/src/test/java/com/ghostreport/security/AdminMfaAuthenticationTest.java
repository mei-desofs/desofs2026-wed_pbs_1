package com.ghostreport.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostreport.model.AuditLog;
import com.ghostreport.model.User;
import com.ghostreport.model.UserRole;
import com.ghostreport.repository.AuditLogRepository;
import com.ghostreport.repository.UserRepository;
import com.ghostreport.service.MfaChallengeService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-mfa-authentication-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "ghostreport.backup-dir=target/test-backups/admin-mfa-authentication",
        "app.upload-dir=target/test-uploads/admin-mfa-authentication",
        "ghostreport.backup-enabled=true",
        "ghostreport.mfa.enabled=true",
        "ghostreport.mfa.required-roles=ADMIN,ANALYST,AUDITOR",
        "ghostreport.mfa.code-ttl-seconds=1",
        "ghostreport.mfa.max-attempts=5",
        "ghostreport.mfa.expose-code=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminMfaAuthenticationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MfaChallengeService mfaChallengeService;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminUsername;
    private String analystUsername;
    private String auditorUsername;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        String suffix = UUID.randomUUID().toString();
        adminUsername = createUser("mfa_admin_" + suffix, UserRole.ADMIN);
        analystUsername = createUser("mfa_analyst_" + suffix, UserRole.ANALYST);
        auditorUsername = createUser("mfa_auditor_" + suffix, UserRole.AUDITOR);
    }

    @Test
    void adminPasswordLoginRequiresMfaAndCannotAccessAdminRoutesBeforeVerification() throws Exception {
        JsonNode challenge = login(adminUsername);

        assertThat(challenge.path("mfaRequired").asBoolean()).isTrue();
        assertThat(challenge.path("token").isNull()).isTrue();
        assertThat(challenge.path("mfaChallengeId").asText()).isNotBlank();
        assertThat(challenge.has("devMfaCode")).isFalse();
        assertThat(mfaCode(challenge)).matches("\\d{6}");

        mockMvc.perform(get("/admin/panel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanCompleteMfaAndUseAdminRoutes() throws Exception {
        JsonNode challenge = login(adminUsername);
        JsonNode verified = verifyMfa(challenge.path("mfaChallengeId").asText(), mfaCode(challenge))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(false))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturnJson();

        String token = verified.path("token").asText();
        mockMvc.perform(get("/admin/panel").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertThat(auditLogRepository.findAll().stream().map(AuditLog::getAction))
                .contains("MFA_CHALLENGE_CREATED", "MFA_VERIFY_SUCCESS", "LOGIN_SUCCESS");
    }

    @Test
    void invalidExpiredAndReusedMfaCodesDoNotIssueToken() throws Exception {
        JsonNode invalidChallenge = login(adminUsername);
        verifyMfa(invalidChallenge.path("mfaChallengeId").asText(), "000000")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));

        JsonNode reusableChallenge = login(adminUsername);
        String challengeId = reusableChallenge.path("mfaChallengeId").asText();
        String code = mfaCode(reusableChallenge);
        verifyMfa(challengeId, code).andExpect(status().isOk());
        verifyMfa(challengeId, code)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));

        JsonNode expiredChallenge = login(adminUsername);
        String expiredChallengeId = expiredChallenge.path("mfaChallengeId").asText();
        mfaChallengeService.expireChallengeForTesting(expiredChallengeId);
        verifyMfa(expiredChallengeId, mfaCode(expiredChallenge))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));

        assertThat(auditLogRepository.findAll().stream().map(AuditLog::getAction))
                .contains("MFA_VERIFY_REJECTED", "MFA_VERIFY_EXPIRED");
    }

    @Test
    void mfaChallengeIsInvalidatedAfterTooManyWrongCodes() throws Exception {
        JsonNode challenge = login(adminUsername);
        String challengeId = challenge.path("mfaChallengeId").asText();
        String correctCode = mfaCode(challenge);

        for (int attempt = 0; attempt < 5; attempt++) {
            verifyMfa(challengeId, "000000")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Invalid credentials"));
        }

        verifyMfa(challengeId, correctCode)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));

        assertThat(auditLogRepository.findAll().stream().map(AuditLog::getAction))
                .contains("MFA_VERIFY_LOCKED");
    }

    @Test
    void analystAndAuditorPasswordLoginsRequireMfaAndCannotUseWrongRoleRoutes() throws Exception {
        JsonNode analystChallenge = login(analystUsername);
        assertThat(analystChallenge.path("mfaRequired").asBoolean()).isTrue();
        assertThat(analystChallenge.path("token").isNull()).isTrue();
        mockMvc.perform(get("/analyst/panel"))
                .andExpect(status().isUnauthorized());

        JsonNode analystLogin = verifyMfa(analystChallenge.path("mfaChallengeId").asText(), mfaCode(analystChallenge))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ANALYST"))
                .andReturnJson();
        mockMvc.perform(get("/analyst/panel")
                        .header("Authorization", "Bearer " + analystLogin.path("token").asText()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/panel")
                        .header("Authorization", "Bearer " + analystLogin.path("token").asText()))
                .andExpect(status().isForbidden());

        JsonNode auditorChallenge = login(auditorUsername);
        assertThat(auditorChallenge.path("mfaRequired").asBoolean()).isTrue();
        assertThat(auditorChallenge.path("token").isNull()).isTrue();
        mockMvc.perform(get("/audit/logs"))
                .andExpect(status().isUnauthorized());

        JsonNode auditorLogin = verifyMfa(auditorChallenge.path("mfaChallengeId").asText(), mfaCode(auditorChallenge))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("AUDITOR"))
                .andReturnJson();
        mockMvc.perform(get("/audit/logs")
                        .header("Authorization", "Bearer " + auditorLogin.path("token").asText()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/panel")
                        .header("Authorization", "Bearer " + auditorLogin.path("token").asText()))
                .andExpect(status().isForbidden());
    }

    @Test
    void reporterEndpointsRemainAnonymousAndDoNotRequireUserRoleLogin() throws Exception {
        mockMvc.perform(post("/reports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Anonymous report",
                                  "description": "Reporter can submit without an account.",
                                  "category": "Ethics"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingCode").isNotEmpty());
    }

    private String createUser(String username, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@ghostreport.test");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setActive(true);
        return userRepository.save(user).getUsername();
    }

    private JsonNode login(String username) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private String mfaCode(JsonNode challenge) {
        String challengeId = challenge.path("mfaChallengeId").asText();
        assertThat(challengeId).isNotBlank();
        String code = mfaChallengeService.getExposedCodeForTesting(challengeId);
        assertThat(code).isNotBlank();
        return code;
    }

    private Result verifyMfa(String challengeId, String code) throws Exception {
        return new Result(mockMvc.perform(post("/auth/mfa/verify")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "challengeId": "%s",
                          "code": "%s"
                        }
                        """.formatted(challengeId, code))));
    }

    private final class Result {
        private final org.springframework.test.web.servlet.ResultActions actions;

        private Result(org.springframework.test.web.servlet.ResultActions actions) {
            this.actions = actions;
        }

        private Result andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
            actions.andExpect(matcher);
            return this;
        }

        private JsonNode andReturnJson() throws Exception {
            return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString());
        }
    }
}
