package com.ghostreport.security;

import com.ghostreport.model.User;
import com.ghostreport.model.UserRole;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:csrf-security-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "ghostreport.backup-dir=target/test-backups/csrf-security",
        "app.upload-dir=target/test-uploads/csrf-security",
        "ghostreport.backup-enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CsrfSecurityTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminUsername;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        adminUsername = "csrf_admin_" + UUID.randomUUID();
        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setEmail(adminUsername + "@ghostreport.test");
        admin.setPasswordHash(passwordEncoder.encode(PASSWORD));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);
    }

    @Test
    void authenticatedPostWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUserBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedPostWithCsrfIsAccepted() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .with(csrf())
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUserBody()))
                .andExpect(status().isCreated());
    }

    private String bearerToken() throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(adminUsername, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = response.replaceAll(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
        return "Bearer " + token;
    }

    private String newUserBody() {
        String username = "csrf_analyst_" + UUID.randomUUID();
        return """
                {
                  "username": "%s",
                  "email": "%s@ghostreport.test",
                  "password": "Password123!",
                  "role": "ANALYST"
                }
                """.formatted(username, username);
    }
}
