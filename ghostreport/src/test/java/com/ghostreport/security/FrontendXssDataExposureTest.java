package com.ghostreport.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static com.ghostreport.validation.ValidationConstants.REPORT_CATEGORY_ALLOWLIST;

class FrontendXssDataExposureTest {

    private static final Pattern DANGEROUS_DOM_SINK = Pattern.compile(
            "\\b(?:innerHTML|outerHTML|insertAdjacentHTML)\\b|document\\.write\\s*\\(");
    private static final Pattern INLINE_SCRIPT_OR_HANDLER = Pattern.compile(
            "(?is)<script(?![^>]+\\bsrc=)[^>]*>|\\son[a-z]+\\s*=");
    private static final Pattern LOCAL_STORAGE = Pattern.compile("\\blocalStorage\\b");
    private static final Pattern SESSION_STORAGE = Pattern.compile("\\bsessionStorage\\b");
    private static final Pattern TRACKING_CODE_QUERY = Pattern.compile(
            "(?:/track\\.html\\?|[?&](?:code|trackingCode)=|URLSearchParams\\s*\\(|trackingCode[^\\n;]*window\\.location|window\\.location[^\\n;]*trackingCode)");
    private static final Pattern FORM_CONTROL_NAME = Pattern.compile("(?is)<(?:form|input|button|select|textarea)\\b[^>]*\\sname\\s*=");
    private static final Pattern HTML_ID = Pattern.compile("\\bid\\s*=\\s*\"([A-Za-z_$][\\w$-]*)\"");
    private static final Pattern REPORT_CATEGORY_OPTION = Pattern.compile("<option\\s+value=\"([^\"]+)\"");

    @Test
    void frontendDoesNotUseDangerousHtmlParsingSinks() throws IOException {
        Map<Path, String> sources = readStaticFiles(".js");

        List<String> offenders = sources.entrySet().stream()
                .filter(entry -> DANGEROUS_DOM_SINK.matcher(entry.getValue()).find())
                .map(entry -> relativeStaticPath(entry.getKey()))
                .toList();

        assertThat(offenders)
                .as("Frontend must render API, URL and user-controlled values with DOM APIs, not HTML parsing sinks")
                .isEmpty();
    }

    @Test
    void bearerTokensAreNotPersistedInLocalStorage() throws IOException {
        Map<Path, String> sources = readStaticFiles(".js");

        List<String> offenders = sources.entrySet().stream()
                .filter(entry -> LOCAL_STORAGE.matcher(entry.getValue()).find())
                .map(entry -> relativeStaticPath(entry.getKey()))
                .toList();

        assertThat(offenders)
                .as("Bearer tokens must never be persisted in localStorage")
                .isEmpty();
    }

    @Test
    void sessionStorageTokenHandlingIsCentralizedInAuthHelper() throws IOException {
        Map<Path, String> sources = readStaticFiles(".js");

        List<String> sessionStorageUsers = sources.entrySet().stream()
                .filter(entry -> SESSION_STORAGE.matcher(entry.getValue()).find())
                .map(entry -> relativeStaticPath(entry.getKey()).replace('\\', '/'))
                .toList();
        String authJs = Files.readString(staticRoot().resolve("js/auth.js"), StandardCharsets.UTF_8);

        assertThat(sessionStorageUsers)
                .as("sessionStorage is allowed only in the centralized browser-session auth helper")
                .containsExactly("js/auth.js");
        assertThat(authJs)
                .contains("SESSION_KEY")
                .contains("setSession")
                .contains("getAuthHeader")
                .contains("\"Authorization\"")
                .contains("clearSession")
                .doesNotContain("password")
                .doesNotContain("mfaCode");
    }

    @Test
    void trackingCodesAreNotPlacedInBrowserUrls() throws IOException {
        Map<Path, String> sources = readStaticFiles(".js", ".html");

        List<String> offenders = sources.entrySet().stream()
                .filter(entry -> TRACKING_CODE_QUERY.matcher(entry.getValue()).find())
                .map(entry -> relativeStaticPath(entry.getKey()))
                .toList();

        assertThat(offenders)
                .as("Tracking codes are report access secrets and must not be placed in query strings, redirects or URL parsers")
                .isEmpty();
    }

    @Test
    void xssPayloadsAreRenderedThroughTextNodeApisOnly() throws IOException {
        String payload = "<img src=x onerror=alert(1)>\"'&";
        assertThat(payload).contains("<").contains("\"").contains("'").contains("&");

        Map<Path, String> sources = readStaticFiles(".js");
        String allSources = String.join("\n", sources.values());
        String domHelper = sources.entrySet().stream()
                .filter(entry -> entry.getKey().getFileName().toString().equals("dom.js"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("");

        assertThat(domHelper)
                .contains("document.createTextNode")
                .contains("textContent");
        assertThat(allSources)
                .doesNotContain("escapeHtml(")
                .doesNotContain("replaceAll(\"<\"")
                .doesNotContain("&#039;");
    }

    @Test
    void frontendDoesNotUseInlineScriptsOrEventHandlers() throws IOException {
        Map<Path, String> sources = readStaticFiles(".html");

        List<String> offenders = sources.entrySet().stream()
                .filter(entry -> INLINE_SCRIPT_OR_HANDLER.matcher(entry.getValue()).find())
                .map(entry -> relativeStaticPath(entry.getKey()))
                .toList();

        assertThat(offenders)
                .as("CSP without unsafe-inline requires external scripts and addEventListener/data-action handlers")
                .isEmpty();
    }

    @Test
    void pagesLoadBrowserSecuritySupportCheck() throws IOException {
        Map<Path, String> sources = readStaticFiles(".html");

        assertThat(sources)
                .as("All static HTML pages should load the browser security feature fallback")
                .allSatisfy((path, html) -> assertThat(html)
                        .as(relativeStaticPath(path))
                        .contains("<script src=\"/js/security-support.js\"></script>"));

        String supportScript = Files.readString(staticRoot().resolve("js/security-support.js"), StandardCharsets.UTF_8);
        assertThat(supportScript)
                .contains("crypto.getRandomValues")
                .contains("fetch")
                .contains("disableInteractiveControls")
                .doesNotContain("innerHTML");
    }

    @Test
    void frontendAvoidsDomClobberingPatterns() throws IOException {
        Map<Path, String> htmlSources = readStaticFiles(".html");
        Map<Path, String> jsSources = readStaticFiles(".js");

        List<String> namedControls = htmlSources.entrySet().stream()
                .filter(entry -> FORM_CONTROL_NAME.matcher(entry.getValue()).find())
                .map(entry -> relativeStaticPath(entry.getKey()))
                .toList();
        List<String> ids = htmlSources.values().stream()
                .flatMap(html -> HTML_ID.matcher(html).results().map(match -> match.group(1)))
                .filter(id -> !"title".equals(id))
                .distinct()
                .toList();
        Pattern documentIdAccess = Pattern.compile("\\bdocument\\.(" +
                ids.stream().map(Pattern::quote).collect(Collectors.joining("|")) + ")\\b");
        List<String> documentIdAccessOffenders = jsSources.entrySet().stream()
                .filter(entry -> documentIdAccess.matcher(entry.getValue()).find())
                .map(entry -> relativeStaticPath(entry.getKey()))
                .toList();

        assertThat(namedControls)
                .as("Form controls should avoid name attributes that can clobber document/window properties")
                .isEmpty();
        assertThat(documentIdAccessOffenders)
                .as("Frontend should use explicit DOM lookup APIs instead of document.<id/name> property access")
                .isEmpty();
    }

    @Test
    void internalPagesLoadCentralAuthHelperBeforeRoleScripts() throws IOException {
        Map<Path, String> htmlSources = readStaticFiles(".html");

        assertThat(htmlSources.get(staticRoot().resolve("admin.html")))
                .containsSubsequence(
                        "<script src=\"/js/api.js\"></script>",
                        "<script src=\"/js/auth.js\"></script>",
                        "<script src=\"/js/admin.js\"></script>"
                );
        assertThat(htmlSources.get(staticRoot().resolve("analyst.html")))
                .containsSubsequence(
                        "<script src=\"/js/api.js\"></script>",
                        "<script src=\"/js/auth.js\"></script>",
                        "<script src=\"/js/analyst.js\"></script>"
                );
        assertThat(htmlSources.get(staticRoot().resolve("auditor.html")))
                .containsSubsequence(
                        "<script src=\"/js/api.js\"></script>",
                        "<script src=\"/js/auth.js\"></script>",
                        "<script src=\"/js/auditor.js\"></script>"
                );
    }

    @Test
    void publicReportFormUsesBackendAcceptedCategoryValues() throws IOException {
        String submitHtml = Files.readString(staticRoot().resolve("submit.html"), StandardCharsets.UTF_8);

        List<String> optionValues = REPORT_CATEGORY_OPTION.matcher(submitHtml)
                .results()
                .map(match -> match.group(1))
                .filter(value -> !value.isBlank())
                .toList();

        assertThat(optionValues)
                .as("Category option values sent by submit.js must match CreateReportRequest allowlist")
                .containsExactly("Fraud", "Corruption", "Harassment", "Security", "Other")
                .allSatisfy(value -> assertThat(value).matches(REPORT_CATEGORY_ALLOWLIST));
    }

    @Test
    void publicTrackingPageDoesNotFetchAttachmentMetadata() throws IOException {
        String trackJs = Files.readString(staticRoot().resolve("js/track.js"), StandardCharsets.UTF_8);

        assertThat(trackJs)
                .doesNotContain("attachments/list")
                .doesNotContain("originalName")
                .doesNotContain("mimeType")
                .doesNotContain("attachment.id");
    }

    private static Map<Path, String> readStaticFiles(String... extensions) throws IOException {
        Path staticRoot = staticRoot();
        List<String> suffixes = List.of(extensions);

        try (Stream<Path> paths = Files.walk(staticRoot)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> suffixes.stream().anyMatch(suffix -> path.getFileName().toString().endsWith(suffix)))
                    .collect(Collectors.toMap(path -> path, FrontendXssDataExposureTest::readString));
        }
    }

    private static Path staticRoot() {
        Path moduleRoot = Path.of("src/main/resources/static");
        if (Files.exists(moduleRoot)) {
            return moduleRoot;
        }
        return Path.of("ghostreport/src/main/resources/static");
    }

    private static String readString(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read " + path, ex);
        }
    }

    private static String relativeStaticPath(Path path) {
        return staticRoot().relativize(path).toString();
    }
}
