package com.meetinghub.meeting.tools;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AI 聊天助手工具类 - 公共信息工具类
 */
@Component
@RequiredArgsConstructor
public class CommonTool {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    @Tool(description = "获取当前日期时间, 返回格式 yyyy-MM-dd HH:mm")
    public String currentTime() {
        return LocalDateTime.now().format(DATE_TIME_FMT);
    }
    
    @Tool(description = "获取当前日期, 返回格式 yyyy-MM-dd")
    public String currentDate() {
        return LocalDate.now().format(DATE_FMT);
    }

}
