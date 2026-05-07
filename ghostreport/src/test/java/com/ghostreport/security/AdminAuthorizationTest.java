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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    private String username;

    @BeforeEach
    void setup() {

        username = "analyst_" + UUID.randomUUID();

        User analyst = new User();

        analyst.setUsername(username);

        analyst.setEmail(username + "@test.com");

        analyst.setPasswordHash(
                passwordEncoder.encode("password")
        );

        analyst.setRole(UserRole.ANALYST);

        analyst.setActive(true);

        userRepository.save(analyst);
    }

    @Test
    void analystCannotAccessAdminPanel() throws Exception {

        mockMvc.perform(
                        get("/admin/panel")
                                .with(httpBasic(username, "password"))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());
    }
}