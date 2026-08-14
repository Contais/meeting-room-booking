package com.meetinghub.user.api.feign;

import com.meetinghub.common.result.Result;
import com.meetinghub.user.api.model.dto.AuthUserDTO;
import com.meetinghub.user.api.model.dto.UserBriefDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * mrb-user 远程调用客户端
 * <p>
 * 内部接口（/uc/user/internal/**）仅供服务间调用，绕过网关鉴权。
 * 返回的 {@link Result} 中 data 可能为 null，调用方需做防御性判空。
 * </p>
 */
@FeignClient(name = "mrb-user")
public interface UserFeignClient {

    @GetMapping("/uc/user/internal/info/username/{username}")
    Result<AuthUserDTO> getUserForAuth(@PathVariable("username") String username);

    /**
     * 批量查询用户名（id -> username），消除逐个拉取的 N+1 调用
     */
    @GetMapping("/uc/user/internal/batch")
    Result<Map<Long, String>> batchUsernames(@RequestParam("ids") List<Long> ids);

    /**
     * 按部门 ID 查询所有启用用户（用于邀请参会人）
     */
    @GetMapping("/uc/user/internal/list-by-department")
    Result<List<UserBriefDTO>> listByDepartment(@RequestParam("departmentId") Long departmentId);

    /**
     * 按 ID 批量查询用户完整信息（用于回填参会人详情）
     */
    @GetMapping("/uc/user/internal/list-by-ids")
    Result<List<UserBriefDTO>> listByIds(@RequestParam("ids") List<Long> ids);
}
