package com.meetinghub.meeting.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.lang.Nullable;

/**
 * 工具回调日志装饰器。
 * <p>
 * 在真实工具执行前后记录：工具名、原始入参、耗时、结果摘要；执行抛异常时输出异常堆栈。
 * 用于在服务器日志中快速定位 AI 工具链路的断点（如参数类型转换失败、某个工具迟迟未返回）。
 * </p>
 * <p>
 * 注意：必须透传 {@link ToolContext}，否则 {@link ToolAuthHelper} 无法在工具方法中取得当前用户身份。
 * </p>
 */
@Slf4j
public class LoggingToolCallback implements ToolCallback {

    /** 结果日志最大长度，避免长列表刷屏 */
    private static final int MAX_LOG_LENGTH = 500;

    private final ToolCallback delegate;

    public LoggingToolCallback(ToolCallback delegate) {
        this.delegate = delegate;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return execute(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        return execute(toolInput, toolContext);
    }

    private String execute(String toolInput, @Nullable ToolContext toolContext) {
        String toolName = getToolDefinition().name();
        long start = System.currentTimeMillis();
        log.info("AI 工具调用开始, tool={}, args={}", toolName, truncate(toolInput));
        try {
            String result = delegate.call(toolInput, toolContext);
            log.info("AI 工具调用完成, tool={}, 耗时={}ms, 结果={}",
                    toolName, System.currentTimeMillis() - start, truncate(result));
            return result;
        } catch (Exception e) {
            log.error("AI 工具调用失败, tool={}, args={}, 耗时={}ms",
                    toolName, truncate(toolInput), System.currentTimeMillis() - start, e);
            throw e;
        }
    }

    private static String truncate(String text) {
        if (text == null) {
            return "null";
        }
        return text.length() <= MAX_LOG_LENGTH
                ? text
                : text.substring(0, MAX_LOG_LENGTH) + "...(已截断, 共" + text.length() + "字符)";
    }
}
