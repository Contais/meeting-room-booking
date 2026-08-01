package com.meetinghub.user.api.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 部门信息 DTO（跨服务传输）
 * <p>
 * 使用 {@code @JsonIgnoreProperties(ignoreUnknown = true)} 容忍远端扩展字段，
 * 防止 mrb-user 的 DepartmentVO 新增字段后导致 Feign 反序列化失败。
 * </p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DepartmentBriefDTO implements Serializable {
    private Long id;
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private Integer status;
    private List<DepartmentBriefDTO> children;
}
