package com.meetinghub.user.service;

/**
 * 密码重置服务
 */
public interface PasswordResetService {

    /**
     * 发送密码重置验证码到用户绑定邮箱
     *
     * @param username 用户名
     */
    void sendResetCode(String username);

    /**
     * 校验验证码并重置密码
     *
     * @param username    用户名
     * @param code        邮箱验证码
     * @param newPassword 新密码
     */
    void resetPassword(String username, String code, String newPassword);
}
