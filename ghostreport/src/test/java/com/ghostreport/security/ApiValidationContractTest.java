package com.ghostreport.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:api-validation-contract-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "server.error.include-stacktrace=never",
        "ghostreport.backup-dir=target/test-backups/api-validation-contract",
        "app.upload-dir=target/test-uploads/api-validation-contract",
        "ghostreport.backup-enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiValidationContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void invalidReportCategoryReturnsSafeValidationError() throws Exception {
        String response = mockMvc.perform(post("/reports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Valid report title",
                                  "description": "This report has enough safe description text.",
                                  "category": "DROP_TABLE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Invalid request"))
                .andExpect(jsonPath("$.fields.category").value("Invalid value"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertDoesNotExposeInternals(response);
    }

    @Test
    void tooLongReportTitleReturnsSafeValidationError() throws Exception {
        String response = mockMvc.perform(post("/reports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "This report has enough safe description text.",
                                  "category": "Security"
                                }
                                """.formatted("A".repeat(201))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Invalid request"))
                .andExpect(jsonPath("$.fields.title").value("Invalid value"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertDoesNotExposeInternals(response);
    }

    @Test
    void blankRequiredFieldReturnsSafeValidationError() throws Exception {
        String response = mockMvc.perform(post("/reports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Valid report title",
                                  "description": "",
                                  "category": "Security"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Invalid request"))
                .andExpect(jsonPath("$.fields.description").value("Invalid value"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertDoesNotExposeInternals(response);
    }

    @Test
    void invalidStatusEnumReturnsSafeValidationError() throws Exception {
        String response = mockMvc.perform(patch("/analyst/reports/999/status")
                        .with(csrf())
                        .with(user("analyst").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ARCHIVED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Invalid request"))
                .andExpect(jsonPath("$.fields.status").value("Invalid value"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertDoesNotExposeInternals(response);
    }

    @Test
    void invalidAdminRoleEnumReturnsSafeValidationError() throws Exception {
        String response = mockMvc.perform(post("/admin/users")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "valid_user",
                                  "email": "valid@example.test",
                                  "password": "ValidPassw0rd!",
                                  "role": "OWNER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Invalid request"))
                .andExpect(jsonPath("$.fields.role").value("Invalid value"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertDoesNotExposeInternals(response);
    }

    @Test
    void unsupportedContentTypeReturnsJsonContractError() throws Exception {
        String response = mockMvc.perform(post("/reports")
                        .with(csrf())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unsupported media type"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertDoesNotExposeInternals(response);
    }

    @Test
    void downloadRequestRequiresPositiveAttachmentIdAndTrackingCodeFormat() throws Exception {
        String response = mockMvc.perform(post("/reports/download")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "trackingCode": "invalid",
                                  "attachmentId": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Invalid request"))
                .andExpect(jsonPath("$.fields.trackingCode").value("Invalid value"))
                .andExpect(jsonPath("$.fields.attachmentId").value("Invalid value"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertDoesNotExposeInternals(response);
    }

    private void assertDoesNotExposeInternals(String response) {
        assertThat(response)
                .doesNotContain("stackTrace")
                .doesNotContain("trace")
                .doesNotContain("java.")
                .doesNotContain("org.springframework")
                .doesNotContain("SQLException")
                .doesNotContain("C:\\");
    }
}
