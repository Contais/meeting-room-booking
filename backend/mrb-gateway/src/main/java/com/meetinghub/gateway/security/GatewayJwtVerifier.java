package com.meetinghub.gateway.security;

import com.meetinghub.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 网关 JWT 验签器
 * <p>
 * 使用与 auth 服务共享的 HMAC 密钥校验签名与过期时间，防止伪造 token 注入身份头。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayJwtVerifier {

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 验签并解析 token；签名无效、token 过期、claims 缺失或解析失败时
     * 返回 {@link Optional#empty()}。
     *
     * @param token 不含 Bearer 前缀的原始 JWT
     */
    public Optional<TokenClaims> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userIdStr = claims.getSubject();
            if (userIdStr == null || userIdStr.isBlank()) {
                log.debug("JWT 缺少 sub 声明");
                return Optional.empty();
            }

            Long userId = Long.valueOf(userIdStr);
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);
            return Optional.of(new TokenClaims(userId, username, role));
        } catch (Exception e) {
            log.debug("JWT 验签失败: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
