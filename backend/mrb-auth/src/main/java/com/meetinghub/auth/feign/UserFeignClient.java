package com.meetinghub.auth.feign;

import com.meetinghub.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 用户服务 Feign 客户端
 */
@FeignClient(name = "mrb-user")
public interface UserFeignClient {

    /**
     * 根据用户名查询用户信息（含密码哈希，仅鉴权服务内部使用）
     */
    @GetMapping("/user/internal/info/username/{username}")
    Result<Map<String, Object>> getUserForAuth(@PathVariable("username") String username);
}
