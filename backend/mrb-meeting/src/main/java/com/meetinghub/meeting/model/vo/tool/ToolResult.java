package com.meetinghub.meeting.model.vo.tool;

/**
 * AI 工具返回值的统一标记接口。
 * <p>
 * 工具方法查询数据后，构造具体结果 VO（实现本接口），
 * 再交由 {@code ToolResponseFormatter.format(ToolResult)} 统一格式化为最终字符串。
 * </p>
 * <p>
 * 设计原则：
 * 1. 工具方法只负责「查询 → 组装 VO」，不直接拼接字符串；
 * 2. 字符串格式化收敛到格式化器一处，避免各工具风格不一致；
 * 3. 结果 VO 仅携带展示所需的字段，屏蔽 userId、deleted 等内部/敏感字段。
 * </p>
 */
public interface ToolResult {

    /**
     * 纯文本结果：用于错误提示、参数校验失败、简单确认等无需结构化展示的场景。
     *
     * @param message 文本内容
     */
    record TextResult(String message) implements ToolResult {
    }
}
