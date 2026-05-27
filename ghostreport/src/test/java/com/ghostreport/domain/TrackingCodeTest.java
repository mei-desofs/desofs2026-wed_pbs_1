package com.ghostreport.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrackingCodeTest {

    @Test
    void generatedCodeIsValid() {

        TrackingCode code =
                TrackingCode.generate();

        assertNotNull(code.value());

        assertTrue(
                code.value().startsWith("GR-")
        );
    }

    @Test
    void invalidCodeThrows() {

        assertThrows(
                IllegalArgumentException.class,
                () -> TrackingCode.from("123")
        );
    }
}