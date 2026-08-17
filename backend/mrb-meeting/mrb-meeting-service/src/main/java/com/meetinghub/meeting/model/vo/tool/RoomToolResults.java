package com.meetinghub.meeting.model.vo.tool;

import com.meetinghub.meeting.model.vo.tool.ReservationToolResults.ReservationBrief;

import java.util.List;

/**
 * 会议室域工具结果 VO 集合。
 * <p>
 * 以嵌套 record 形式聚合会议室相关结果类型，避免大量碎片化文件。
 * 工具方法直接返回这些 record 或 {@code List<X>}，由 Spring AI 序列化为 JSON。
 * </p>
 */
public final class RoomToolResults {

    private RoomToolResults() {
    }

    /**
     * 会议室简要信息（屏蔽 imageUrl、description、deleted 等内部字段）。
     *
     * @param name         名称
     * @param location     位置
     * @param capacity     容量
     * @param equipment    设备
     * @param needApproval 是否需要审批
     * @param advanceDays  提前预约天数
     * @param maxDuration  单次最大预约时长（分钟）
     * @param bookableStart 可预约开始时间 HH:mm
     * @param bookableEnd   可预约结束时间 HH:mm
     */
    public record RoomSummary(String name, String location, int capacity, String equipment,
                              boolean needApproval, int advanceDays, int maxDuration,
                              String bookableStart, String bookableEnd) {
    }

    /**
     * 某会议室某日预约列表。
     *
     * @param roomName     会议室名称
     * @param date         日期 yyyy-MM-dd
     * @param reservations 预约简要列表（时间已格式化为 String）
     */
    public record RoomReservationResult(String roomName, String date,
                                        List<ReservationBrief> reservations) implements ToolResult {
    }

    /**
     * 会议室预约统计项。
     *
     * @param name  会议室名称
     * @param count 当日预约数量
     */
    public record RoomStat(String name, long count) {
    }

    /**
     * 空闲时段：由起止时间构成的结构化时段。
     *
     * @param start 开始时间 HH:mm
     * @param end   结束时间 HH:mm
     */
    public record TimeSlot(String start, String end) {
    }

    /**
     * 空闲时段结果。
     *
     * @param roomName 会议室名称
     * @param date     日期 yyyy-MM-dd
     * @param slots    空闲时段列表（空列表表示无空闲）
     */
    public record FreeSlotResult(String roomName, String date, List<TimeSlot> slots) implements ToolResult {
    }

    /**
     * 会议室推荐结果。
     *
     * @param date      日期 yyyy-MM-dd
     * @param startTime 开始时间 HH:mm（可为 null）
     * @param endTime   结束时间 HH:mm（可为 null）
     * @param rooms     符合条件的空闲会议室列表（空列表表示无匹配）
     */
    public record RoomRecommendResult(String date, String startTime, String endTime,
                                      List<RoomSummary> rooms) implements ToolResult {
    }
}
