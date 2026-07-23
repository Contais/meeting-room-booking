package com.meetinghub.meeting;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.meetinghub")
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.meetinghub.meeting.repository")
/** 会议室服务启动类 */
public class MeetingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetingApplication.class, args);
    }
}
