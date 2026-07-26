package com.meetinghub.common.config;

import com.meetinghub.common.interceptor.RoleInterceptor;
import com.meetinghub.common.interceptor.UserContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * <p>
 * 拦截器执行顺序按注册顺序：UserContextInterceptor 必须先于 RoleInterceptor，
 * 以确保角色校验时上下文已就绪。
 * </p>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserContextInterceptor userContextInterceptor;
    private final RoleInterceptor roleInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 用户上下文：解析网关注入的头，填充 ThreadLocal
        registry.addInterceptor(userContextInterceptor)
                .addPathPatterns("/**");

        // 2. 角色校验：依赖已填充的 UserContext
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/user/admin/**");
    }
}
