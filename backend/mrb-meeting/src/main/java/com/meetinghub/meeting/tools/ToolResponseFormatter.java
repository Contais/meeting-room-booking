package com.meetinghub.meeting.tools;

import com.meetinghub.common.enums.ReservationStatusEnum;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.model.vo.AttendeeVO;
import com.meetinghub.meeting.model.vo.ReservationBriefVO;
import com.meetinghub.meeting.model.vo.tool.ReservationToolResults.AttendeeListResult;
import com.meetinghub.meeting.model.vo.tool.ReservationToolResults.DepartmentBrief;
import com.meetinghub.meeting.model.vo.tool.ReservationToolResults.DepartmentListResult;
import com.meetinghub.meeting.model.vo.tool.ReservationToolResults.OperationResult;
import com.meetinghub.meeting.model.vo.tool.ReservationToolResults.ReservationHistoryResult;
import com.meetinghub.meeting.model.vo.tool.ReservationToolResults.ReservationListResult;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.FreeSlotResult;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.RoomListResult;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.RoomRecommendResult;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.RoomReservationResult;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.RoomStat;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.RoomStatsResult;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.RoomSummary;
import com.meetinghub.meeting.model.vo.tool.ToolResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 工具响应格式化器。
 * <p>
 * 作为工具返回值字符串化的「专门入口」：工具方法仅负责查询数据并组装 {@link ToolResult}，
 * 再交由本类 {@link #format(ToolResult)} 统一渲染为面向用户的字符串。
 * </p>
 * <p>
 * 设计原则：
 * 1. 输出简洁、面向用户，屏蔽内部字段（id、deleted、userId 等）；
 * 2. 列表型结果使用「标题 + 列表项」结构，单条信息一行；
 * 3. 时间格式化遵循「yyyy-MM-dd HH:mm」；
 * 4. 状态文案统一引用 {@link ReservationStatusEnum}，避免散落硬编码。
 * </p>
 */
public final class ToolResponseFormatter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private ToolResponseFormatter() {
    }

    // ============================== 统一格式化入口 ==============================

    /**
     * 将工具结果 VO 统一格式化为字符串。
     *
     * @param result 工具结果
     * @return 面向用户的字符串
     */
    public static String format(ToolResult result) {
        if (result == null) {
            return "";
        }
        if (result instanceof ToolResult.TextResult t) {
            return t.message();
        }
        if (result instanceof RoomListResult r) {
            return formatRoomList(r);
        }
        if (result instanceof RoomRecommendResult r) {
            return formatRoomRecommend(r);
        }
        if (result instanceof RoomReservationResult r) {
            return formatRoomReservations(r);
        }
        if (result instanceof RoomStatsResult r) {
            return formatRoomStats(r);
        }
        if (result instanceof FreeSlotResult r) {
            return formatFreeSlots(r);
        }
        if (result instanceof ReservationListResult r) {
            return formatReservationList(r);
        }
        if (result instanceof ReservationHistoryResult r) {
            return formatReservationHistory(r);
        }
        if (result instanceof OperationResult o) {
            return o.message();
        }
        if (result instanceof DepartmentListResult d) {
            return formatDepartmentList(d);
        }
        if (result instanceof AttendeeListResult a) {
            return formatAttendeeList(a);
        }
        return result.toString();
    }

    // ============================== 实体 → VO 转换 ==============================

    /**
     * 将预约实体转换为简要 VO（屏蔽 userId、roomId 等内部字段）。
     *
     * @param r        预约实体
     * @param roomName 会议室名称（由调用方关联查询后回填），可为 null
     * @return 简要 VO
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
        vo.setRemark(r.getRemark());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }

    /**
     * 将预约实体列表转换为简要 VO 列表。
     *
     * @param reservations 预约实体列表
     * @param roomNameMap  会议室ID -> 名称映射，用于回填 roomName；可为 null
     * @return 简要 VO 列表
     */
    public static List<ReservationBriefVO> toBriefVOList(List<MeetingRoomReservation> reservations,
                                                         Map<Long, String> roomNameMap) {
        if (reservations == null) {
            return List.of();
        }
        return reservations.stream()
                .map(r -> toBriefVO(r, roomNameMap != null ? roomNameMap.get(r.getRoomId()) : null))
                .collect(Collectors.toList());
    }

    // ============================== 会议室域渲染 ==============================

    private static String formatRoomList(RoomListResult r) {
        StringBuilder sb = new StringBuilder(r.title()).append('\n');
        for (RoomSummary room : r.rooms()) {
            sb.append(formatRoomSummary(room)).append('\n');
        }
        return sb.toString();
    }

    private static String formatRoomRecommend(RoomRecommendResult r) {
        StringBuilder sb = new StringBuilder("推荐会议室：\n");
        for (RoomSummary room : r.rooms()) {
            sb.append(formatRoomSummary(room)).append('\n');
        }
        return sb.toString();
    }

    private static String formatRoomSummary(RoomSummary r) {
        return String.format("- %s（%s，容纳%d人，设备：%s）",
                r.name(),
                r.location() != null ? r.location() : "未设置位置",
                r.capacity(),
                r.equipment() != null ? r.equipment() : "无");
    }

    private static String formatRoomReservations(RoomReservationResult r) {
        return String.format("%s 在 %s 的预约情况：\n", r.roomName(), r.date())
                + formatBriefList(r.reservations());
    }

    private static String formatRoomStats(RoomStatsResult r) {
        StringBuilder sb = new StringBuilder("今日会议室预约统计：\n");
        for (RoomStat s : r.stats()) {
            sb.append(String.format("- %s：%d个预约\n", s.name(), s.count()));
        }
        return sb.toString();
    }

    private static String formatFreeSlots(FreeSlotResult r) {
        String slots = r.slots().stream()
                .map(s -> s.start().format(TIME_FMT) + "~" + s.end().format(TIME_FMT))
                .collect(Collectors.joining("、"));
        return String.format("%s 在 %s 的空闲时段：%s", r.roomName(), r.date(), slots);
    }

    // ============================== 预约域渲染 ==============================

    private static String formatReservationList(ReservationListResult r) {
        return r.title() + '\n' + formatBriefList(r.reservations());
    }

    private static String formatReservationHistory(ReservationHistoryResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("共 %d 条预约记录（已确认 %d、待确认 %d、已取消 %d）：\n",
                r.total(), r.confirmed(), r.pending(), r.cancelled()));
        for (ReservationBriefVO item : r.reservations()) {
            sb.append(formatHistoryItem(item)).append('\n');
        }
        if (r.total() > r.shown()) {
            sb.append(String.format("（仅展示前 %d 条，共 %d 条）\n", r.shown(), r.total()));
        }
        return sb.toString();
    }

    private static String formatDepartmentList(DepartmentListResult d) {
        StringBuilder sb = new StringBuilder("部门列表：\n");
        for (DepartmentBrief dept : d.departments()) {
            sb.append(String.format("- [%d] %s\n", dept.id(), dept.name()));
        }
        return sb.toString();
    }

    private static String formatAttendeeList(AttendeeListResult a) {
        StringBuilder sb = new StringBuilder("参会人列表：\n");
        for (AttendeeVO p : a.attendees()) {
            String name = (p.getRealName() != null && !p.getRealName().isBlank())
                    ? p.getRealName() : p.getUsername();
            sb.append(String.format("- %s（%s） %s\n",
                    name,
                    p.getDepartmentName() != null ? p.getDepartmentName() : "未分配部门",
                    attendeeStatusText(p.getStatus())));
        }
        return sb.toString();
    }

    // ============================== 私有渲染助手 ==============================

    /**
     * 渲染预约简要列表：「共 N 条预约记录：\n - 项...」。
     */
    private static String formatBriefList(List<ReservationBriefVO> reservations) {
        if (reservations == null || reservations.isEmpty()) {
            return "暂无预约记录";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("共 %d 条预约记录：\n", reservations.size()));
        for (ReservationBriefVO r : reservations) {
            sb.append(formatBriefItem(r)).append('\n');
        }
        return sb.toString();
    }

    private static String formatBriefItem(ReservationBriefVO r) {
        return String.format("- %s %s~%s %s（%s，%s）",
                r.getStartTime().format(DATE_FMT),
                r.getStartTime().format(TIME_FMT),
                r.getEndTime().format(TIME_FMT),
                r.getSubject() != null ? r.getSubject() : "未命名",
                r.getRoomName() != null ? r.getRoomName() : "未知会议室",
                statusText(r.getStatus()));
    }

    private static String formatHistoryItem(ReservationBriefVO r) {
        return String.format("- %s %s~%s %s（%s）",
                r.getStartTime().format(DATE_FMT),
                r.getStartTime().format(TIME_FMT),
                r.getEndTime().format(TIME_FMT),
                r.getSubject() != null ? r.getSubject() : "未命名",
                statusText(r.getStatus()));
    }

    private static String statusText(Integer status) {
        return ReservationStatusEnum.getDescByCode(status);
    }

    private static String attendeeStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "待查阅";
            case 1:
                return "已查阅";
            case 2:
                return "已拒绝";
            default:
                return "未知";
        }
    }

    /**
     * 格式化日期时间（供工具方法拼装操作结果文案使用）。
     *
     * @param dt 日期时间
     * @return yyyy-MM-dd HH:mm
     */
    public static String formatDateTime(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-";
    }
}
