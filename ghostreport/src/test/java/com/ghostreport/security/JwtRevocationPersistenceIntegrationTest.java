package com.ghostreport.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostreport.repository.RevokedTokenRepository;
import com.ghostreport.service.JwtService;
import com.ghostreport.service.PersistentRevokedTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jwt-revocation-persistence-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "ghostreport.backup-dir=target/test-backups/jwt-revocation-persistence",
        "app.upload-dir=target/test-uploads/jwt-revocation-persistence",
        "ghostreport.backup-enabled=true",
        "ghostreport.jwt.secret=test-secret-with-more-than-32-characters",
        "ghostreport.jwt.active-key-id=current-key",
        "ghostreport.jwt.expiration-seconds=3600"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class JwtRevocationPersistenceIntegrationTest {

    private static final String SECRET = "test-secret-with-more-than-32-characters";
    private static final String KEY_ID = "current-key";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    @BeforeEach
    void setUp() {
        revokedTokenRepository.deleteAll();
    }

    @Test
    void revokedTokenRemainsRejectedAfterServiceInstanceReplacement() throws Exception {
        UserDetails user = User.withUsername("analyst")
                .password("unused")
                .roles("ANALYST")
                .build();
        PersistentRevokedTokenStore firstStore = new PersistentRevokedTokenStore(revokedTokenRepository);
        JwtService firstService = new JwtService(objectMapper, firstStore, SECRET, KEY_ID, "", 3600);
        String token = firstService.generateToken(user);
        String tokenId = tokenId(token);

        firstService.revokeToken(token);

        assertThat(revokedTokenRepository.findByTokenId(tokenId))
                .isPresent()
                .get()
                .satisfies(revoked -> {
                    assertThat(revoked.getKeyId()).isEqualTo(KEY_ID);
                    assertThat(revoked.getSubject()).isEqualTo("analyst");
                    assertThat(revoked.getExpiresAt()).isAfter(Instant.now());
                });

        PersistentRevokedTokenStore replacementStore = new PersistentRevokedTokenStore(revokedTokenRepository);
        JwtService replacementService = new JwtService(objectMapper, replacementStore, SECRET, KEY_ID, "", 3600);

        assertThat(replacementService.isTokenValid(token, user)).isFalse();
    }

    private String tokenId(String token) throws Exception {
        String[] parts = token.split("\\.", -1);
        byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
        Map<String, Object> claims = objectMapper.readValue(new String(payload, StandardCharsets.UTF_8), Map.class);
        return (String) claims.get("jti");
    }
}
