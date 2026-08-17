package com.meetinghub.platform.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.meetinghub.common.model.BaseEntity;
import lombok.Data;

/**
 * 平台知识库条目实体
 * <p>
 * 存放 AI 助手无法从业务表直接推导的非结构化知识：
 * 预约规则条款、操作流程、异常处理、运营公告等。
 * </p>
 */
@Data
@TableName("platform_knowledge_entry")
public class KnowledgeEntry extends BaseEntity {

    /** 分类编码（见 {@link com.meetinghub.platform.enums.KnowledgeCategoryEnum}） */
    private String category;

    /** 条目标题/来源 */
    private String title;

    /** 常见问法/问题 */
    private String question;

    /** 答案内容 */
    private String answer;

    /** 标签（逗号分隔） */
    private String tags;

    /** 排序号 */
    private Integer sort;

    /** 状态: 0-禁用, 1-启用 */
    private Integer status;

}
