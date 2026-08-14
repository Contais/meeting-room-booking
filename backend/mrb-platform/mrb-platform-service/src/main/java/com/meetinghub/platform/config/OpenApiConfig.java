package com.meetinghub.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 平台服务 OpenAPI 文档配置。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI platformOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("会议室预约系统 - 平台服务")
                .description("菜单、角色、通知、字典、系统配置与文件接口")
                .version("1.0.0"));
    }
}
