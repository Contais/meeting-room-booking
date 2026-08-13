package com.meetinghub.gateway.filter;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlockInternalPathFilterTest {

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    private BlockInternalPathFilter filter;

    @BeforeEach
    void setUp() {
        filter = new BlockInternalPathFilter();
        lenient().when(exchange.getRequest()).thenReturn(request);
        lenient().when(exchange.getResponse()).thenReturn(response);
        lenient().when(chain.filter(exchange)).thenReturn(Mono.empty());
    }

    @Test
    void should_blockPlatformNotificationInternalPath() {
        mockRequest("/api/platform/internal/notification/send");
        mockNotFoundResponse();

        filter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.NOT_FOUND);
        verify(chain, never()).filter(any());
    }

    @Test
    void should_blockPlatformFileInternalPath() {
        mockRequest("/api/platform/internal/file/presigned-urls");
        mockNotFoundResponse();

        filter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.NOT_FOUND);
        verify(chain, never()).filter(any());
    }

    @Test
    void should_blockUserInternalPath() {
        mockRequest("/api/uc/user/internal/batch?ids=1,2");
        mockNotFoundResponse();

        filter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.NOT_FOUND);
        verify(chain, never()).filter(any());
    }

    @Test
    void should_passThrough_when_publicPlatformPath() {
        mockRequest("/api/platform/notification/page");

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    @Test
    void should_passThrough_when_authLogin() {
        mockRequest("/api/auth/login");

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    @Test
    void should_passThrough_when_webSocket() {
        mockRequest("/ws/notification?token=abc");

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    private void mockRequest(String path) {
        lenient().when(request.getURI()).thenReturn(URI.create(path));
    }

    private void mockNotFoundResponse() {
        lenient().when(response.getHeaders()).thenReturn(new HttpHeaders());
        lenient().when(response.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        lenient().when(response.writeWith(any())).thenReturn(Mono.empty());
    }
}
