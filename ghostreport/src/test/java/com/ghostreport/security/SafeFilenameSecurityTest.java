package com.ghostreport.security;

import com.ghostreport.domain.SafeFilename;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SafeFilenameSecurityTest {

    @Test
    void shouldRejectPathTraversalFilename() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new SafeFilename("../../windows/system32")
        );
    }

    @Test
    void shouldRejectBackslashTraversal() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new SafeFilename("..\\..\\secret.txt")
        );
    }
}