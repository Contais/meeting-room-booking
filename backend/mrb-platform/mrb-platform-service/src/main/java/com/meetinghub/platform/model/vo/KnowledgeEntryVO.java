package com.meetinghub.platform.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识条目视图对象（管理端）
 */
@Data
public class KnowledgeEntryVO implements Serializable {

    private Long id;

    /** 分类编码 */
    private String category;

    /** 分类中文名 */
    private String categoryName;

    private String title;

    private String question;

    private String answer;

    private String tags;

    private Integer sort;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
