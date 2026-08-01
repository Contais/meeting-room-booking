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

    /** 标准日期时间格式：yyyy-MM-dd HH:mm（通知模板、AI 回复展示等通用场景） */
    public static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
}
