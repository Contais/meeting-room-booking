package com.meetinghub.meeting.tools;

import com.meetinghub.common.constant.DateTimePatternConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI 聊天助手工具类 - 公共信息工具类
 */
@Component
@RequiredArgsConstructor
public class CommonTool {

    @Tool(description = "获取当前日期时间, 返回格式 yyyy-MM-dd HH:mm:ss")
    public String currentTime() {
        return LocalDateTime.now().format(DateTimePatternConstant.DATETIME_FMT);
    }
    
    @Tool(description = "获取当前日期, 返回格式 yyyy-MM-dd")
    public String currentDate() {
        return LocalDate.now().format(DateTimePatternConstant.DATE_FMT);
    }

}
