package com.meetinghub.user.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 部门更新请求参数
 */
@Data
public class DepartmentUpdateDTO {

    @NotNull(message = "部门ID不能为空")
    private Long id;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 64, message = "部门名称不能超过64个字符")
    private String name;

    /**
     * 父部门ID, 0为顶级
     */
    private Long parentId;

    /**
     * 排序号
     */
    private Integer sortOrder;
}
