package com.meetinghub.meeting.tools;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import java.util.Arrays;

/**
 * 为 AI 工具统一注入调用日志的 {@link ToolCallbackProvider}。
 * <p>
 * 基于 {@link MethodToolCallbackProvider} 生成各 {@code @Tool} 方法的回调后，
 * 用 {@link LoggingToolCallback} 包装，使每次工具调用都有「工具名 + 入参 + 耗时 + 结果/异常」日志。
 * </p>
 */
public class LoggingToolCallbackProvider implements ToolCallbackProvider {

    private final Object[] toolObjects;

    public LoggingToolCallbackProvider(Object... toolObjects) {
        this.toolObjects = toolObjects;
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        return Arrays.stream(MethodToolCallbackProvider.builder()
                        .toolObjects(toolObjects)
                        .build()
                        .getToolCallbacks())
                .map(LoggingToolCallback::new)
                .toArray(ToolCallback[]::new);
    }
}
