package com.meetinghub.platform.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MenuUpdateDTO {

    @NotNull(message = "菜单ID不能为空")
    private Long id;

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 64, message = "菜单名称不能超过64个字符")
    private String name;

    private String path;

    private String icon;

    private Long parentId;

    private Integer sortOrder;

    private Integer status;

    private Integer visible;
}
