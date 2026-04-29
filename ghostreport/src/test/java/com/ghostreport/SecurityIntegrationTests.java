package com.ghostreport;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityIntegrationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void adminEndpointWithoutAuthShouldReturnUnauthorized() {
        ResponseEntity<String> response =
                rest.getForEntity(url("/admin/users"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void analystShouldNotAccessAdminEndpoint() {
        ResponseEntity<String> response =
                rest.withBasicAuth("analyst", "Analyst123!")
                        .getForEntity(url("/admin/users"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void trackingCodeMissingShouldReturnBadRequestOrForbidden() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>("{}", headers);

        ResponseEntity<String> response =
                rest.postForEntity(url("/reports/verify"), request, String.class);

        assertThat(response.getStatusCode())
                .isIn(HttpStatus.BAD_REQUEST, HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);
    }

    @Test
    void invalidTrackingCodeShouldNotExposeSensitiveData() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {
                  "trackingCode": "wrong-code"
                }
                """;

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                rest.postForEntity(url("/reports/1/verify"), request, String.class);

        assertThat(response.getBody()).doesNotContain("trackingCodeHash");
        assertThat(response.getBody()).doesNotContain("passwordHash");
    }
}