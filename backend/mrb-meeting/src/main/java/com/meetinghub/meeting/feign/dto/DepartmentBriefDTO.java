package com.meetinghub.meeting.feign.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 部门信息 DTO（跨服务传输）
 */
@Data
public class DepartmentBriefDTO implements Serializable {
    private Long id;
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private Integer status;
    private List<DepartmentBriefDTO> children;
}
