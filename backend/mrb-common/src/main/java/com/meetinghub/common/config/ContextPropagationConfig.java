package com.meetinghub.common.config;

import com.meetinghub.common.context.UserContextThreadLocalAccessor;
import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * Reactor 上下文传播配置。
 * <p>
 * 将 {@link UserContextThreadLocalAccessor} 注册到全局 {@link ContextRegistry}，
 * 使 Spring Boot 自动启用的 Reactor 上下文传播机制能在 Flux/Mono 跨线程时
 * 自动恢复 UserContext ThreadLocal，供 AI 工具方法在流式响应线程中使用。
 * </p>
 */
@Configuration
public class ContextPropagationConfig {

    @PostConstruct
    public void registerThreadLocalAccessor() {
        ContextRegistry.getInstance()
                .registerThreadLocalAccessor(new UserContextThreadLocalAccessor());
    }
}
