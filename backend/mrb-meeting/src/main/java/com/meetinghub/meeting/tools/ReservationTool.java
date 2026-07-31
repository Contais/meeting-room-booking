package com.meetinghub.meeting.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meetinghub.common.enums.ReservationStatusEnum;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.meeting.feign.DepartmentFeignClient;
import com.meetinghub.meeting.model.dto.ReservationCreateDTO;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.model.vo.ReservationBriefVO;
import com.meetinghub.meeting.model.vo.tool.ReservationToolResults.AttendeeListResult;
import com.meetinghub.meeting.model.vo.tool.ReservationToolResults.DepartmentBrief;
import com.meetinghub.meeting.model.vo.tool.ReservationToolResults.DepartmentListResult;
import com.meetinghub.meeting.model.vo.tool.ReservationToolResults.OperationResult;
import com.meetinghub.meeting.model.vo.tool.ReservationToolResults.ReservationHistoryResult;
import com.meetinghub.meeting.model.vo.tool.ReservationToolResults.ReservationListResult;
import com.meetinghub.meeting.model.vo.tool.ToolResult;
import com.meetinghub.meeting.repository.ReservationRepository;
import com.meetinghub.meeting.service.ReservationAttendeeService;
import com.meetinghub.meeting.service.ReservationService;
import com.meetinghub.meeting.tools.RoomResolver.RoomMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 聊天助手工具类 - 预约域。
 * <p>
 * 提供预约、参会人、部门相关能力：创建/取消预约、查询本人未结束/历史预约、
 * 查询部门列表、按部门邀请参会人、查询参会人列表。会议室查询能力见 {@link MeetingRoomTool}。
 * </p>
 * <p>
 * 方法体统一遵循「查询 → 组装 {@link ToolResult} → 交由 {@link ToolResponseFormatter} 格式化」的模式，
 * 不在工具内直接拼接展示字符串。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationTool {

    private final ReservationService reservationService;
    private final ReservationAttendeeService attendeeService;
    private final ReservationRepository reservationRepository;
    private final RoomResolver roomResolver;
    private final DepartmentFeignClient departmentFeignClient;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    /** 历史预约最多展示条数，避免回复过长 */
    private static final int HISTORY_DISPLAY_LIMIT = 10;

    @Tool(description = "创建会议室预约。传入会议室名称（支持模糊匹配，但需能唯一确定）、日期、开始/结束时间、会议主题。创建后可在详情页添加参会人员。返回预约结果。")
    public String createReservation(
            ToolContext toolContext,
            @ToolParam(description = "会议室名称") String roomName,
            @ToolParam(description = "预约日期，格式 yyyy-MM-dd") String date,
            @ToolParam(description = "开始时间，格式 HH:mm") String startTime,
            @ToolParam(description = "结束时间，格式 HH:mm") String endTime,
            @ToolParam(description = "会议主题") String subject) {
        Long userId = ToolAuthHelper.requireUserId(toolContext);

        if (roomName == null || roomName.isBlank()) {
            return ToolResponseFormatter.format(new ToolResult.TextResult("请提供会议室名称"));
        }
        if (date == null || startTime == null || endTime == null) {
            return ToolResponseFormatter.format(new ToolResult.TextResult("请提供日期和开始/结束时间"));
        }

        RoomMatch match = roomResolver.resolveByName(roomName);
        if (!(match instanceof RoomMatch.Single s)) {
            return ToolResponseFormatter.format(RoomResolver.toErrorResult(match));
        }
        MeetingRoom room = s.room();

        LocalDateTime start;
        LocalDateTime end;
        try {
            LocalDate d = LocalDate.parse(date, DATE_FMT);
            LocalTime st = LocalTime.parse(startTime, TIME_FMT);
            LocalTime et = LocalTime.parse(endTime, TIME_FMT);
            start = LocalDateTime.of(d, st);
            end = LocalDateTime.of(d, et);
        } catch (Exception e) {
            return ToolResponseFormatter.format(new ToolResult.TextResult(
                    "时间格式有误，日期请用 yyyy-MM-dd，时间请用 HH:mm"));
        }

        ReservationCreateDTO dto = new ReservationCreateDTO();
        dto.setRoomId(room.getId());
        dto.setSubject(subject);
        dto.setStartTime(start);
        dto.setEndTime(end);

        reservationService.createReservation(userId, dto);
        return ToolResponseFormatter.format(new OperationResult(true,
                String.format("预约创建成功：%s %s ~ %s「%s」",
                        room.getName(), ToolResponseFormatter.formatDateTime(start),
                        ToolResponseFormatter.formatDateTime(end), subject)));
    }

    @Tool(description = "取消本人的会议室预约，传入预约记录ID。仅可取消本人创建的预约，无法取消他人的预约。")
    public String cancelMyReservation(
            ToolContext toolContext,
            @ToolParam(description = "预约记录ID") Long reservationId) {
        Long userId = ToolAuthHelper.requireUserId(toolContext);

        MeetingRoomReservation reservation = reservationRepository.selectById(reservationId);
        if (reservation == null) {
            return ToolResponseFormatter.format(new ToolResult.TextResult("预约记录不存在"));
        }
        if (!reservation.getUserId().equals(userId)) {
            return ToolResponseFormatter.format(new ToolResult.TextResult("无权取消他人的预约"));
        }
        reservationService.cancelReservation(userId, reservationId);
        return ToolResponseFormatter.format(new OperationResult(true, "预约 " + reservationId + " 已取消"));
    }

    @Tool(description = "查看本人未结束的预约，返回会议室名称、日期、时段、预约编号、主题、状态")
    public String listMyUpcomingReservations(ToolContext toolContext) {
        Long userId = ToolAuthHelper.requireUserId(toolContext);
        LocalDateTime now = LocalDateTime.now();
        List<MeetingRoomReservation> reservations = reservationRepository.selectList(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .eq(MeetingRoomReservation::getUserId, userId)
                        .notIn(MeetingRoomReservation::getStatus, ReservationStatusEnum.EXCLUDED_CODES)
                        .ge(MeetingRoomReservation::getEndTime, now)
                        .orderByAsc(MeetingRoomReservation::getStartTime)
        );
        if (reservations.isEmpty()) {
            return ToolResponseFormatter.format(new ToolResult.TextResult("您当前没有未结束的预约"));
        }
        // 批量查询会议室名称
        List<Long> roomIds = reservations.stream().map(MeetingRoomReservation::getRoomId).distinct().collect(Collectors.toList());
        Map<Long, String> roomNameMap = roomResolver.batchNames(roomIds);
        List<ReservationBriefVO> briefs = ToolResponseFormatter.toBriefVOList(reservations, roomNameMap);
        return ToolResponseFormatter.format(new ReservationListResult("您未结束的预约：", briefs));
    }

    @Tool(description = "查询本人历史预约记录（含已完成/已取消），支持按日期范围筛选。返回统计与列表。")
    public String listMyReservationHistory(
            ToolContext toolContext,
            @ToolParam(description = "开始日期，格式 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(description = "结束日期，格式 yyyy-MM-dd", required = false) String endDate) {
        Long userId = ToolAuthHelper.requireUserId(toolContext);

        LambdaQueryWrapper<MeetingRoomReservation> wrapper = new LambdaQueryWrapper<MeetingRoomReservation>()
                .eq(MeetingRoomReservation::getUserId, userId)
                .orderByDesc(MeetingRoomReservation::getStartTime);
        LocalDateTime rangeStart = parseDateStart(startDate);
        LocalDateTime rangeEnd = parseDateEnd(endDate);
        if (rangeStart != null) {
            wrapper.ge(MeetingRoomReservation::getStartTime, rangeStart);
        }
        if (rangeEnd != null) {
            wrapper.le(MeetingRoomReservation::getStartTime, rangeEnd);
        }

        List<MeetingRoomReservation> list = reservationRepository.selectList(wrapper);
        if (list.isEmpty()) {
            return ToolResponseFormatter.format(new ToolResult.TextResult("您在指定时间段内没有预约记录"));
        }

        long total = list.size();
        long confirmed = list.stream().filter(r -> r.getStatus().equals(ReservationStatusEnum.CONFIRMED.getCode())).count();
        long cancelled = list.stream().filter(r -> r.getStatus().equals(ReservationStatusEnum.CANCELLED.getCode())).count();
        long pending = list.stream().filter(r -> r.getStatus().equals(ReservationStatusEnum.PENDING.getCode())).count();

        // 仅展示前 N 条，避免回复过长；历史记录不回填会议室名称
        List<MeetingRoomReservation> showList = list.stream().limit(HISTORY_DISPLAY_LIMIT).collect(Collectors.toList());
        List<ReservationBriefVO> briefs = ToolResponseFormatter.toBriefVOList(showList, null);
        return ToolResponseFormatter.format(new ReservationHistoryResult(
                total, confirmed, pending, cancelled, briefs, showList.size()));
    }

    @Tool(description = "查询所有部门列表（用于邀请参会人时按部门选择）。返回部门ID和名称。")
    public String listDepartments() {
        try {
            var result = departmentFeignClient.listFlat();
            if (result == null || result.getData() == null || result.getData().isEmpty()) {
                return ToolResponseFormatter.format(new ToolResult.TextResult("当前系统未配置部门"));
            }
            List<DepartmentBrief> departments = result.getData().stream()
                    .map(d -> new DepartmentBrief(d.getId(), d.getName()))
                    .collect(Collectors.toList());
            return ToolResponseFormatter.format(new DepartmentListResult(departments));
        } catch (Exception e) {
            log.error("listDepartments 调用失败", e);
            return ToolResponseFormatter.format(new ToolResult.TextResult("查询部门列表失败，请稍后重试"));
        }
    }

    @Tool(description = "按部门邀请参会人。传入预约ID和部门ID，将该部门所有成员加入参会人列表。仅预约创建者可操作。")
    public String inviteDepartmentAttendees(
            ToolContext toolContext,
            @ToolParam(description = "预约记录ID") Long reservationId,
            @ToolParam(description = "部门ID") Long departmentId) {
        Long userId = ToolAuthHelper.requireUserId(toolContext);
        if (reservationId == null || departmentId == null) {
            return ToolResponseFormatter.format(new ToolResult.TextResult("请提供预约ID和部门ID"));
        }
        try {
            int count = attendeeService.inviteDepartment(reservationId, userId, departmentId);
            return ToolResponseFormatter.format(new OperationResult(true,
                    String.format("已成功邀请 %d 位部门成员加入预约 %d", count, reservationId)));
        } catch (BusinessException e) {
            return ToolResponseFormatter.format(new OperationResult(false, "邀请失败：" + e.getMessage()));
        } catch (Exception e) {
            log.error("inviteDepartmentAttendees 调用失败, reservationId={}, departmentId={}",
                    reservationId, departmentId, e);
            return ToolResponseFormatter.format(new OperationResult(false, "邀请参会人失败，请稍后重试"));
        }
    }

    @Tool(description = "查询某预约的参会人列表。返回参会人姓名、部门、参会状态。")
    public String listReservationAttendees(
            @ToolParam(description = "预约记录ID") Long reservationId) {
        if (reservationId == null) {
            return ToolResponseFormatter.format(new ToolResult.TextResult("请提供预约ID"));
        }
        var attendees = attendeeService.listAttendees(reservationId);
        if (attendees.isEmpty()) {
            return ToolResponseFormatter.format(new ToolResult.TextResult("该预约暂无参会人"));
        }
        return ToolResponseFormatter.format(new AttendeeListResult(attendees));
    }

    /**
     * 解析开始日期为当日 00:00，格式非法返回 null。
     */
    private LocalDateTime parseDateStart(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date, DATE_FMT).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析结束日期为当日 23:59:59.999，格式非法返回 null。
     */
    private LocalDateTime parseDateEnd(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date, DATE_FMT).atTime(LocalTime.MAX);
        } catch (Exception e) {
            return null;
        }
    }
}
