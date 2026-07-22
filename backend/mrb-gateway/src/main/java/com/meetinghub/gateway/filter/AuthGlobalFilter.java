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

import java.util.Base64;
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

        String payload = extractPayload(token.replace(TOKEN_PREFIX, ""));
        String role = extractJsonField(payload, "role");
        String userId = extractJsonField(payload, "sub");
        String username = extractJsonField(payload, "username");

        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(TOKEN_HEADER, token)
                .header("X-User-Role", role != null ? role : "")
                .header("X-User-Id", userId != null ? userId : "")
                .header("X-User-Username", username != null ? username : "")
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    private String extractPayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            return new String(Base64.getUrlDecoder().decode(parts[1]));
        } catch (Exception e) {
            return null;
        }
    }

    private String extractJsonField(String json, String field) {
        if (json == null) return null;
        String pattern = "\"" + field + "\":\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) {
            // 数值型 (如 sub)
            pattern = "\"" + field + "\":";
            idx = json.indexOf(pattern);
            if (idx < 0) return null;
            int start = idx + pattern.length();
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
                end++;
            }
            return json.substring(start, end);
        }
        int start = idx + pattern.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
