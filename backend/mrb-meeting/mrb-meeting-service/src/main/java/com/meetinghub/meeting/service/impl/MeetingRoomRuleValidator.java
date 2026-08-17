package com.meetinghub.meeting.service.impl;

import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 会议室使用规则校验器。
 * <p>
 * 同时服务于会议室管理链路（写入前校验）与预约创建链路（读取旧数据时的防御性校验），
 * 避免非法配置以 {@link DateTimeParseException} 等非业务异常形式泄漏为 500。
 * </p>
 */
final class MeetingRoomRuleValidator {

    private static final DateTimeFormatter BOOKABLE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private MeetingRoomRuleValidator() {
    }

    /**
     * 校验完整会议室规则配置。
     *
     * @param bookableStart 可预约开始时间 HH:mm
     * @param bookableEnd   可预约结束时间 HH:mm
     * @param minDuration   单次最小预约时长（分钟），可为 null（历史数据）
     * @param maxDuration   单次最大预约时长（分钟），可为 null（历史数据）
     * @param advanceDays   提前预约天数，可为 null（历史数据）
     */
    static void validate(String bookableStart, String bookableEnd,
                         Integer minDuration, Integer maxDuration, Integer advanceDays) {
        validateBookableWindow(bookableStart, bookableEnd);
        validateDurationRange(minDuration, maxDuration);
        validateAdvanceDays(advanceDays);
    }

    /**
     * 校验可预约时间窗口：格式必须为 HH:mm，且开始时间早于结束时间。
     */
    static void validateBookableWindow(String bookableStart, String bookableEnd) {
        requireConfigured(bookableStart != null && !bookableStart.isBlank(), "可预约开始时间未配置");
        requireConfigured(bookableEnd != null && !bookableEnd.isBlank(), "可预约结束时间未配置");

        LocalTime start = parseBookableTime(bookableStart, "可预约开始时间");
        LocalTime end = parseBookableTime(bookableEnd, "可预约结束时间");
        if (!start.isBefore(end)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "可预约开始时间必须早于结束时间");
        }
    }

    private static void validateDurationRange(Integer minDuration, Integer maxDuration) {
        if (minDuration != null && minDuration < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "单次最小预约时长不能为负数");
        }
        if (maxDuration != null && maxDuration <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "单次最大预约时长必须大于0");
        }
        if (minDuration != null && maxDuration != null && minDuration > maxDuration) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "单次最小预约时长不能大于最大预约时长");
        }
    }

    private static void validateAdvanceDays(Integer advanceDays) {
        if (advanceDays != null && advanceDays < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "提前预约天数不能为负数");
        }
    }

    private static LocalTime parseBookableTime(String value, String fieldName) {
        try {
            return LocalTime.parse(value, BOOKABLE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    fieldName + "格式不正确，应为 HH:mm");
        }
    }

    private static void requireConfigured(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), message);
        }
    }
}
