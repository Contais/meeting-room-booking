package com.meetinghub.platform.model.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MenuVO {

    private Long id;
    private String name;
    private String path;
    private String icon;
    private Long parentId;
    private Integer sortOrder;
    private Integer visible;
    private Integer status;
    private LocalDateTime createTime;
    private List<MenuVO> children;
}
