package com.ghostreport.security;

import com.ghostreport.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class TrackingCodeEnumerationTest {

    @Autowired
    private ReportService reportService;

    @Test
    void invalidTrackingCodeShouldFail() {

        assertThrows(
                ResponseStatusException.class,
                () -> reportService.verifyTrackingCodeOnly("INVALID-CODE")
        );
    }
}