package com.meetinghub.meeting.model.vo.tool;

import com.meetinghub.common.constant.DateTimePatternConstant;
import com.meetinghub.meeting.api.enums.ReservationStatusEnum;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.model.vo.AttendeeVO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 预约域工具结果 VO 集合。
 * <p>
 * 以嵌套 record 形式聚合预约、参会人、部门相关结果类型，避免大量碎片化文件。
 * 工具方法直接返回这些 record 或 {@code List<X>}，由 Spring AI 序列化为 JSON。
 * </p>
 */
public final class ReservationToolResults {

    private ReservationToolResults() {
    }

    /**
     * 工具专用预约简要信息。
     * <p>
     * 时间字段已格式化为 String，状态转为中文描述，屏蔽 userId、roomId 等内部字段。
     * 与 {@link com.meetinghub.meeting.model.vo.ReservationBriefVO} 区别：
     * 后者同时服务 REST API（保留 LocalDateTime 类型），本 record 仅供 AI 工具 JSON 返回。
     * </p>
     *
     * @param reservationCode 预约编号（B 开头）
     * @param subject         会议主题
     * @param roomName        会议室名称
     * @param startTime       开始时间 yyyy-MM-dd HH:mm
     * @param endTime         结束时间 yyyy-MM-dd HH:mm
     * @param status          状态中文：已确认/待确认/已取消
     * @param attendeeCount   参会人数
     * @param remark          备注
     */
    public record ReservationBrief(
            String reservationCode,
            String subject,
            String roomName,
            String startTime,
            String endTime,
            String status,
            Integer attendeeCount,
            String remark
    ) {
    }

    /**
     * 工具专用参会人简要信息。
     * <p>
     * 屏蔽 userId、phone、email、departmentId 等敏感/内部字段，仅保留展示所需信息。
     * </p>
     *
     * @param name           姓名（优先 realName，回退 username）
     * @param departmentName 部门名称
     * @param status         查阅状态中文：待查阅/已查阅/已拒绝
     */
    public record AttendeeBrief(
            String name,
            String departmentName,
            String status
    ) {
    }

    /**
     * 预约历史结果（含统计汇总）。
     *
     * @param total        总记录数
     * @param confirmed    已确认数
     * @param pending      待确认数
     * @param cancelled    已取消数
     * @param reservations 预约简要列表（已截断为展示条数）
     */
    public record ReservationHistoryResult(long total, long confirmed, long pending, long cancelled,
                                           List<ReservationBrief> reservations) implements ToolResult {
    }

    /**
     * 操作结果：用于创建、取消、邀请等写操作。
     *
     * @param success 是否成功
     * @param message 结果描述
     */
    public record OperationResult(boolean success, String message) implements ToolResult {
    }

    /**
     * 部门简要信息。
     *
     * @param id   部门ID（供模型调用邀请工具使用，不应展示给用户）
     * @param name 部门名称
     */
    public record DepartmentBrief(Long id, String name) {
    }

    /**
     * 部门列表结果。
     *
     * @param departments 部门简要列表
     */
    public record DepartmentListResult(List<DepartmentBrief> departments) implements ToolResult {
    }

    /**
     * 参会人列表结果。
     *
     * @param attendees 参会人简要列表
     */
    public record AttendeeListResult(List<AttendeeBrief> attendees) implements ToolResult {
    }

    // ============================== 实体 → record 转换 ==============================

    /**
     * 将预约实体转换为工具专用简要 record（时间格式化为 String，状态转为中文）。
     *
     * @param r        预约实体
     * @param roomName 会议室名称（由调用方关联查询后回填），可为 null
     * @return 工具专用简要 record
     */
    public static ReservationBrief toBrief(MeetingRoomReservation r, String roomName) {
        return new ReservationBrief(
                r.getReservationCode(),
                r.getSubject(),
                roomName,
                formatDateTime(r.getStartTime()),
                formatDateTime(r.getEndTime()),
                ReservationStatusEnum.getDescByCode(r.getStatus()),
                r.getAttendeeCount(),
                r.getRemark()
        );
    }

    /**
     * 将预约实体列表转换为工具专用简要 record 列表。
     *
     * @param reservations 预约实体列表
     * @param roomNameMap  会议室ID -> 名称映射，用于回填 roomName；可为 null
     * @return 工具专用简要 record 列表
     */
    public static List<ReservationBrief> toBriefList(List<MeetingRoomReservation> reservations,
                                                     Map<Long, String> roomNameMap) {
        if (reservations == null) {
            return List.of();
        }
        return reservations.stream()
                .map(r -> toBrief(r, roomNameMap != null ? roomNameMap.get(r.getRoomId()) : null))
                .toList();
    }

    /**
     * 将参会人 VO 转换为工具专用简要 record（屏蔽 userId/phone/email 等敏感字段）。
     *
     * @param a 参会人 VO
     * @return 工具专用简要 record
     */
    public static AttendeeBrief toBrief(AttendeeVO a) {
        String name = (a.getRealName() != null && !a.getRealName().isBlank())
                ? a.getRealName() : a.getUsername();
        return new AttendeeBrief(
                name,
                a.getDepartmentName(),
                attendeeStatusText(a.getStatus())
        );
    }

    /**
     * 将参会人 VO 列表转换为工具专用简要 record 列表。
     *
     * @param attendees 参会人 VO 列表
     * @return 工具专用简要 record 列表
     */
    public static List<AttendeeBrief> toBriefList(List<AttendeeVO> attendees) {
        if (attendees == null) {
            return List.of();
        }
        return attendees.stream().map(ReservationToolResults::toBrief).toList();
    }

    private static String formatDateTime(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimePatternConstant.DATETIME_FMT) : null;
    }

    private static String attendeeStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待查阅";
            case 1 -> "已查阅";
            case 2 -> "已拒绝";
            default -> "未知";
        };
    }
}
