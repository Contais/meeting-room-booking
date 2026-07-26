package com.meetinghub.common.context;

import io.micrometer.context.ThreadLocalAccessor;

/**
 * UserContext 的 Reactor 上下文传播桥接器。
 * <p>
 * 注册到 {@link io.micrometer.context.ContextRegistry} 后，Spring Boot 3.x
 * 自动启用的 Reactor 上下文传播机制会在 Flux/Mono 跨线程执行时
 * 自动捕获并恢复 {@link UserContext} ThreadLocal，
 * 使 AI 工具方法在流式（SSE）线程中也能通过 {@link UserContext#getCurrentUserId()} 获取当前用户。
 * </p>
 */
public class UserContextThreadLocalAccessor implements ThreadLocalAccessor<UserContext> {

    public static final String KEY = "mrb:userContext";

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public UserContext getValue() {
        return UserContext.get();
    }

    @Override
    public void setValue(UserContext value) {
        UserContext.set(value);
    }

    @Override
    public void setValue() {
        UserContext.clear();
    }
}
