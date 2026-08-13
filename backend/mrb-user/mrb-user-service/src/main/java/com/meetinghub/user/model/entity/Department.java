package com.meetinghub.user.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.meetinghub.common.model.BaseEntity;
import lombok.Data;

/**
 * 部门实体
 */
@Data
@TableName("uc_department")
public class Department extends BaseEntity {

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

}
