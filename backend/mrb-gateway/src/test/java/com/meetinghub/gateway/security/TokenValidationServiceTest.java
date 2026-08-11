package com.meetinghub.gateway.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenValidationServiceTest {

    private static final String TOKEN = "valid-token";
    private static final String USER_TOKEN_KEY = "mrb:user:token:100";

    @Mock
    private GatewayJwtVerifier verifier;

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenValidationService service;

    @Test
    void should_returnEmpty_when_signatureInvalid() {
        when(verifier.verify(TOKEN)).thenReturn(Optional.empty());

        Optional<TokenClaims> result = service.validate(TOKEN).block();

        assertThat(result).isEmpty();
    }

    @Test
    void should_returnClaims_when_redisTokenMatches() {
        TokenClaims claims = new TokenClaims(100L, "alice", "ROLE_USER");
        when(verifier.verify(TOKEN)).thenReturn(Optional.of(claims));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(USER_TOKEN_KEY)).thenReturn(Mono.just(TOKEN));

        Optional<TokenClaims> result = service.validate(TOKEN).block();

        assertThat(result).isPresent();
        assertThat(result.get().userId()).isEqualTo(100L);
        assertThat(result.get().username()).isEqualTo("alice");
        assertThat(result.get().role()).isEqualTo("ROLE_USER");
    }

    @Test
    void should_returnEmpty_when_redisTokenDiffers() {
        TokenClaims claims = new TokenClaims(100L, "alice", "ROLE_USER");
        when(verifier.verify(TOKEN)).thenReturn(Optional.of(claims));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(USER_TOKEN_KEY)).thenReturn(Mono.just("another-token"));

        Optional<TokenClaims> result = service.validate(TOKEN).block();

        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_redisTokenMissing() {
        TokenClaims claims = new TokenClaims(100L, "alice", "ROLE_USER");
        when(verifier.verify(TOKEN)).thenReturn(Optional.of(claims));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(USER_TOKEN_KEY)).thenReturn(Mono.empty());

        Optional<TokenClaims> result = service.validate(TOKEN).block();

        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_redisFails() {
        TokenClaims claims = new TokenClaims(100L, "alice", "ROLE_USER");
        when(verifier.verify(TOKEN)).thenReturn(Optional.of(claims));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(USER_TOKEN_KEY))
                .thenReturn(Mono.error(new RuntimeException("redis unavailable")));

        Optional<TokenClaims> result = service.validate(TOKEN).block();

        assertThat(result).isEmpty();
    }
}
