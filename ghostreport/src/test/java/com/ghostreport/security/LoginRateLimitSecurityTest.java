package com.ghostreport.security;

import com.ghostreport.model.SecurityAlert;
import com.ghostreport.model.User;
import com.ghostreport.model.UserRole;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:login-rate-limit-security-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "security.rate-limit.login.max-attempts=2",
        "security.rate-limit.login.window-seconds=600",
        "ghostreport.backup-dir=target/test-backups/login-rate-limit-security",
        "app.upload-dir=target/test-uploads/login-rate-limit-security",
        "ghostreport.backup-enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LoginRateLimitSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityAlertRepository securityAlertRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String username;
    private String clientIp;

    @BeforeEach
    void setUp() {
        securityAlertRepository.deleteAll();
        userRepository.deleteAll();

        username = "login_limit_" + UUID.randomUUID();
        clientIp = "192.0.2." + Math.abs(username.hashCode() % 200 + 1);
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@ghostreport.test");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setRole(UserRole.ANALYST);
        user.setActive(true);
        userRepository.save(user);
    }

    @Test
    void repeatedFailedLoginAttemptsCreateAlertAndEventuallyReturn429() throws Exception {
        failedLogin("WrongPassword123!").andExpect(status().isUnauthorized());
        failedLogin("WrongPassword123!").andExpect(status().isUnauthorized());

        assertThat(securityAlertRepository.findAll().stream().map(SecurityAlert::getAlertType))
                .contains("BRUTE_FORCE_LOGIN_ATTEMPT");

        failedLogin("WrongPassword123!")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too many requests"));
    }

    @Test
    void successfulLoginClearsPreviousFailuresForClient() throws Exception {
        failedLogin("WrongPassword123!").andExpect(status().isUnauthorized());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setRemoteAddr(clientIp);
                            return request;
                        })
                        .content(loginBody("Password123!")))
                .andExpect(status().isOk());

        failedLogin("WrongPassword123!").andExpect(status().isUnauthorized());
    }

    @Test
    void loginRateLimitIsScopedToUsernameAndClientAddress() throws Exception {
        failedLogin("WrongPassword123!").andExpect(status().isUnauthorized());
        failedLogin("WrongPassword123!").andExpect(status().isUnauthorized());
        failedLogin("WrongPassword123!").andExpect(status().isTooManyRequests());

        String secondUsername = username + "_second";
        User secondUser = new User();
        secondUser.setUsername(secondUsername);
        secondUser.setEmail(secondUsername + "@ghostreport.test");
        secondUser.setPasswordHash(passwordEncoder.encode("Password123!"));
        secondUser.setRole(UserRole.ANALYST);
        secondUser.setActive(true);
        userRepository.save(secondUser);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setRemoteAddr(clientIp);
                            return request;
                        })
                        .content(loginBody(secondUsername, "Password123!")))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions failedLogin(String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .with(request -> {
                    request.setRemoteAddr(clientIp);
                    return request;
                })
                .content(loginBody(password)));
    }

    private String loginBody(String password) {
        return loginBody(username, password);
    }

    private String loginBody(String loginUsername, String password) {
        return """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(loginUsername, password);
    }
}
