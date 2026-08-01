package com.meetinghub.common.constant;

import java.time.format.DateTimeFormatter;

/**
 * 日期时间格式化常量
 * <p>
 * 集中管理跨模块复用的日期时间格式，避免各业务类重复 {@link DateTimeFormatter#ofPattern}。
 * </p>
 */
public final class DateTimePatternConstant {

    private DateTimePatternConstant() {
    }

    /** 标准日期时间格式：yyyy-MM-dd HH:mm:ss（API 字段、通知文本、AI 回复等统一场景） */
    public static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 标准日期格式：yyyy-MM-dd */
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 标准时间格式：HH:mm */
    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
}
