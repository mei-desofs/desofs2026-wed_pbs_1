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

        assertThat(style).contains(".hidden,\n[hidden] {\n    display: none !important;");
        assertThat(admin).contains("id=\"adminNav\" class=\"admin-nav hidden\"");
        assertThat(analyst).contains("id=\"analystNav\" class=\"analyst-nav hidden\"");
        assertThat(auditor).contains("id=\"auditorNav\" class=\"admin-nav hidden\"");
    }
}
