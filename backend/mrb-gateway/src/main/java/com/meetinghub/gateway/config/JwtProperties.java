package com.meetinghub.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 网关 JWT 配置属性
 * <p>
 * secret 必须与 mrb-auth-service 的 jwt.secret 保持一致，否则验签失败。
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
}
