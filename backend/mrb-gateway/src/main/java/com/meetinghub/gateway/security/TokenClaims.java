package com.meetinghub.gateway.security;

/**
 * JWT 验签通过后的用户身份信息
 */
public record TokenClaims(Long userId, String username, String role) {
}
