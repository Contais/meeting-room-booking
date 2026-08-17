package com.meetinghub.platform.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识条目新增 DTO
 */
@Data
public class KnowledgeCreateDTO {

    @NotBlank(message = "分类不能为空")
    private String category;

    @NotBlank(message = "条目标题不能为空")
    @Size(max = 128, message = "条目标题不能超过128个字符")
    private String title;

    @NotBlank(message = "问题不能为空")
    @Size(max = 512, message = "问题不能超过512个字符")
    private String question;

    @NotBlank(message = "答案不能为空")
    private String answer;

    @Size(max = 255, message = "标签不能超过255个字符")
    private String tags;

    private Integer sort;

    private Integer status;

}
