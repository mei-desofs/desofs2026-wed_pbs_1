package com.ghostreport.security;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FrontendNavbarVisibilityTest {

    private static final Path STATIC_ROOT = Path.of("src/main/resources/static");

    @Test
    void authenticatedRoleNavsAreHiddenBeforeLogin() throws Exception {
        String style = Files.readString(STATIC_ROOT.resolve("css/style.css"));
        String admin = Files.readString(STATIC_ROOT.resolve("admin.html"));
        String analyst = Files.readString(STATIC_ROOT.resolve("analyst.html"));
        String auditor = Files.readString(STATIC_ROOT.resolve("auditor.html"));
        String adminJs = Files.readString(STATIC_ROOT.resolve("js/admin.js"));
        String analystJs = Files.readString(STATIC_ROOT.resolve("js/analyst.js"));
        String auditorJs = Files.readString(STATIC_ROOT.resolve("js/auditor.js"));

        assertThat(style).contains(".hidden,\n[hidden] {\n    display: none !important;");
        assertThat(admin)
                .contains("id=\"adminNav\" class=\"admin-nav hidden\"")
                .contains("id=\"mfaSection\" class=\"hidden\"")
                .contains("id=\"editUserPage\"")
                .doesNotContain("mfaDevCode")
                .doesNotContain("value=\"USER\"");
        assertThat(analyst).contains("id=\"analystNav\" class=\"analyst-nav hidden\"");
        assertThat(auditor).contains("id=\"auditorNav\" class=\"admin-nav hidden\"");
        assertThat(adminJs)
                .contains("classList.remove(\"hidden\")")
                .contains("classList.add(\"hidden\")")
                .contains("loginData.mfaRequired")
                .contains("cancelAdminMfa")
                .contains("#/admin/users/")
                .doesNotContain("devMfaCode")
                .doesNotContain("updateUser: () => updateUser");
        assertThat(analystJs).contains("classList.remove(\"hidden\")");
        assertThat(auditorJs).contains("classList.remove(\"hidden\")");
    }

    @Test
    void reporterModelDoesNotExposeAuthenticatedUserRole() throws Exception {
        String userRole = Files.readString(Path.of("src/main/java/com/ghostreport/model/UserRole.java"));
        String validation = Files.readString(Path.of("src/main/java/com/ghostreport/validation/ValidationConstants.java"));
        String dataInitializer = Files.readString(Path.of("src/main/java/com/ghostreport/config/DataInitializer.java"));

        assertThat(userRole).doesNotContain("USER");
        assertThat(validation).doesNotContain("USER|");
        assertThat(dataInitializer).doesNotContain("UserPassword123");
    }
}
