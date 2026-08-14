package com.meetinghub.gateway.filter;

import com.meetinghub.gateway.security.TokenClaims;
import com.meetinghub.gateway.security.TokenValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 全局认证过滤器：验签 + Redis 校验 Token，校验通过后注入用户信息头
 * <p>
 * 仅在验签和 Redis 一致性校验全部通过后，才将 JWT claims 写入 X-User-* 头；
 * 同时覆盖客户端自带的 X-User-* 头，防止下游服务信任伪造身份。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private static final String X_USER_ID = "X-User-Id";
    private static final String X_USER_ROLE = "X-User-Role";
    private static final String X_USER_USERNAME = "X-User-Username";

    private static final List<String> WHITE_LIST = List.of(
            "/api/auth/login",
            // 本地存储模式下文件静态资源对外公开读（与 COS 公开读一致），
            // 上传接口仍需鉴权
            "/api/file/static/"
    );

    private static final String UNAUTHORIZED_BODY =
            "{\"code\":401,\"message\":\"未授权或Token无效\"," +
                    "\"data\":null}";

    private final TokenValidationService tokenValidationService;

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
            // WebSocket 连接无法设置 Authorization 头，从 query param 降级提取
            String query = exchange.getRequest().getURI().getQuery();
            if (query != null && query.contains("token=")) {
                String wsToken = query.split("token=")[1].split("&")[0];
                token = TOKEN_PREFIX + wsToken;
            }
        }
        if (!StringUtils.hasText(token) || !token.startsWith(TOKEN_PREFIX)) {
            return rejectUnauthorized(exchange, "请求缺少有效Token");
        }

        String bearerToken = token;
        String rawToken = bearerToken.substring(TOKEN_PREFIX.length());
        return tokenValidationService.validate(rawToken)
                .flatMap(claimsOptional -> {
                    if (claimsOptional.isEmpty()) {
                        return rejectUnauthorized(exchange, "Token验签或Redis校验未通过");
                    }

                    TokenClaims claims = claimsOptional.get();
                    ServerHttpRequest request = exchange.getRequest().mutate()
                            .headers(headers -> {
                                headers.remove(X_USER_ID);
                                headers.remove(X_USER_ROLE);
                                headers.remove(X_USER_USERNAME);
                            })
                            .header(TOKEN_HEADER, bearerToken)
                            .header(X_USER_ID, String.valueOf(claims.userId()))
                            .header(X_USER_ROLE, claims.role() != null ? claims.role() : "")
                            .header(X_USER_USERNAME, claims.username() != null ? claims.username() : "")
                            .build();
                    return chain.filter(exchange.mutate().request(request).build());
                });
    }

    /**
     * 返回 401 并写入符合 {code, message, data} 结构的 JSON 响应体
     */
    private Mono<Void> rejectUnauthorized(ServerWebExchange exchange, String reason) {
        log.warn("鉴权失败: path={}, reason={}", exchange.getRequest().getURI().getPath(), reason);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = UNAUTHORIZED_BODY.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
