package com.meetinghub.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 内部接口访问拦截过滤器
 * <p>
 * 服务间 Feign 内部接口（路径形如 {@code /api/{service}/internal/{resource}}）仅供服务间直连（{@code lb://}）调用，
 * 禁止经网关从外部访问，统一短路返回 404，避免暴露内部端点。
 * </p>
 */
@Component
public class BlockInternalPathFilter implements GlobalFilter, Ordered {

    private static final String API_PREFIX = "/api/";
    private static final String INTERNAL_SEGMENT = "/internal/";

    private static final String NOT_FOUND_BODY =
            "{\"code\":404,\"message\":\"资源不存在\",\"data\":null}";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith(API_PREFIX) && path.contains(INTERNAL_SEGMENT)) {
            return rejectNotFound(exchange);
        }
        return chain.filter(exchange);
    }

    private Mono<Void> rejectNotFound(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.NOT_FOUND);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = NOT_FOUND_BODY.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}
