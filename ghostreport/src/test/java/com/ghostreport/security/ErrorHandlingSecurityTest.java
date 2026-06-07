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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:error-handling-security-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "server.error.include-stacktrace=never",
        "ghostreport.backup-dir=target/test-backups/error-handling-security",
        "app.upload-dir=target/test-uploads/error-handling-security",
        "ghostreport.backup-enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorHandlingSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void malformedJsonReturnsGenericErrorWithoutInternals() throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed request"))
                .andExpect(jsonPath("$.correlationId").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertDoesNotExposeInternals(response);
    }

    @Test
    void unauthorizedProtectedEndpointReturnsGenericJsonWithoutInternals() throws Exception {
        String response = mockMvc.perform(get("/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.correlationId").isString())
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
                .doesNotContain("C:\\");
    }
}
