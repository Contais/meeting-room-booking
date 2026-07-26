package com.meetinghub.common.context;

import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;

/**
 * 当前登录用户上下文
 * <p>
 * 由 {@link com.meetinghub.common.interceptor.UserContextInterceptor} 在请求到达时
 * 从网关注入的 HTTP 头（X-User-Id / X-User-Role / X-User-Username）解析后填充，
 * 通过 {@link ThreadLocal} 在整个请求处理链路中共享。
 * </p>
 * <p>
 * 使用约束：
 * <ul>
 *     <li>仅可在 Controller 层或同线程同步逻辑中读取；</li>
 *     <li>对响应式（Flux/Mono）或线程池异步逻辑，必须在切换线程前显式捕获值；</li>
 *     <li>Service 层应继续通过方法参数接收 userId，避免与上下文耦合以保持可测试性。</li>
 * </ul>
 * </p>
 */
public class UserContext {

    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    private Long userId;
    private String username;
    private String role;

    private UserContext() {
    }

    /**
     * 设置当前线程的用户上下文，仅供拦截器调用
     */
    public static void set(UserContext context) {
        HOLDER.set(context);
    }

    /**
     * 清除当前线程的用户上下文，必须在 afterCompletion 中调用以防线程池内存泄漏
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 获取当前线程的用户上下文，可能为 null（如白名单或内部 Feign 接口）
     */
    public static UserContext get() {
        return HOLDER.get();
    }

    /**
     * 获取当前登录用户 ID，若未登录则抛出业务异常
     */
    public static Long getCurrentUserId() {
        UserContext ctx = HOLDER.get();
        if (ctx == null || ctx.userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "当前用户未登录");
        }
        return ctx.userId;
    }

    /**
     * 获取当前登录用户角色，未登录返回 null
     */
    public static String getCurrentRole() {
        UserContext ctx = HOLDER.get();
        return ctx == null ? null : ctx.role;
    }

    /**
     * 获取当前登录用户名，未登录返回 null
     */
    public static String getCurrentUsername() {
        UserContext ctx = HOLDER.get();
        return ctx == null ? null : ctx.username;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    /**
     * 工厂方法：从网关注入的 HTTP 头构建上下文
     *
     * @param userIdHeader X-User-Id 头（字符串形式 Long）
     * @param roleHeader    X-User-Role 头
     * @param usernameHeader X-User-Username 头
     * @return 构建好的上下文；userId 非法时仍构建上下文但 userId 为 null
     */
    public static UserContext of(String userIdHeader, String roleHeader, String usernameHeader) {
        UserContext ctx = new UserContext();
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                ctx.userId = Long.parseLong(userIdHeader);
            } catch (NumberFormatException ignored) {
                // 非法 userId 头留空，由调用方在需要时抛 401
            }
        }
        ctx.role = roleHeader;
        ctx.username = usernameHeader;
        return ctx;
    }
}
