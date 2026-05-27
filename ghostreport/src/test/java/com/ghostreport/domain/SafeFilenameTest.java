package com.ghostreport.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SafeFilenameTest {

    @Test
    void validFilenameWorks() {

        SafeFilename file =
                new SafeFilename("report.pdf");

        assertNotNull(file);
    }

    @Test
    void pathTraversalRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new SafeFilename(
                        "../../passwd"
                )
        );
    }

    @Test
    void exeRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new SafeFilename(
                        "virus.exe"
                )
        );
    }
}