package com.ghostreport.security;

import com.ghostreport.repository.SecurityAlertRepository;
import com.ghostreport.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class TrackingCodeEnumerationTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private SecurityAlertRepository securityAlertRepository;

    @BeforeEach
    void setup() {
        securityAlertRepository.deleteAll();
    }

    @Test
    void invalidTrackingCodeShouldFail() {

        assertThrows(
                ResponseStatusException.class,
                () -> reportService.verifyTrackingCodeOnly("INVALID-CODE")
        );
    }

    @Test
    void repeatedInvalidTrackingCodesCreateSecurityAlert() {
        String rawTrackingCode = "GR-aaaaaaaaaaaaaaaaaaaa";

        for (int i = 0; i < 5; i++) {
            assertThrows(
                    ResponseStatusException.class,
                    () -> reportService.verifyTrackingCodeOnly(rawTrackingCode)
            );
        }

        assertTrue(
                securityAlertRepository.findAll()
                        .stream()
                        .anyMatch(alert -> "TRACKING_CODE_ENUMERATION".equals(alert.getAlertType()))
        );
        assertFalse(
                securityAlertRepository.findAll()
                        .stream()
                        .anyMatch(alert -> alert.getDescription() != null && alert.getDescription().contains(rawTrackingCode))
        );
    }
}
