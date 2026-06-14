package com.ghostreport.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReportDescriptionTest {

    @Test
    void validDescriptionWorks() {

        ReportDescription desc =
                new ReportDescription(
                        "  This is a valid description  "
                );

        assertNotNull(desc);
        assertEquals(
                "This is a valid description",
                desc.value()
        );
        assertEquals(
                "This is a valid description",
                desc.toString()
        );
    }

    @Test
    void shortDescriptionThrows() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new ReportDescription("abc")
        );
    }

    @Test
    void minimumLengthDescriptionIsAccepted() {

        ReportDescription desc =
                new ReportDescription("1234567890");

        assertEquals("1234567890", desc.value());
    }

    @Test
    void descriptionJustBelowMinimumLengthThrows() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new ReportDescription("123456789")
        );
    }

    @Test
    void maximumLengthDescriptionIsAccepted() {

        String value = "a".repeat(3000);

        ReportDescription desc =
                new ReportDescription(value);

        assertEquals(value, desc.value());
    }

    @Test
    void descriptionAboveMaximumLengthThrows() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new ReportDescription("a".repeat(3001))
        );
    }
}
