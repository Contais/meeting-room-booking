package com.meetinghub.gateway.filter;

import com.meetinghub.gateway.security.TokenClaims;
import com.meetinghub.gateway.security.TokenValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthGlobalFilterTest {

    private static final TokenClaims CLAIMS = new TokenClaims(100L, "alice", "ROLE_USER");

    @Mock
    private TokenValidationService tokenValidationService;

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    private AuthGlobalFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AuthGlobalFilter(tokenValidationService);
        lenient().when(exchange.getRequest()).thenReturn(request);
        lenient().when(exchange.getResponse()).thenReturn(response);
        lenient().when(chain.filter(exchange)).thenReturn(Mono.empty());
    }

    @Test
    void should_passThrough_when_pathInWhitelist() {
        when(request.getURI()).thenReturn(URI.create("/api/auth/login"));

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(tokenValidationService, never()).validate(anyString());
    }

    @Test
    void should_reject_when_tokenMissing() {
        mockRequest("/api/meeting/room/list", new HttpHeaders());
        mockUnauthorizedResponse();

        filter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void should_reject_when_tokenInvalid() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer forged.any.payload");
        mockRequest("/api/meeting/room/list", headers);
        when(tokenValidationService.validate("forged.any.payload"))
                .thenReturn(Mono.just(Optional.empty()));
        mockUnauthorizedResponse();

        filter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void should_injectVerifiedClaims_when_tokenValid() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer valid-token");
        headers.set("X-User-Role", "ROLE_ADMIN");
        mockRequest("/api/meeting/room/list", headers);
        when(tokenValidationService.validate("valid-token"))
                .thenReturn(Mono.just(Optional.of(CLAIMS)));

        ServerHttpRequest mutatedRequest = mockRequestMutation();

        filter.filter(exchange, chain).block();

        HttpHeaders forwarded = mutatedRequest.getHeaders();
        assertThat(forwarded.getFirst("Authorization")).isEqualTo("Bearer valid-token");
        assertThat(forwarded.getFirst("X-User-Id")).isEqualTo("100");
        assertThat(forwarded.getFirst("X-User-Role")).isEqualTo("ROLE_USER");
        assertThat(forwarded.getFirst("X-User-Username")).isEqualTo("alice");
        verify(chain).filter(exchange);
    }

    @Test
    void should_validateToken_when_webSocketQueryParam() {
        mockRequest("/ws/notify?token=ws-token", new HttpHeaders());
        when(tokenValidationService.validate("ws-token"))
                .thenReturn(Mono.just(Optional.of(CLAIMS)));

        ServerHttpRequest mutatedRequest = mockRequestMutation();

        filter.filter(exchange, chain).block();

        assertThat(mutatedRequest.getHeaders().getFirst("Authorization")).isEqualTo("Bearer ws-token");
        assertThat(mutatedRequest.getHeaders().getFirst("X-User-Id")).isEqualTo("100");
        verify(chain).filter(exchange);
    }

    @Test
    void should_reject_when_webSocketQueryTokenInvalid() {
        mockRequest("/ws/notify?token=forged", new HttpHeaders());
        when(tokenValidationService.validate("forged"))
                .thenReturn(Mono.just(Optional.empty()));
        mockUnauthorizedResponse();

        filter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    private void mockRequest(String path, HttpHeaders headers) {
        lenient().when(request.getURI()).thenReturn(URI.create(path));
        lenient().when(request.getHeaders()).thenReturn(headers);
    }

    private void mockUnauthorizedResponse() {
        lenient().when(response.getHeaders()).thenReturn(new HttpHeaders());
        lenient().when(response.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        lenient().when(response.writeWith(any())).thenReturn(Mono.empty());
    }

    private ServerHttpRequest mockRequestMutation() {
        HttpHeaders mutatedHeaders = new HttpHeaders();
        ServerHttpRequest.Builder builder = mock(ServerHttpRequest.Builder.class);
        lenient().when(request.mutate()).thenReturn(builder);
        lenient().when(builder.headers(any())).thenAnswer(invocation -> {
            Consumer<HttpHeaders> consumer = invocation.getArgument(0);
            consumer.accept(mutatedHeaders);
            return builder;
        });
        lenient().when(builder.header(anyString(), anyString())).thenAnswer(invocation -> {
            mutatedHeaders.set(invocation.getArgument(0), invocation.getArgument(1));
            return builder;
        });

        ServerHttpRequest mutatedRequest = mock(ServerHttpRequest.class);
        lenient().when(mutatedRequest.getHeaders()).thenReturn(mutatedHeaders);
        lenient().when(builder.build()).thenReturn(mutatedRequest);

        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        lenient().when(exchange.mutate()).thenReturn(exchangeBuilder);
        lenient().when(exchangeBuilder.request(any(ServerHttpRequest.class))).thenReturn(exchangeBuilder);
        lenient().when(exchangeBuilder.build()).thenReturn(exchange);
        return mutatedRequest;
    }
}
