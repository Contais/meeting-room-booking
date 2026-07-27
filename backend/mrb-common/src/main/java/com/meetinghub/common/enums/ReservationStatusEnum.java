package com.meetinghub.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 预约状态枚举
 */
@Getter
@AllArgsConstructor
public enum ReservationStatusEnum {

    PENDING(0, "待确认"),
    CONFIRMED(1, "已确认"),
    CANCELLED(2, "已取消"),
    REJECTED(3, "已拒绝");

    /**
     * 查询时需排除的状态码集合（已取消 + 已拒绝，表示时段已释放）
     * 统一引用此常量，避免散落各处的 Arrays.asList(CANCELLED, REJECTED) 重复代码
     */
    public static final List<Integer> EXCLUDED_CODES = List.of(CANCELLED.getCode(), REJECTED.getCode());

    private final Integer code;
    private final String desc;

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return 枚举实例
     */
    public static ReservationStatusEnum fromCode(Integer code) {
        for (ReservationStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return PENDING;
    }

    /**
     * 根据状态码获取描述
     * @param code 状态码
     * @return 描述
     */
    public static String getDescByCode(Integer code) {
        for (ReservationStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status.desc;
            }
        }
        return PENDING.desc;
    }
}
