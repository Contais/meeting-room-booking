package com.meetinghub.gateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * 网关 Token 校验服务
 * <p>
 * 验签通过后比对 Redis 中保存的 token，保证登出/刷新后旧 token 立即失效；
 * Redis 异常按校验失败处理（fail-closed）。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenValidationService {

    /**
     * 与 mrb-common RedisKeyConstant.USER_TOKEN 保持一致（mrb:user:token:）。
     * 网关为 WebFlux 工程，不引入 mrb-common 以避免 Servlet 依赖污染。
     */
    private static final String USER_TOKEN_KEY_PREFIX = "mrb:user:token:";

    private final GatewayJwtVerifier verifier;
    private final ReactiveStringRedisTemplate redisTemplate;

    /**
     * 校验 token：签名/过期无效、Redis 无记录或与当前 token 不一致时
     * 返回 {@link Optional#empty()}。
     *
     * @param token 不含 Bearer 前缀的原始 JWT
     */
    public Mono<Optional<TokenClaims>> validate(String token) {
        return Mono.just(token)
                .map(verifier::verify)
                .flatMap(claimsOptional -> {
                    if (claimsOptional.isEmpty()) {
                        return Mono.just(Optional.empty());
                    }
                    TokenClaims claims = claimsOptional.get();
                    String redisKey = USER_TOKEN_KEY_PREFIX + claims.userId();
                    return redisTemplate.opsForValue().get(redisKey)
                            .map(stored -> stored.equals(token)
                                    ? Optional.of(claims)
                                    : Optional.<TokenClaims>empty())
                            .defaultIfEmpty(Optional.empty())
                            .onErrorResume(error -> {
                                log.error("Redis token 校验异常: userId={}", claims.userId(), error);
                                return Mono.just(Optional.empty());
                            });
                });
    }
}
