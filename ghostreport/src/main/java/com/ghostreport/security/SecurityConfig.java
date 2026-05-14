package com.ghostreport.security;

import com.ghostreport.service.SecurityMonitoringService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityMonitoringService securityMonitoringService
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/submit.html", "/track.html", "/analyst.html", "/admin.html", "/auditor.html").permitAll()
                        .requestMatchers("/css/**", "/js/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/reports").permitAll()
                        .requestMatchers(HttpMethod.POST, "/reports/verify").permitAll()
                        .requestMatchers(HttpMethod.POST, "/reports/{id}/attachments").permitAll()

                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/analyst/**").hasAnyRole("ANALYST", "ADMIN")
                        .requestMatchers("/audit/**").hasAnyRole("AUDITOR", "ADMIN")

                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getRequestURI().startsWith("/admin/backups")) {
                                securityMonitoringService.recordUnauthorizedBackupAccess(request.getRequestURI());
                            }
                            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"GhostReport\"");
                            response.sendError(401, "Unauthorized");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (request.getRequestURI().startsWith("/admin/backups")) {
                                securityMonitoringService.recordUnauthorizedBackupAccess(request.getRequestURI());
                            }
                            response.sendError(403, "Access denied");
                        })
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
