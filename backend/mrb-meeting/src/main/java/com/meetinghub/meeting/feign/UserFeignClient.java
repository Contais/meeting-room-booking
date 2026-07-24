package com.meetinghub.meeting.feign;

import com.meetinghub.common.model.dto.AuthUserDTO;
import com.meetinghub.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "mrb-user")
public interface UserFeignClient {

    @GetMapping("/user/internal/info/username/{username}")
    Result<AuthUserDTO> getUserForAuth(@PathVariable("username") String username);
}
