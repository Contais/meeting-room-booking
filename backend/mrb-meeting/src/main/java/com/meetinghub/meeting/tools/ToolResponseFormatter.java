package com.meetinghub.meeting.tools;

import com.meetinghub.meeting.model.entity.MeetingRoom;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.model.vo.ReservationBriefVO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 工具响应格式化器
 * <p>
 * 统一处理工具返回值的字符串格式化，避免各工具自行拼接导致风格不一致。
 * 设计原则：
 * 1. 输出简洁、面向用户，屏蔽内部字段（id、deleted、userId 等）
 * 2. 列表型结果使用「标题 + 列表项」结构，单条信息一行
 * 3. 时间格式化遵循「yyyy-MM-dd HH:mm」
 * </p>
 */
public final class ToolResponseFormatter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ToolResponseFormatter() {}

    /**
     * 格式化预约列表为字符串
     *
     * @param reservations 预约列表
     * @param roomNameMap  会议室ID -> 名称映射，用于回填 roomName；可为 null
     */
    public static String formatReservations(List<MeetingRoomReservation> reservations, Map<Long, String> roomNameMap) {
        if (reservations == null || reservations.isEmpty()) {
            return "暂无预约记录";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("共 %d 条预约记录：\n", reservations.size()));
        for (MeetingRoomReservation r : reservations) {
            String roomName = roomNameMap != null ? roomNameMap.getOrDefault(r.getRoomId(), "未知会议室") : "未知会议室";
            sb.append(String.format("- %s %s~%s %s（%s，%s）\n",
                    r.getStartTime().format(DATE_FMT),
                    r.getStartTime().format(TIME_FMT),
                    r.getEndTime().format(TIME_FMT),
                    r.getSubject() != null ? r.getSubject() : "未命名",
                    roomName,
                    statusText(r.getStatus())));
        }
        return sb.toString();
    }

    /**
     * 格式化预约简要信息列表为字符串
     */
    public static String formatBriefReservations(List<ReservationBriefVO> reservations) {
        if (reservations == null || reservations.isEmpty()) {
            return "暂无预约记录";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("共 %d 条预约记录：\n", reservations.size()));
        for (ReservationBriefVO r : reservations) {
            sb.append(String.format("- %s %s~%s %s（%s，%s）\n",
                    r.getStartTime().format(DATE_FMT),
                    r.getStartTime().format(TIME_FMT),
                    r.getEndTime().format(TIME_FMT),
                    r.getSubject() != null ? r.getSubject() : "未命名",
                    r.getRoomName() != null ? r.getRoomName() : "未知会议室",
                    statusText(r.getStatus())));
        }
        return sb.toString();
    }

    /**
     * 将预约实体转换为简要 VO
     */
    public static ReservationBriefVO toBriefVO(MeetingRoomReservation r, String roomName) {
        ReservationBriefVO vo = new ReservationBriefVO();
        vo.setReservationCode(r.getReservationCode());
        vo.setSubject(r.getSubject());
        vo.setRoomName(roomName);
        vo.setStartTime(r.getStartTime());
        vo.setEndTime(r.getEndTime());
        vo.setStatus(r.getStatus());
        vo.setAttendeeCount(r.getAttendeeCount());
        vo.setContactPhone(r.getContactPhone());
        vo.setRemark(r.getRemark());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }

    /**
     * 将预约实体列表转换为简要 VO 列表
     */
    public static List<ReservationBriefVO> toBriefVOList(List<MeetingRoomReservation> reservations, Map<Long, String> roomNameMap) {
        if (reservations == null) return List.of();
        return reservations.stream()
                .map(r -> toBriefVO(r, roomNameMap != null ? roomNameMap.get(r.getRoomId()) : null))
                .collect(Collectors.toList());
    }

    /**
     * 格式化会议室列表为字符串
     */
    public static String formatRooms(List<MeetingRoom> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return "暂无可用会议室";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("共 %d 间会议室：\n", rooms.size()));
        for (MeetingRoom r : rooms) {
            sb.append(String.format("- %s（%s，容纳%d人，设备：%s）\n",
                    r.getName(),
                    r.getLocation() != null ? r.getLocation() : "未设置位置",
                    r.getCapacity(),
                    r.getEquipment() != null ? r.getEquipment() : "无"));
        }
        return sb.toString();
    }

    /**
     * 预约状态文案
     */
    public static String statusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待确认";
            case 1: return "已确认";
            case 2: return "已取消";
            default: return "未知";
        }
    }

    /**
     * 格式化日期时间
     */
    public static String formatDateTime(LocalDateTime dt) {
        return dt != null ? dt.format(DATETIME_FMT) : "-";
    }

    /**
     * 格式化日期
     */
    public static String formatDate(LocalDateTime dt) {
        return dt != null ? dt.format(DATE_FMT) : "-";
    }

    /**
     * 格式化时间
     */
    public static String formatTime(LocalDateTime dt) {
        return dt != null ? dt.format(TIME_FMT) : "-";
    }
}
