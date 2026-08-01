package com.meetinghub.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 平台基础服务启动类
 * <p>
 * 承载文件存储（含 COS 预签名）、消息通知（含 WebSocket 实时推送）、字典、系统配置。
 * </p>
 */
@SpringBootApplication(scanBasePackages = "com.meetinghub")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.meetinghub")
@MapperScan("com.meetinghub.platform.repository")
public class PlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}
