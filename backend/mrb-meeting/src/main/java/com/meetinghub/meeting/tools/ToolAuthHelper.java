package com.meetinghub.meeting.tools;

import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

/**
 * AI 工具鉴权辅助类
 * <p>
 * 从 {@link ToolContext} 中提取由 ChatController 通过 .toolContext() 注入的用户身份信息，
 * 提供 userId 获取和角色校验能力。Tool 方法按需调用，无需鉴权的工具无需引入。
 * </p>
 */
public final class ToolAuthHelper {

    public static final String CTX_USER_ID = "userId";
    public static final String CTX_ROLE = "role";

    private ToolAuthHelper() {
    }

    /**
     * 从 ToolContext 获取当前登录用户 ID，未登录则抛出业务异常
     */
    public static Long requireUserId(ToolContext toolContext) {
        if (toolContext == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        Map<String, Object> ctx = toolContext.getContext();
        Object userId = ctx.get(CTX_USER_ID);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        if (userId instanceof Long l) {
            return l;
        }
        try {
            return Long.parseLong(userId.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "用户身份无效");
        }
    }

    /**
     * 从 ToolContext 获取当前用户角色，未登录返回 null
     */
    public static String getCurrentRole(ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }
        Object role = toolContext.getContext().get(CTX_ROLE);
        return role != null ? role.toString() : null;
    }

    /**
     * 校验当前用户具有指定角色，否则抛出 403
     */
    public static void requireRole(ToolContext toolContext, String requiredRole) {
        requireUserId(toolContext);
        String role = getCurrentRole(toolContext);
        if (role == null || !role.equals(requiredRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(),
                    "需要角色: " + requiredRole);
        }
    }
}