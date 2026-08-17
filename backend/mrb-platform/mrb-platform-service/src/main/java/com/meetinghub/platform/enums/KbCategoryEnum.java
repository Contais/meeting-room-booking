package com.meetinghub.platform.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 知识库分类枚举
 * <p>
 * 固定分类集合，管理端以下拉选择维护，便于扩展。
 * 结构化规则（审批模式、设备等）不走知识库，由会议室工具实时查询。
 * </p>
 */
@Getter
@AllArgsConstructor
public enum KbCategoryEnum {

    RULES("RULES", "预约规则"),
    FLOW("FLOW", "流程指引"),
    EXCEPTION("EXCEPTION", "异常处理"),
    ANNOUNCEMENT("ANNOUNCEMENT", "公告运营");

    private final String code;
    private final String label;

    /**
     * 根据分类编码获取枚举，未知编码返回 null。
     *
     * @param code 分类编码
     * @return 枚举实例
     */
    public static KbCategoryEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (KbCategoryEnum category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        return null;
    }
}
