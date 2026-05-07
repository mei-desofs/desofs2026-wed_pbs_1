package com.ghostreport.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReportDescriptionTest {

    @Test
    void validDescriptionWorks() {

        ReportDescription desc =
                new ReportDescription(
                        "This is a valid description"
                );

        assertNotNull(desc);
    }

    @Test
    void shortDescriptionThrows() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new ReportDescription("abc")
        );
    }
}