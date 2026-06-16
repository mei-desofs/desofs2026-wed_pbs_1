package com.ghostreport.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseDataMinimizationTest {

    private static final Path DTO_ROOT = Path.of("src/main/java/com/ghostreport/dto");

    @Test
    void responseDtosDoNotExposeInternalFilesystemPaths() throws IOException {
        List<String> responseSources;
        try (var paths = Files.walk(DTO_ROOT)) {
            responseSources = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("Response.java"))
                    .map(ResponseDataMinimizationTest::read)
                    .toList();
        }

        assertThat(String.join("\n", responseSources))
                .doesNotContain("packagePath")
                .doesNotContain("restorePath")
                .doesNotContain("storagePath")
                .doesNotContain("generatedFiles");
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path, e);
        }
    }
}
