package com.ghostreport.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:rbac-authorization-matrix-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "ghostreport.backup-dir=target/test-backups/rbac-authorization-matrix",
        "app.upload-dir=target/test-uploads/rbac-authorization-matrix",
        "ghostreport.backup-enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RbacAuthorizationMatrixTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String analystUsername;
    private String adminUsername;
    private String auditorUsername;
    private Long reportId;
    private String trackingCode;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString();
        analystUsername = createUser("rbac_analyst_" + suffix, UserRole.ANALYST);
        adminUsername = createUser("rbac_admin_" + suffix, UserRole.ADMIN);
        auditorUsername = createUser("rbac_auditor_" + suffix, UserRole.AUDITOR);

        JsonNode createdReport = createPublicReport();
        reportId = createdReport.get("id").asLong();
        trackingCode = createdReport.get("trackingCode").asText();
    }

    @Test
    void publicReportEndpointsRemainAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Public RBAC report",
                                  "description": "Report submitted by an unauthenticated citizen.",
                                  "category": "Security"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.trackingCode").isNotEmpty())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        mockMvc.perform(post("/reports/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "trackingCode": "%s"
                                }
                                """.formatted(trackingCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void anonymousUserCannotAccessProtectedRoleEndpoints() throws Exception {
        mockMvc.perform(get("/analyst/panel"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/admin/panel"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/audit/logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void analystCanUseAnalysisEndpointsButCannotUseAdminOrRestrictedAuditEndpoints() throws Exception {
        mockMvc.perform(get("/analyst/panel")
                        .header("Authorization", bearerToken(analystUsername, PASSWORD)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/analyst/reports")
                        .header("Authorization", bearerToken(analystUsername, PASSWORD)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/analyst/reports/{id}/assign", reportId)
                        .header("Authorization", bearerToken(analystUsername, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(reportId))
                .andExpect(jsonPath("$.assignedAnalystUsername").value(analystUsername));

        mockMvc.perform(patch("/analyst/reports/{id}/status", reportId)
                        .header("Authorization", bearerToken(analystUsername, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "UNDER_REVIEW"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId))
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        mockMvc.perform(get("/admin/panel")
                        .header("Authorization", bearerToken(analystUsername, PASSWORD)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/users")
                        .header("Authorization", bearerToken(analystUsername, PASSWORD)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/audit/logs")
                        .header("Authorization", bearerToken(analystUsername, PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUseAdminEndpointsAndHasOversightAccess() throws Exception {
        mockMvc.perform(get("/admin/panel")
                        .header("Authorization", bearerToken(adminUsername, PASSWORD)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/users")
                        .header("Authorization", bearerToken(adminUsername, PASSWORD)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/analyst/panel")
                        .header("Authorization", bearerToken(adminUsername, PASSWORD)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/audit/logs")
                        .header("Authorization", bearerToken(adminUsername, PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void auditorCanReadAuditEndpointsOnlyAndCannotChangeReportsOrRunAdminActions() throws Exception {
        mockMvc.perform(get("/audit/logs")
                        .header("Authorization", bearerToken(auditorUsername, PASSWORD)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/audit/security-alerts")
                        .header("Authorization", bearerToken(auditorUsername, PASSWORD)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/audit/cases/closed")
                        .header("Authorization", bearerToken(auditorUsername, PASSWORD)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/panel")
                        .header("Authorization", bearerToken(auditorUsername, PASSWORD)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/users")
                        .header("Authorization", bearerToken(auditorUsername, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "auditor-created-user",
                                  "email": "auditor-created-user@ghostreport.test",
                                  "password": "Password123!",
                                  "role": "ANALYST"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/analyst/reports/{id}/status", reportId)
                        .header("Authorization", bearerToken(auditorUsername, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/analyst/reports/{id}/assign", reportId)
                        .header("Authorization", bearerToken(auditorUsername, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    private String createUser(String username, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@ghostreport.test");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setActive(true);
        userRepository.save(user);
        return username;
    }

    private JsonNode createPublicReport() throws Exception {
        String body = mockMvc.perform(post("/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Controlled RBAC report",
                                  "description": "Report used by RBAC authorization tests.",
                                  "category": "Security"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(body);
    }
    private String bearerToken(String username, String password) throws Exception {
        String body = """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);

        String response = mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = response.replaceAll(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
        return "Bearer " + token;
    }
}