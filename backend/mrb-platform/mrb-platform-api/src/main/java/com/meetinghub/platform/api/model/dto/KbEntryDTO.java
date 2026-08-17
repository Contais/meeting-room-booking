package com.meetinghub.platform.api.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

/**
 * 知识库检索条目 DTO（服务间 Feign 传输）
 * <p>
 * 仅携带 AI 生成回答所需字段，屏蔽 id、status、create_time 等内部字段。
 * 使用 {@code @JsonIgnoreProperties(ignoreUnknown = true)} 容忍远端扩展字段。
 * </p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KbEntryDTO implements Serializable {

    /** 条目标题/来源（如「预约规则·提前预约天数」） */
    private String title;

    /** 分类中文名（如「预约规则」） */
    private String category;

    /** 答案内容 */
    private String answer;

    /** 标签（逗号分隔） */
    private String tags;

}
