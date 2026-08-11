package com.meetinghub.gateway.security;

import com.meetinghub.gateway.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayJwtVerifierTest {

    private static final String SECRET = "mrb-meeting-room-booking-jwt-secret-key-2024";

    private GatewayJwtVerifier verifier;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        verifier = new GatewayJwtVerifier(properties);
    }

    @Test
    void should_returnClaims_when_tokenValid() {
        String token = generateToken(100L, "alice", "ROLE_USER");

        Optional<TokenClaims> result = verifier.verify(token);

        assertThat(result).isPresent();
        assertThat(result.get().userId()).isEqualTo(100L);
        assertThat(result.get().username()).isEqualTo("alice");
        assertThat(result.get().role()).isEqualTo("ROLE_USER");
    }

    @Test
    void should_returnEmpty_when_tokenTampered() {
        String token = generateToken(100L, "alice", "ROLE_USER");
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        Optional<TokenClaims> result = verifier.verify(tampered);

        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_tokenExpired() {
        String token = Jwts.builder()
                .subject("100")
                .claim("username", "alice")
                .claim("role", "ROLE_USER")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        Optional<TokenClaims> result = verifier.verify(token);

        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_signedByWrongSecret() {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "another-secret-key-0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("100")
                .claim("username", "alice")
                .claim("role", "ROLE_ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(otherKey)
                .compact();

        Optional<TokenClaims> result = verifier.verify(token);

        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_malformedToken() {
        assertThat(verifier.verify("not-a-jwt")).isEmpty();
        assertThat(verifier.verify("a.b")).isEmpty();
        assertThat(verifier.verify("")).isEmpty();
        assertThat(verifier.verify(null)).isEmpty();
    }

    private String generateToken(Long userId, String username, String role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
