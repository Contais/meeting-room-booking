package com.meetinghub.user.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户分页查询参数
 */
@Data
public class UserPageQuery implements Serializable {

    private Integer page = 1;
    private Integer size = 10;
    private String keyword;
    private Integer status;
}
