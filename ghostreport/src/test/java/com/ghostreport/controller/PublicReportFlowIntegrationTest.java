package com.ghostreport.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostreport.repository.AttachmentRepository;
import com.ghostreport.repository.ReportRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:public-report-flow-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "server.error.include-stacktrace=never",
        "ghostreport.backup-dir=target/test-backups/public-report-flow",
        "app.upload-dir=target/test-uploads/public-report-flow",
        "ghostreport.backup-enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicReportFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Value("${app.upload-dir}")
    private String uploadDir;

    private Path uploadBase;

    @BeforeEach
    void setup() throws Exception {
        uploadBase = Path.of(uploadDir).toAbsolutePath().normalize();
        attachmentRepository.deleteAll();
        reportRepository.deleteAll();
        deleteRecursively(uploadBase);
        Files.createDirectories(uploadBase);
    }

    @AfterEach
    void cleanup() throws Exception {
        attachmentRepository.deleteAll();
        reportRepository.deleteAll();
        deleteRecursively(uploadBase);
    }

    @Test
    void createReportWithValidPayloadCreatesReportAndReturnsTrackingCodeWithoutInternalHash() throws Exception {
        String response = createReport("Payroll fraud", "Controlled public report description.", "Fraud")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.trackingCode").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        String trackingCode = json.get("trackingCode").asText();
        Long reportId = json.get("id").asLong();

        assertEquals(1, reportRepository.count());
        assertTrue(trackingCode.matches("GR-[A-Za-z0-9_-]{20,}"));
        assertFalse(response.contains("trackingCodeHash"));
        assertFalse(response.contains("hash"));
        assertTrue(
                Files.exists(uploadBase.resolve("reports").resolve(String.valueOf(reportId)).resolve("documents")),
                "Expected report document directory to be generated"
        );
    }

    @Test
    void createReportWithInvalidPayloadReturnsBadRequestAndDoesNotCreateReport() throws Exception {
        String response = mockMvc.perform(post("/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "",
                                "description", "",
                                "category", "Fraud"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(0, reportRepository.count());
        assertDoesNotExposeSensitiveData(response);
    }

    @Test
    void verifyWithValidTrackingCodeReturnsReportDataWithoutSensitiveFields() throws Exception {
        JsonNode created = objectMapper.readTree(
                createReport("Procurement issue", "Supplier conflict evidence.", "Procurement")
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        );

        String response = mockMvc.perform(post("/reports/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "trackingCode", created.get("trackingCode").asText()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.get("id").asLong()))
                .andExpect(jsonPath("$.title").value("Procurement issue"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.category").value("Procurement"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertFalse(response.contains("trackingCode"));
        assertFalse(response.contains("trackingCodeHash"));
        assertFalse(response.contains("hash"));
    }

    @Test
    void verifyWithUnknownTrackingCodeReturnsControlledErrorWithoutStackTrace() throws Exception {
        String response = mockMvc.perform(post("/reports/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "trackingCode", "GR-aaaaaaaaaaaaaaaaaaaa"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Denúncia não encontrada"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertDoesNotExposeSensitiveData(response);
    }

    @Test
    void verifyWithInvalidTrackingCodeFormatReturnsSafeError() throws Exception {
        String response = mockMvc.perform(post("/reports/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "trackingCode", "../invalid"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Código inválido"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertDoesNotExposeSensitiveData(response);
    }

    @Test
    void publicReportEndpointsRemainAccessibleWithoutAuthentication() throws Exception {
        JsonNode created = objectMapper.readTree(
                createReport("Anonymous public report", "Created without authentication.", "Ethics")
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        );

        mockMvc.perform(post("/reports/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "trackingCode", created.get("trackingCode").asText()
                        ))))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions createReport(
            String title,
            String description,
            String category
    ) throws Exception {
        return mockMvc.perform(post("/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "title", title,
                        "description", description,
                        "category", category
                ))));
    }

    private void assertDoesNotExposeSensitiveData(String response) {
        assertFalse(response.contains(uploadBase.toString()));
        assertFalse(response.contains("trackingCodeHash"));
        assertFalse(response.contains("java."));
        assertFalse(response.contains("org.springframework"));
        assertFalse(response.contains("stackTrace"));
        assertFalse(response.contains("trace"));
    }

    private void deleteRecursively(Path path) throws Exception {
        if (path == null || !Files.exists(path)) {
            return;
        }

        try (var files = Files.walk(path)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(file);
            }
        }
    }
}
