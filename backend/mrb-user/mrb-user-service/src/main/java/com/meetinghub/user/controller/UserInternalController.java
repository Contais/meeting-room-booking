package com.meetinghub.user.controller;

import com.meetinghub.common.result.Result;
import com.meetinghub.user.api.model.dto.AuthUserDTO;
import com.meetinghub.user.model.entity.User;
import com.meetinghub.user.model.vo.UserVO;
import com.meetinghub.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户内部接口控制器（服务间 Feign）
 * <p>
 * 路径前缀 {@code /uc/user/internal/**}，仅供服务间 Feign 调用，不经过网关。
 * </p>
 */
@RestController
@RequestMapping("/uc/user/internal")
@RequiredArgsConstructor
@Tag(name = "用户内部接口", description = "服务间 Feign 调用，不经过网关")
public class UserInternalController {

    private final UserService userService;

    @Operation(summary = "按用户名查询鉴权信息")
    @GetMapping("/info/username/{username}")
    public Result<AuthUserDTO> getUserForAuth(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.ok(null);
        }
        AuthUserDTO dto = new AuthUserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setPassword(user.getPassword());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        return Result.ok(dto);
    }

    /**
     * 批量查询用户名（id -> username），供跨服务回填展示名，消除 N+1 调用
     */
    @Operation(summary = "批量查询用户名")
    @GetMapping("/batch")
    public Result<Map<Long, String>> batchUsernames(@RequestParam List<Long> ids) {
        return Result.ok(userService.getUsernamesByIds(ids));
    }

    /**
     * 按部门子树查询所有启用用户（跨服务调用，供 mrb-meeting 邀请参会人使用）。
     * 范围包含目标部门及其所有启用后代部门。
     */
    @Operation(summary = "按部门子树查询启用用户")
    @GetMapping("/list-by-department")
    public Result<List<UserVO>> listByDepartment(@RequestParam Long departmentId) {
        return Result.ok(userService.listContactsByDepartmentTree(departmentId));
    }

    /**
     * 按 ID 批量查询用户完整信息（供 mrb-meeting 回填参会人详情）
     */
    @Operation(summary = "按 ID 批量查询用户详情")
    @GetMapping("/list-by-ids")
    public Result<List<UserVO>> listByIds(@RequestParam List<Long> ids) {
        return Result.ok(userService.listByIdsDetailed(ids));
    }
}
