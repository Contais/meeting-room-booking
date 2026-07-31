package com.meetinghub.meeting.model.vo.tool;

import com.meetinghub.meeting.model.vo.AttendeeVO;
import com.meetinghub.meeting.model.vo.ReservationBriefVO;

import java.util.List;

/**
 * 预约域工具结果 VO 集合。
 * <p>
 * 以嵌套 record 形式聚合预约、参会人、部门相关结果类型，避免大量碎片化文件。
 * 所有结果均实现 {@link ToolResult}，由格式化器统一渲染。
 * </p>
 */
public final class ReservationToolResults {

    private ReservationToolResults() {
    }

    /**
     * 预约列表结果：用于「本人未结束预约」等列表展示。
     *
     * @param title        列表标题（含尾部冒号）
     * @param reservations 预约简要列表
     */
    public record ReservationListResult(String title, List<ReservationBriefVO> reservations) implements ToolResult {
    }

    /**
     * 预约历史结果（含统计汇总）。
     *
     * @param total        总记录数
     * @param confirmed    已确认数
     * @param pending      待确认数
     * @param cancelled    已取消数
     * @param reservations 展示的预约简要列表（已截断）
     * @param shown        实际展示条数
     */
    public record ReservationHistoryResult(long total, long confirmed, long pending, long cancelled,
                                           List<ReservationBriefVO> reservations, int shown) implements ToolResult {
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
     * @param id   部门ID
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
     * @param attendees 参会人视图列表
     */
    public record AttendeeListResult(List<AttendeeVO> attendees) implements ToolResult {
    }
}
