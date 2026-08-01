package com.meetinghub.meeting.model.vo.tool;

/**
 * AI 工具返回值的统一标记接口。
 * <p>
 * 工具方法直接返回结构化 record / List，由 Spring AI 的
 * {@code DefaultToolCallResultConverter} 序列化为 JSON 回传模型，
 * 不再经过 {@code ToolResponseFormatter} 渲染为字符串。
 * </p>
 * <p>
 * 设计原则：
 * 1. 工具方法只负责「查询 → 组装结构化结果」，展示文案交给 system prompt 与模型组织；
 * 2. 结果 record 仅携带业务字段，屏蔽 userId、deleted 等内部/敏感字段；
 * 3. 时间字段统一格式化为 {@code yyyy-MM-dd HH:mm} 或 {@code HH:mm} String，
 *    避免依赖 Jackson 默认 ISO-8601 序列化。
 * </p>
 */
public interface ToolResult {

    /**
     * 纯文本提示：用于中性通知场景（保留兼容，新代码优先用 {@link ErrorResult} 或空列表）。
     *
     * @param message 文本内容
     */
    record TextResult(String message) implements ToolResult {
    }

    /**
     * 错误结果：用于参数缺失、格式非法、权限不足、未找到等错误语义。
     *
     * @param message 错误描述
     */
    record ErrorResult(String message) implements ToolResult {
    }
}
