package com.meetinghub.common.interceptor;

import com.meetinghub.common.annotation.RequiresRole;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 角色校验拦截器
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequiresRole requiresRole = handlerMethod.getMethodAnnotation(RequiresRole.class);
        if (requiresRole == null) {
            return true;
        }

        String role = request.getHeader("X-User-Role");
        if (role == null || !role.equals(requiresRole.value())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "无权访问，需要角色: " + requiresRole.value());
        }

        return true;
    }
}
