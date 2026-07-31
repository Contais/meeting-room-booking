package com.meetinghub.meeting.model.vo.tool;

import com.meetinghub.meeting.model.vo.ReservationBriefVO;

import java.time.LocalTime;
import java.util.List;

/**
 * 会议室域工具结果 VO 集合。
 * <p>
 * 以嵌套 record 形式聚合会议室相关结果类型，避免大量碎片化文件。
 * 所有结果均实现 {@link ToolResult}，由格式化器统一渲染。
 * </p>
 */
public final class RoomToolResults {

    private RoomToolResults() {
    }

    /**
     * 会议室简要信息（屏蔽 imageUrl、description、deleted 等内部字段）。
     *
     * @param name      名称
     * @param location  位置
     * @param capacity  容量
     * @param equipment 设备
     */
    public record RoomSummary(String name, String location, int capacity, String equipment) {
    }

    /**
     * 会议室列表结果：用于列表查询、推荐、歧义匹配等场景。
     *
     * @param title 列表标题（含尾部冒号），如「共 3 间会议室：」「匹配到多个会议室，请明确指定：」
     * @param rooms 会议室简要列表
     */
    public record RoomListResult(String title, List<RoomSummary> rooms) implements ToolResult {
    }

    /**
     * 某会议室某日预约列表。
     *
     * @param roomName     会议室名称
     * @param date         日期 yyyy-MM-dd
     * @param reservations 预约简要列表
     */
    public record RoomReservationResult(String roomName, String date,
                                        List<ReservationBriefVO> reservations) implements ToolResult {
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
     * 会议室预约统计结果。
     *
     * @param stats 各会议室统计项
     */
    public record RoomStatsResult(List<RoomStat> stats) implements ToolResult {
    }

    /**
     * 空闲时段：由起止时间构成的结构化时段。
     *
     * @param start 开始时间 HH:mm
     * @param end   结束时间 HH:mm
     */
    public record TimeSlot(LocalTime start, LocalTime end) {
    }

    /**
     * 空闲时段结果。
     *
     * @param roomName 会议室名称
     * @param date     日期 yyyy-MM-dd
     * @param slots    空闲时段列表
     */
    public record FreeSlotResult(String roomName, String date, List<TimeSlot> slots) implements ToolResult {
    }

    /**
     * 会议室推荐结果。
     *
     * @param date      日期 yyyy-MM-dd
     * @param startTime 开始时间 HH:mm（可为 null）
     * @param endTime   结束时间 HH:mm（可为 null）
     * @param rooms     符合条件的空闲会议室列表
     */
    public record RoomRecommendResult(String date, String startTime, String endTime,
                                      List<RoomSummary> rooms) implements ToolResult {
    }
}
