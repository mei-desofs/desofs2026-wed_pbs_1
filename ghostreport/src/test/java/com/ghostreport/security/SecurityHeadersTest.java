package com.ghostreport.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicPageIncludesSecurityHeaders() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("style-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("object-src 'none'")))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Content-Security-Policy", containsString("base-uri 'none'")))
                .andExpect(header().string("Content-Security-Policy", containsString("form-action 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("upgrade-insecure-requests")))
                .andExpect(header().string("Content-Security-Policy", containsString("report-uri /security/csp-report")))
                .andExpect(header().string("Content-Security-Policy", not(containsString("unsafe-inline"))))
                .andExpect(header().string("Content-Security-Policy", not(containsString("unsafe-eval"))))
                .andExpect(header().string("Permissions-Policy", containsString("geolocation=()")))
                .andExpect(header().string("Cross-Origin-Opener-Policy", "same-origin"))
                .andExpect(header().string("Cross-Origin-Resource-Policy", "same-origin"))
                .andExpect(header().string("Cross-Origin-Embedder-Policy", "require-corp"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().doesNotExist("Server"));
    }

    @Test
    void cspReportsAreAcceptedWithoutAuthenticationOrCsrfToken() throws Exception {
        mockMvc.perform(post("/security/csp-report")
                        .contentType("application/csp-report")
                        .content("""
                                {
                                  "csp-report": {
                                    "document-uri": "https://ghostreport.example/index.html",
                                    "violated-directive": "script-src",
                                    "blocked-uri": "inline",
                                    "trackingCode": "GR-abcdefghijklmnopqrstuvwxyz"
                                  }
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.message").value("Report accepted"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void sensitiveApiResponsesAreNotCacheable() throws Exception {
        mockMvc.perform(get("/admin/panel"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Cache-Control", containsString("no-cache")))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", containsString("1970")));
    }

    @Test
    void crossSiteUnsafeRequestsAreRejectedBeforeControllerHandling() throws Exception {
        mockMvc.perform(post("/reports")
                        .with(csrf())
                        .header("Sec-Fetch-Site", "cross-site")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Cross-site request rejected"));
    }

    @Test
    void traceAndOversizedAuthorizationHeadersAreRejected() throws Exception {
        mockMvc.perform(request(HttpMethod.TRACE, "/admin/panel"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/admin/panel")
                        .header("Authorization", "Bearer " + "a".repeat(9000)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request headers"));
    }

    @Test
    void duplicatedScalarParametersAreRejectedBeforeControllerHandling() throws Exception {
        mockMvc.perform(get("/admin/panel?role=ADMIN&role=AUDITOR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Ambiguous request parameters"));
    }
}
