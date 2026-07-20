package com.meetinghub.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private static final List<String> WHITE_LIST = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/user/register"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        for (String whitePath : WHITE_LIST) {
            if (path.startsWith(whitePath)) {
                return chain.filter(exchange);
            }
        }

        String token = exchange.getRequest().getHeaders().getFirst(TOKEN_HEADER);
        if (!StringUtils.hasText(token) || !token.startsWith(TOKEN_PREFIX)) {
            log.warn("请求缺少有效Token: path={}", path);
            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 简单解析 JWT 获取 role（Base64 解码 payload）
        String role = extractRoleFromToken(token.replace(TOKEN_PREFIX, ""));

        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(TOKEN_HEADER, token)
                .header("X-User-Role", role != null ? role : "")
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    private String extractRoleFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            // 简单解析 JSON 中的 role 字段
            int roleIdx = payload.indexOf("\"role\":\"");
            if (roleIdx < 0) return null;
            int start = roleIdx + 8;
            int end = payload.indexOf("\"", start);
            return payload.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
