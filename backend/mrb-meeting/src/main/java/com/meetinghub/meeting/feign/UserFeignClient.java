package com.meetinghub.meeting.feign;

import com.meetinghub.common.model.dto.AuthUserDTO;
import com.meetinghub.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "mrb-user")
public interface UserFeignClient {

    @GetMapping("/user/internal/info/username/{username}")
    Result<AuthUserDTO> getUserForAuth(@PathVariable("username") String username);

    /**
     * 批量查询用户名（id -> username），消除逐个拉取的 N+1 调用
     */
    @GetMapping("/user/internal/batch")
    Result<Map<Long, String>> batchUsernames(@RequestParam("ids") List<Long> ids);
}
