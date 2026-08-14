package com.meetinghub.meeting.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 会议室服务 OpenAPI 文档配置。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI meetingOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("会议室预约系统 - 会议室服务")
                .description("会议室、预约、设备、首页统计与 AI 助手接口")
                .version("1.0.0"));
    }
}
