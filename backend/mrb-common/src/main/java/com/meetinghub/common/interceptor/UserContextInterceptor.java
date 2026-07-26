package com.meetinghub.common.interceptor;

import com.meetinghub.common.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户上下文拦截器
 * <p>
 * 在请求进入 Controller 之前，从网关注入的 HTTP 头解析用户信息并放入
 * {@link UserContext}（基于 ThreadLocal），在请求结束后清理以避免线程池内存泄漏。
 * </p>
 * <p>
 * 适用于 Spring MVC（servlet）侧服务；网关侧（WebFlux）使用
 * {@link com.meetinghub.gateway.filter.AuthGlobalFilter} 直接解析 JWT 注入头。
 * </p>
 */
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_USER_USERNAME = "X-User-Username";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserContext context = UserContext.of(
                request.getHeader(HEADER_USER_ID),
                request.getHeader(HEADER_USER_ROLE),
                request.getHeader(HEADER_USER_USERNAME)
        );
        UserContext.set(context);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        UserContext.clear();
    }
}
