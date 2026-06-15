package com.ghostreport.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

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
    void generatedCodesAreNotConstant() {

        Set<String> generatedCodes = new HashSet<>();

        for (int i = 0; i < 8; i++) {
            generatedCodes.add(
                    TrackingCode.generate().value()
            );
        }

        assertTrue(
                generatedCodes.size() > 1
        );
    }

    @Test
    void validCodeCanBeRestored() {

        String value = "GR-abcdefghijklmnopqrst";

        TrackingCode code =
                TrackingCode.from(value);

        assertNotNull(code);
        assertEquals(value, code.value());
    }

    @Test
    void invalidCodeThrows() {

        assertThrows(
                IllegalArgumentException.class,
                () -> TrackingCode.from("123")
        );
    }
}
