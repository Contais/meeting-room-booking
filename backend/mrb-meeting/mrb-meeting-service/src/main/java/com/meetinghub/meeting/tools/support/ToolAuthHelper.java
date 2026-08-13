package com.meetinghub.meeting.tools.support;

import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import org.springframework.ai.chat.model.ToolContext;

/**
 * AI 工具统一鉴权助手。
 * <p>
 * 从 Spring AI 的 {@link ToolContext} 中提取当前用户身份（userId/role）。
 * 该上下文由 {@link com.meetinghub.meeting.controller.ChatController} 在
 * Servlet 请求线程中捕获后，通过 {@code .toolContext(Map)} 显式传入 Flux 管道，
 * 供工具回调在异步线程中安全读取。
 * </p>
 * <p>
 * 不再依赖 {@link com.meetinghub.common.context.UserContext} 的 ThreadLocal：
 * Spring AI 工具回调执行线程与 Servlet 请求线程并非同一线程，
 * Reactor 上下文传播在工具执行路径上不可靠，ThreadLocal 无法保证被恢复。
 * </p>
 */
public final class ToolAuthHelper {

    /** ToolContext 中存储当前用户 ID 的键 */
    public static final String KEY_USER_ID = "userId";
    /** ToolContext 中存储当前用户角色的键 */
    public static final String KEY_ROLE = "role";

    private ToolAuthHelper() {
    }

    /**
     * 获取当前登录用户 ID，未登录抛 {@link BusinessException}。
     *
     * @param toolContext Spring AI 工具上下文，由 ChatController 注入
     * @return 当前用户 ID
     */
    public static Long requireUserId(ToolContext toolContext) {
        if (toolContext == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "当前用户未登录");
        }
        Object userId = toolContext.getContext().get(KEY_USER_ID);
        if (userId instanceof Long id) {
            return id;
        }
        if (userId instanceof Number n) {
            return n.longValue();
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "当前用户未登录");
    }

    /**
     * 获取当前登录用户角色，可能为 null。
     *
     * @param toolContext Spring AI 工具上下文，由 ChatController 注入
     * @return 角色字符串，未登录或未注入时返回 null
     */
    public static String getCurrentRole(ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }
        Object role = toolContext.getContext().get(KEY_ROLE);
        return role == null ? null : role.toString();
    }
}
