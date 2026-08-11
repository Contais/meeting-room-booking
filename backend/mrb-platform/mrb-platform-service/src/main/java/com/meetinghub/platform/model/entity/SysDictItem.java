package com.meetinghub.platform.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.meetinghub.common.model.BaseEntity;
import lombok.Data;

/**
 * 字典项表实体
 */
@Data
@TableName("sys_dict_item")
public class SysDictItem extends BaseEntity {

    /** 所属字典 ID */
    private Long dictId;

    /** 字典项编码 */
    private String code;

    /** 展示标签 */
    private String label;

    /** 字典项值 */
    private String value;

    /** 排序号（升序） */
    private Integer sort;

    /** 状态：0-禁用，1-启用 */
    private Integer status;

}
