package com.meetinghub.user.model.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门视图对象
 */
@Data
public class DepartmentVO {

    /**
     * 部门ID
     */
    private Long id;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 父部门ID, 0为顶级
     */
    private Long parentId;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 状态: 0-禁用, 1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 子部门列表
     */
    private List<DepartmentVO> children;
}
