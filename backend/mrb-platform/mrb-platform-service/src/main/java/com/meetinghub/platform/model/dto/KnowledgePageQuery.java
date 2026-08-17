package com.meetinghub.platform.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 知识条目分页查询参数
 */
@Data
public class KnowledgePageQuery implements Serializable {

    private Integer page = 1;

    private Integer size = 10;

    private String keyword;

    private String category;

    private Integer status;

}
