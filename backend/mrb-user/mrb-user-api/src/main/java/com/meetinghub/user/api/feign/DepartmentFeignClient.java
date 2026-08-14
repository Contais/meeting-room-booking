package com.meetinghub.user.api.feign;

import com.meetinghub.common.result.Result;
import com.meetinghub.user.api.model.dto.DepartmentBriefDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * mrb-user 部门远程调用客户端
 */
@FeignClient(name = "mrb-user", contextId = "departmentFeignClient")
public interface DepartmentFeignClient {

    /**
     * 查询扁平化的部门列表（不含树结构）
     */
    @GetMapping("/uc/department/list")
    Result<List<DepartmentBriefDTO>> listFlat();
}
