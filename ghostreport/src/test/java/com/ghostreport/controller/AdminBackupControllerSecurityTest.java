package com.ghostreport.controller;

import com.ghostreport.repository.SecurityAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:backup-controller-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "ghostreport.backup-dir=target/test-backups/controller",
        "app.upload-dir=target/test-uploads/controller",
        "ghostreport.backup-enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminBackupControllerSecurityTest {

    private static final Path BACKUP_DIR = Path.of("target/test-backups/controller");
    private static final Path UPLOAD_DIR = Path.of("target/test-uploads/controller");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityAlertRepository securityAlertRepository;

    @BeforeEach
    void setUp() throws Exception {
        cleanDirectory(BACKUP_DIR);
        cleanDirectory(UPLOAD_DIR);
        Files.createDirectories(BACKUP_DIR);
        Files.createDirectories(UPLOAD_DIR);
        securityAlertRepository.deleteAll();
    }

    @Test
    void backupEndpointsRequireAdminRole() throws Exception {
        mockMvc.perform(get("/admin/backups"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/admin/backups").header("Authorization", bearerToken("analyst", "Analyst123!")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/backups").header("Authorization", bearerToken("admin", "Admin123!")))
                .andExpect(status().isOk());

        assertThat(securityAlertRepository.findAll())
                .anyMatch(alert -> alert.getAlertType().equals("BACKUP_UNAUTHORIZED_ATTEMPT"));
    }

    @Test
    void adminCanCreateAndVerifyBackupThroughEndpoints() throws Exception {
        String body = mockMvc.perform(post("/admin/backups").header("Authorization", bearerToken("admin", "Admin123!")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String filename = body.replaceAll(".*\"filename\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/admin/backups/{filename}/verify", filename).header("Authorization", bearerToken("admin", "Admin123!")))
                .andExpect(status().isOk());
    }

    @Test
    void pathTraversalInEndpointFilenameIsRejected() throws Exception {
        mockMvc.perform(post("/admin/backups/{filename}/verify", "ghostreport-backup-20260507-154500..zip").header("Authorization", bearerToken("admin", "Admin123!")))
                .andExpect(status().isBadRequest());

        assertThat(securityAlertRepository.findAll())
                .anyMatch(alert -> alert.getAlertType().equals("BACKUP_PATH_TRAVERSAL_ATTEMPT"));
    }

    private void cleanDirectory(Path dir) throws Exception {
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
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

        String token = response.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        return "Bearer " + token;
    }
}
