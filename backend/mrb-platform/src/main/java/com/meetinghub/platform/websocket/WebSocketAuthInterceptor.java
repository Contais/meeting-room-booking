package com.meetinghub.platform.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器：从网关注入的 X-User-Id 头中提取用户ID
 * <p>
 * 网关 AuthGlobalFilter 已校验 JWT 并将 userId 注入到 X-User-Id 头，
 * 此拦截器将其存入 WebSocketSession.attributes 供 Handler 使用。
 * </p>
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String userIdStr = servletRequest.getServletRequest().getHeader("X-User-Id");
            if (userIdStr == null || userIdStr.isBlank()) {
                log.warn("WebSocket 握手缺少 X-User-Id 头，拒绝连接");
                return false;
            }
            try {
                attributes.put("userId", Long.parseLong(userIdStr));
                return true;
            } catch (NumberFormatException e) {
                log.warn("WebSocket 握手 X-User-Id 格式非法: {}", userIdStr);
                return false;
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
