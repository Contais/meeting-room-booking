package com.meetinghub.user.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MenuCreateDTO {

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 64, message = "菜单名称不能超过64个字符")
    private String name;

    private String path;

    private String icon;

    private Long parentId;

    private Integer sortOrder;

    private Integer visible;
}
