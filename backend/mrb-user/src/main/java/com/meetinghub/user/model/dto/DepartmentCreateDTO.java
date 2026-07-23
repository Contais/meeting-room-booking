package com.meetinghub.user.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 部门新增请求参数
 */
@Data
public class DepartmentCreateDTO {

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 64, message = "部门名称不能超过64个字符")
    private String name;

    /** 父部门ID, 0为顶级 */
    private Long parentId;

    /** 排序号 */
    private Integer sortOrder;
}
