package com.meetinghub.meeting.tools;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * AI 聊天助手工具类 - 公共信息工具类
 */
@Component
@RequiredArgsConstructor
public class CommonTool {

    @Tool(description = "获取当前时间")
    public LocalDateTime current() {
        return LocalDateTime.now();
    }

}
