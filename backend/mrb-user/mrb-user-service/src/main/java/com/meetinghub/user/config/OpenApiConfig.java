package com.meetinghub.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 用户服务 OpenAPI 文档配置。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("会议室预约系统 - 用户服务")
                .description("用户与部门管理接口")
                .version("1.0.0"));
    }
}
