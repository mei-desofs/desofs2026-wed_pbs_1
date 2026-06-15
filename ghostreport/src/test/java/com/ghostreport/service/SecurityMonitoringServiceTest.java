package com.ghostreport.service;

import com.ghostreport.model.SecurityAlert;
import com.ghostreport.repository.SecurityAlertRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SecurityMonitoringServiceTest {

    private final SecurityAlertRepository securityAlertRepository = mock(SecurityAlertRepository.class);
    private final SecurityMonitoringService securityMonitoringService =
            new SecurityMonitoringService(securityAlertRepository, new SecurityLogSanitizer());

    @Test
    void cspViolationReportsAreStoredAsSanitizedSecurityAlerts() {
        securityMonitoringService.recordCspViolation("""
                {
                  "csp-report": {
                    "violated-directive": "script-src",
                    "blocked-uri": "inline",
                    "trackingCode": "GR-abcdefghijklmnopqrstuvwxyz",
                    "sample": "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.signature"
                  }
                }
                """);

        ArgumentCaptor<SecurityAlert> captor = ArgumentCaptor.forClass(SecurityAlert.class);
        verify(securityAlertRepository).save(captor.capture());

        SecurityAlert alert = captor.getValue();
        assertThat(alert.getAlertType()).isEqualTo("CSP_VIOLATION");
        assertThat(alert.getSeverity()).isEqualTo("MEDIUM");
        assertThat(alert.getTargetType()).isEqualTo("BROWSER");
        assertThat(alert.getDescription()).contains("script-src");
        assertThat(alert.getDescription()).contains("[REDACTED]");
        assertThat(alert.getDescription()).doesNotContain("GR-abcdefghijklmnopqrstuvwxyz");
        assertThat(alert.getDescription()).doesNotContain("eyJhbGciOiJIUzI1NiJ9");
        assertThat(alert.getIntegrityHash()).isNotBlank();
    }
}
