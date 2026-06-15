package com.ghostreport.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostreport.service.SecurityMonitoringService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Configuration
public class SecurityConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String[] PUBLIC_PAGES = {
            "/",
            "/index.html",
            "/submit.html",
            "/track.html",
            "/analyst.html",
            "/admin.html",
            "/auditor.html"
    };
    private static final String CSP_REPORTING_GROUP = "csp-endpoint";
    private static final String CSP_REPORT_ENDPOINT = "/security/csp-report";
    private static final String CONTENT_SECURITY_POLICY = "default-src 'self'; "
            + "script-src 'self'; "
            + "style-src 'self'; "
            + "object-src 'none'; "
            + "frame-ancestors 'none'; "
            + "base-uri 'none'; "
            + "form-action 'self'; "
            + "upgrade-insecure-requests; "
            + "report-to " + CSP_REPORTING_GROUP;
    private static final String REPORT_TO_HEADER = "{\"group\":\"" + CSP_REPORTING_GROUP
            + "\",\"max_age\":10886400,\"endpoints\":[{\"url\":\"" + CSP_REPORT_ENDPOINT + "\"}]}";

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityMonitoringService securityMonitoringService,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorrelationIdFilter correlationIdFilter
    ) throws Exception {
        configureCsrf(http);
        configureHeaders(http);
        configureAuthorization(http);
        configureExceptionHandling(http, securityMonitoringService);
        configureFilters(http, jwtAuthenticationFilter, correlationIdFilter);
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }

    private void configureCsrf(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers("/auth/login", CSP_REPORT_ENDPOINT)
        );
    }

    private void configureHeaders(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
                .contentTypeOptions(contentTypeOptions -> {
                })
                .frameOptions(frameOptions -> frameOptions.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .preload(true)
                        .maxAgeInSeconds(31_536_000)
                )
                .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                .referrerPolicy(referrer -> referrer
                        .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)
                )
                .addHeaderWriter(new StaticHeadersWriter("Report-To", REPORT_TO_HEADER))
                .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "geolocation=(), microphone=(), camera=(), payment=()"))
                .addHeaderWriter(new StaticHeadersWriter("Cross-Origin-Opener-Policy", "same-origin"))
                .addHeaderWriter(new StaticHeadersWriter("Cross-Origin-Resource-Policy", "same-origin"))
                .addHeaderWriter(new StaticHeadersWriter("Cross-Origin-Embedder-Policy", "require-corp"))
        );
    }

    private void configureAuthorization(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PAGES).permitAll()
                .requestMatchers("/css/**", "/js/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/mfa/verify").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/password-reset/request").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/password-reset/confirm").permitAll()
                .requestMatchers(HttpMethod.POST, CSP_REPORT_ENDPOINT).permitAll()
                .requestMatchers(HttpMethod.POST, "/reports").permitAll()
                .requestMatchers(HttpMethod.POST, "/reports/verify").permitAll()
                .requestMatchers(HttpMethod.POST, "/reports/{id}/attachments").permitAll()
                .requestMatchers(HttpMethod.POST, "/reports/{id}/attachments/list").permitAll()
                .requestMatchers(HttpMethod.POST, "/reports/download").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/analyst/**").hasAnyRole("ANALYST", "ADMIN")
                .requestMatchers("/audit/**").hasAnyRole("AUDITOR", "ADMIN")
                .anyRequest().authenticated()
        );
    }

    private void configureExceptionHandling(
            HttpSecurity http,
            SecurityMonitoringService securityMonitoringService
    ) throws Exception {
        http.exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    recordBackupAccessAttempt(request, securityMonitoringService);
                    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"GhostReport\"");
                    writeSecurityError(response, 401, "Unauthorized");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    securityMonitoringService.recordForbiddenAccess(request.getRequestURI());
                    recordBackupAccessAttempt(request, securityMonitoringService);
                    writeSecurityError(response, 403, "Access denied");
                })
        );
    }

    private void configureFilters(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorrelationIdFilter correlationIdFilter
    ) {
        http.addFilterBefore(new HttpRequestBoundaryFilter(), CsrfFilter.class)
                .addFilterBefore(new FetchMetadataFilter(), CsrfFilter.class)
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .addFilterAfter(new SensitiveResponseCacheControlFilter(), CsrfCookieFilter.class)
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, CorrelationIdFilter.class);
    }

    private void recordBackupAccessAttempt(
            HttpServletRequest request,
            SecurityMonitoringService securityMonitoringService
    ) {
        if (request.getRequestURI().startsWith("/admin/backups")) {
            securityMonitoringService.recordUnauthorizedBackupAccess(request.getRequestURI());
        }
    }

    @SuppressWarnings("java:S3330")
    private CookieCsrfTokenRepository csrfTokenRepository() {
        // The frontend reads XSRF-TOKEN and returns it in X-XSRF-TOKEN.
        // The token is not an authentication secret; JWTs remain in Authorization headers.
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.sameSite("Lax"));
        return repository;
    }

    private static final class CsrfCookieFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken == null) {
                csrfToken = (CsrfToken) request.getAttribute("_csrf");
            }
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }

    private static final class SensitiveResponseCacheControlFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            filterChain.doFilter(request, response);
            if (isSensitivePath(request.getRequestURI())) {
                response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, max-age=0, must-revalidate");
                response.setHeader(HttpHeaders.PRAGMA, "no-cache");
                response.setDateHeader(HttpHeaders.EXPIRES, 0);
            }
        }

        private boolean isSensitivePath(String path) {
            return path.startsWith("/auth/")
                    || path.startsWith("/reports")
                    || path.startsWith("/security/")
                    || path.startsWith("/admin/")
                    || path.startsWith("/analyst/")
                    || path.startsWith("/audit/");
        }
    }

    private static final class FetchMetadataFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            if (isUnsafeMethod(request.getMethod()) && isCrossSite(request)) {
                writeFilterError(response, 403, "Cross-site request rejected");
                return;
            }
            filterChain.doFilter(request, response);
        }

        private boolean isCrossSite(HttpServletRequest request) {
            String secFetchSite = request.getHeader("Sec-Fetch-Site");
            if ("cross-site".equalsIgnoreCase(secFetchSite)) {
                return true;
            }

            String origin = request.getHeader(HttpHeaders.ORIGIN);
            if (origin == null || origin.isBlank()) {
                return false;
            }
            return !isSameOrigin(request, origin);
        }

        private boolean isSameOrigin(HttpServletRequest request, String origin) {
            String expected = request.getScheme() + "://" + request.getServerName();
            int port = request.getServerPort();
            if (!isDefaultPort(request.getScheme(), port)) {
                expected += ":" + port;
            }
            return expected.equalsIgnoreCase(origin);
        }

        private boolean isDefaultPort(String scheme, int port) {
            return ("http".equalsIgnoreCase(scheme) && port == 80)
                    || ("https".equalsIgnoreCase(scheme) && port == 443);
        }
    }

    private static final class HttpRequestBoundaryFilter extends OncePerRequestFilter {

        private static final int MAX_AUTHORIZATION_HEADER_LENGTH = 8192;
        private static final int MAX_HEADER_VALUE_LENGTH = 8192;
        private static final Set<String> CONNECTION_SPECIFIC_HEADERS = Set.of(
                "connection",
                "keep-alive",
                "proxy-connection",
                "transfer-encoding",
                "upgrade"
        );

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            if ("TRACE".equalsIgnoreCase(request.getMethod())) {
                writeFilterError(response, 405, "Method not allowed");
                return;
            }

            if (requestsSourceControlMetadata(request)) {
                writeFilterError(response, 404, "Not found");
                return;
            }

            if (hasUnsafeHeader(request)) {
                writeFilterError(response, 400, "Invalid request headers");
                return;
            }

            if (hasDuplicatedScalarParameter(request)) {
                writeFilterError(response, 400, "Ambiguous request parameters");
                return;
            }

            filterChain.doFilter(request, response);
        }

        private boolean requestsSourceControlMetadata(HttpServletRequest request) {
            String uri = request.getRequestURI();
            return uri != null
                    && (uri.equals("/.git")
                    || uri.startsWith("/.git/")
                    || uri.equals("/.svn")
                    || uri.startsWith("/.svn/"));
        }

        private boolean hasUnsafeHeader(HttpServletRequest request) {
            Enumeration<String> names = request.getHeaderNames();
            while (names != null && names.hasMoreElements()) {
                String name = names.nextElement();
                if (containsControlCharacters(name)) {
                    return true;
                }
                if (isHttp2OrHttp3(request) && CONNECTION_SPECIFIC_HEADERS.contains(name.toLowerCase())) {
                    return true;
                }
                Enumeration<String> values = request.getHeaders(name);
                while (values != null && values.hasMoreElements()) {
                    String value = values.nextElement();
                    if (containsControlCharacters(value) || value.length() > MAX_HEADER_VALUE_LENGTH) {
                        return true;
                    }
                    if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)
                            && value.length() > MAX_AUTHORIZATION_HEADER_LENGTH) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean isHttp2OrHttp3(HttpServletRequest request) {
            String protocol = request.getProtocol();
            return protocol != null
                    && (protocol.startsWith("HTTP/2") || protocol.startsWith("HTTP/3"));
        }

        private boolean hasDuplicatedScalarParameter(HttpServletRequest request) {
            String contentType = request.getContentType();
            if (contentType != null && contentType.toLowerCase().startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)) {
                return false;
            }

            return request.getParameterMap()
                    .values()
                    .stream()
                    .anyMatch(values -> values != null && values.length > 1);
        }

        private boolean containsControlCharacters(String value) {
            if (value == null) {
                return false;
            }
            return value.chars().anyMatch(ch -> (ch < 32 && ch != '\t') || ch == 127);
        }
    }

    private static boolean isUnsafeMethod(String method) {
        return !("GET".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method)
                || "TRACE".equalsIgnoreCase(method));
    }

    private void writeSecurityError(
            jakarta.servlet.http.HttpServletResponse response,
            int status,
            String error
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("correlationId", CorrelationId.current());
        OBJECT_MAPPER.writeValue(response.getWriter(), body);
    }

    private static void writeFilterError(HttpServletResponse response, int status, String error) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("correlationId", CorrelationId.current());
        OBJECT_MAPPER.writeValue(response.getWriter(), body);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
