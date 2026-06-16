package com.ghostreport.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:authentication-security-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "ghostreport.seed-users.enabled=false",
        "ghostreport.backup-dir=target/test-backups/authentication-security",
        "app.upload-dir=target/test-uploads/authentication-security",
        "ghostreport.backup-enabled=true",
        "security.rate-limit.login.max-attempts=5",
        "security.rate-limit.login.window-seconds=600"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthenticationSecurityIntegrationTest {

    private static final String PASSWORD = "ValidPassword123!";
    private static final String JWT_SECRET = "test-only-change-this-secret-32-chars";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private SecurityAlertRepository securityAlertRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearSecurityRecords() {
        auditLogRepository.deleteAll();
        securityAlertRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void adminCanDeactivateAndActivateUserAndInactiveLoginIsAuditedWithoutDisclosure() throws Exception {
        User admin = createUser("admin_control", UserRole.ADMIN, true);
        User analyst = createUser("analyst_control", UserRole.ANALYST, true);
        String adminBearer = bearerToken(admin.getUsername(), PASSWORD, "10.10.0.1");
        String analystBearer = bearerToken(analyst.getUsername(), PASSWORD, "10.10.0.5");

        mockMvc.perform(patch("/admin/users/{id}/deactivate", analyst.getId())
                        .with(csrf())
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/analyst/panel").header("Authorization", analystBearer))
                .andExpect(status().isUnauthorized());

        String inactiveLoginResponse = login(analyst.getUsername(), PASSWORD, "10.10.0.2")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"))
                .andReturn().getResponse().getContentAsString();

        String invalidPasswordResponse = login(admin.getUsername(), "incorrect-password", "10.10.0.3")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"))
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(inactiveLoginResponse).get("error").asText())
                .isEqualTo(objectMapper.readTree(invalidPasswordResponse).get("error").asText());

        mockMvc.perform(patch("/admin/users/{id}/activate", analyst.getId())
                        .with(csrf())
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        login(analyst.getUsername(), PASSWORD, "10.10.0.4").andExpect(status().isOk());

        assertThat(auditLogRepository.findAll())
                .extracting(log -> log.getAction())
                .contains("USER_DEACTIVATED", "USER_ACTIVATED", "LOGIN_BLOCKED_INACTIVE_USER");
    }

    @Test
    void nonAdminCannotChangeUserActiveStateAndLastAdminCannotBeDisabled() throws Exception {
        User admin = createUser("sole_admin", UserRole.ADMIN, true);
        User analyst = createUser("plain_analyst", UserRole.ANALYST, true);

        mockMvc.perform(patch("/admin/users/{id}/deactivate", admin.getId())
                        .with(csrf())
                        .header("Authorization", bearerToken(analyst.getUsername(), PASSWORD, "10.10.1.1")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/admin/users/{id}/deactivate", admin.getId())
                        .with(csrf())
                        .header("Authorization", bearerToken(admin.getUsername(), PASSWORD, "10.10.1.2")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Request conflict"));
    }

    @Test
    void repeatedFailedLoginIsRateLimitedAndProducesSingleNonSensitiveAlert() throws Exception {
        createUser("rate_limited_user", UserRole.ANALYST, true);

        for (int attempt = 0; attempt < 5; attempt++) {
            login("rate_limited_user", "wrong-password", "10.10.2.1")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Invalid credentials"));
        }

        login("rate_limited_user", "wrong-password", "10.10.2.1")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too many requests"));

        assertThat(securityAlertRepository.findAll())
                .filteredOn(alert -> "BRUTE_FORCE_LOGIN_ATTEMPT".equals(alert.getAlertType()))
                .hasSize(1)
                .allSatisfy(alert -> {
                    assertThat(alert.getDescription()).doesNotContain("rate_limited_user");
                    assertThat(alert.getDescription()).doesNotContain("wrong-password");
                });
    }

    @Test
    void adminCreateUserEnforcesPasswordPolicy() throws Exception {
        User admin = createUser("password_admin", UserRole.ADMIN, true);
        String bearer = bearerToken(admin.getUsername(), PASSWORD, "10.10.3.1");
        String[] invalidPasswords = {
                "Short1!"
        };

        for (int index = 0; index < invalidPasswords.length; index++) {
            mockMvc.perform(post("/admin/users")
                            .with(csrf())
                            .header("Authorization", bearer)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createUserBody("invalid_" + index, invalidPasswords[index])))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fields.password").exists());
        }

        mockMvc.perform(post("/admin/users")
                        .with(csrf())
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody("valid_user", "long lowercase password")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("valid_user"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void jwtFailuresReturnControlled401AndValidWrongRoleReturnsControlled403() throws Exception {
        User analyst = createUser("jwt_analyst", UserRole.ANALYST, true);
        User auditor = createUser("jwt_auditor", UserRole.AUDITOR, true);
        long now = Instant.now().getEpochSecond();

        assertUnauthorized(token(Map.of(
                "sub", analyst.getUsername(), "role", "ANALYST", "iat", now - 120, "exp", now - 1
        )));
        assertUnauthorized(token(Map.of(
                "sub", analyst.getUsername(), "iat", now, "exp", now + 600
        )));
        assertUnauthorized(token(Map.of(
                "sub", analyst.getUsername(), "role", "ADMIN", "iat", now, "exp", now + 600
        )));
        assertUnauthorized("malformed");
        assertUnauthorized("abc.def.ghi");

        String validToken = token(Map.of(
                "sub", analyst.getUsername(), "role", "ANALYST", "iat", now, "exp", now + 600
        ));
        String tampered = validToken.substring(0, validToken.length() - 1)
                + (validToken.endsWith("A") ? "B" : "A");
        assertUnauthorized(tampered);

        mockMvc.perform(get("/admin/panel")
                        .header("Authorization", bearerToken(auditor.getUsername(), PASSWORD, "10.10.4.1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"))
                .andExpect(jsonPath("$.path").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void malformedRequestDoesNotLeakImplementationDetails() throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .with(remoteAddress("10.10.5.1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed request"))
                .andExpect(jsonPath("$.path").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .doesNotContain("java.", "SQLException", "Exception", "C:\\", "/src/");
    }

    private void assertUnauthorized(String token) throws Exception {
        mockMvc.perform(get("/analyst/panel").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    private org.springframework.test.web.servlet.ResultActions login(String username, String password, String ip)
            throws Exception {
        return mockMvc.perform(post("/auth/login")
                .with(remoteAddress(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s"}
                        """.formatted(username, password)));
    }

    private String bearerToken(String username, String password, String ip) throws Exception {
        String response = login(username, password, ip)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(response).get("token").asText();
    }

    private String createUserBody(String username, String password) {
        return """
                {
                  "username":"%s",
                  "email":"%s@ghostreport.test",
                  "password":"%s",
                  "role":"ANALYST"
                }
                """.formatted(username, username, password);
    }

    private User createUser(String username, UserRole role, boolean active) {
        User user = new User();
        user.setUsername(username + "_" + UUID.randomUUID());
        user.setEmail(user.getUsername() + "@ghostreport.test");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setActive(active);
        return userRepository.save(user);
    }

    private String token(Map<String, Object> claims) throws Exception {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        String unsigned = base64(objectMapper.writeValueAsBytes(header)) + "."
                + base64(objectMapper.writeValueAsBytes(claims));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return unsigned + "." + base64(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private RequestPostProcessor remoteAddress(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }
}
