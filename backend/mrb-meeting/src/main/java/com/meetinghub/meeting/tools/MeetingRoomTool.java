package com.meetinghub.meeting.tools;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meetinghub.common.enums.EnableStatusEnum;
import com.meetinghub.common.enums.ReservationStatusEnum;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.meeting.feign.DepartmentFeignClient;
import com.meetinghub.meeting.feign.dto.DepartmentBriefDTO;
import com.meetinghub.meeting.model.dto.ReservationCreateDTO;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.repository.MeetingRoomRepository;
import com.meetinghub.meeting.repository.ReservationRepository;
import com.meetinghub.meeting.service.ReservationAttendeeService;
import com.meetinghub.meeting.service.ReservationService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 聊天助手工具类 - 提供会议室查询、预约、参会人邀请等能力
 * <p>
 * 一期已支持：会议室列表、当日预约查询、统计、创建/取消预约、查询本人未结束预约
 * 二期新增：会议室推荐、空闲时段查询、本人历史预约、按部门邀请参会人、查询参会人列表
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingRoomTool {

    private final ReservationService reservationService;
    private final ReservationAttendeeService attendeeService;
    private final MeetingRoomRepository meetingRoomRepository;
    private final ReservationRepository reservationRepository;
    private final DepartmentFeignClient departmentFeignClient;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Tool(description = "查询所有可用的会议室列表，返回名称、位置、容量、设备信息")
    public String listAvailableRooms() {
        List<MeetingRoom> rooms = meetingRoomRepository.selectList(
                new LambdaQueryWrapper<MeetingRoom>().eq(MeetingRoom::getStatus, EnableStatusEnum.ENABLED.getCode())
        );
        if (rooms.isEmpty()) return "当前没有可用的会议室";
        return ToolResponseFormatter.formatRooms(rooms);
    }

    @Tool(description = "查询指定日期某个会议室的预约情况")
    public String queryRoomReservationsNew(
            @ToolParam(description = "会议室名称，支持模糊匹配") String roomName,
            @ToolParam(description = "日期，格式 yyyy-MM-dd") String date) {
        List<MeetingRoom> rooms = meetingRoomRepository.selectList(
                new LambdaQueryWrapper<MeetingRoom>().like(MeetingRoom::getName, roomName).eq(MeetingRoom::getStatus, EnableStatusEnum.ENABLED.getCode())
        );
        if (CollUtil.isEmpty(rooms)) {
            return "未找到名为「" + roomName + "」的可用会议室";
        }
        if (rooms.size() > 1) {
            StringBuilder sb = new StringBuilder("匹配到多个会议室，请明确指定：\n");
            for (MeetingRoom r : rooms) {
                sb.append(String.format("- %s（%s）\n", r.getName(), r.getLocation()));
            }
            return sb.toString();
        }
        MeetingRoom room = rooms.get(0);
        LocalDate d;
        try {
            d = LocalDate.parse(date, DATE_FMT);
        } catch (Exception e) {
            return "日期格式有误，请用 yyyy-MM-dd";
        }
        List<MeetingRoomReservation> reservations = reservationRepository.selectList(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .eq(MeetingRoomReservation::getRoomId, room.getId())
                        .notIn(MeetingRoomReservation::getStatus, ReservationStatusEnum.EXCLUDED_CODES)
                        .between(MeetingRoomReservation::getStartTime, d.atStartOfDay(), d.atTime(LocalTime.MAX))
                        .orderByAsc(MeetingRoomReservation::getStartTime)
        );
        if (reservations.isEmpty()) {
            return String.format("%s 在 %s 没有预约", room.getName(), date);
        }
        Map<Long, String> roomNameMap = Map.of(room.getId(), room.getName());
        return String.format("%s 在 %s 的预约情况：\n", room.getName(), date)
                + ToolResponseFormatter.formatReservations(reservations, roomNameMap);
    }

    @Tool(description = "查询所有会议室今天的整体预约统计，返回每个会议室的预约数量")
    public String todayReservationStats() {
        LocalDate today = LocalDate.now();
        List<MeetingRoom> rooms = meetingRoomRepository.selectList(
                new LambdaQueryWrapper<MeetingRoom>().eq(MeetingRoom::getStatus, EnableStatusEnum.ENABLED.getCode())
        );
        StringBuilder sb = new StringBuilder("今日会议室预约统计：\n");
        for (MeetingRoom r : rooms) {
            long count = reservationRepository.selectCount(
                    new LambdaQueryWrapper<MeetingRoomReservation>()
                            .eq(MeetingRoomReservation::getRoomId, r.getId())
                            .notIn(MeetingRoomReservation::getStatus, ReservationStatusEnum.EXCLUDED_CODES)
                            .between(MeetingRoomReservation::getStartTime, today.atStartOfDay(), today.atTime(LocalTime.MAX))
            );
            sb.append(String.format("- %s：%d个预约\n", r.getName(), count));
        }
        return sb.toString();
    }

    @Tool(description = "创建会议室预约。传入会议室名称（支持模糊匹配，但需能唯一确定）、日期、开始/结束时间、会议主题。创建后可在详情页添加参会人员。返回预约结果。")
    public String createReservation(
            ToolContext toolContext,
            @ToolParam(description = "会议室名称") String roomName,
            @ToolParam(description = "预约日期，格式 yyyy-MM-dd") String date,
            @ToolParam(description = "开始时间，格式 HH:mm") String startTime,
            @ToolParam(description = "结束时间，格式 HH:mm") String endTime,
            @ToolParam(description = "会议主题") String subject) {
        Long userId = ToolAuthHelper.requireUserId(toolContext);

        if (roomName == null || roomName.isBlank()) return "请提供会议室名称";
        if (date == null || startTime == null || endTime == null) return "请提供日期和开始/结束时间";

        List<MeetingRoom> rooms = meetingRoomRepository.selectList(
                new LambdaQueryWrapper<MeetingRoom>()
                        .like(MeetingRoom::getName, roomName)
                        .eq(MeetingRoom::getStatus, EnableStatusEnum.ENABLED.getCode())
        );
        if (rooms.isEmpty()) return "未找到名为「" + roomName + "」的可用会议室";
        if (rooms.size() > 1) {
            StringBuilder sb = new StringBuilder("匹配到多个会议室，请明确指定：\n");
            for (MeetingRoom r : rooms) {
                sb.append(String.format("- %s（%s）\n", r.getName(), r.getLocation()));
            }
            return sb.toString();
        }

        MeetingRoom room = rooms.get(0);

        LocalDateTime start;
        LocalDateTime end;
        try {
            LocalDate d = LocalDate.parse(date, DATE_FMT);
            LocalTime st = LocalTime.parse(startTime, TIME_FMT);
            LocalTime et = LocalTime.parse(endTime, TIME_FMT);
            start = LocalDateTime.of(d, st);
            end = LocalDateTime.of(d, et);
        } catch (Exception e) {
            return "时间格式有误，日期请用 yyyy-MM-dd，时间请用 HH:mm";
        }

        ReservationCreateDTO dto = new ReservationCreateDTO();
        dto.setRoomId(room.getId());
        dto.setSubject(subject);
        dto.setStartTime(start);
        dto.setEndTime(end);

        reservationService.createReservation(userId, dto);
        return String.format("预约创建成功：%s %s ~ %s「%s」",
                room.getName(), start.format(DATETIME_FMT), end.format(DATETIME_FMT), subject);
    }

    @Tool(description = "取消本人的会议室预约，传入预约记录ID。仅可取消本人创建的预约，无法取消他人的预约。")
    public String cancelMyReservation(
            ToolContext toolContext,
            @ToolParam(description = "预约记录ID") Long reservationId) {
        Long userId = ToolAuthHelper.requireUserId(toolContext);

        MeetingRoomReservation reservation = reservationRepository.selectById(reservationId);
        if (reservation == null) {
            return "预约记录不存在";
        }
        if (!reservation.getUserId().equals(userId)) {
            return "无权取消他人的预约";
        }
        reservationService.cancelReservation(userId, reservationId);
        return "预约 " + reservationId + " 已取消";
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
            return "您当前没有未结束的预约";
        }
        // 批量查询会议室名称
        List<Long> roomIds = reservations.stream().map(MeetingRoomReservation::getRoomId).distinct().collect(Collectors.toList());
        Map<Long, String> roomNameMap = batchRoomNames(roomIds);
        return "您未结束的预约：\n" + ToolResponseFormatter.formatReservations(reservations, roomNameMap);
    }

    /**
     * 批量查询会议室名称
     */
    private Map<Long, String> batchRoomNames(List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) return Map.of();
        List<MeetingRoom> rooms = meetingRoomRepository.selectBatchIds(roomIds);
        return rooms.stream().collect(Collectors.toMap(MeetingRoom::getId, MeetingRoom::getName, (a, b) -> a));
    }

    // ============== 二期新工具 ==============

    @Tool(description = "根据需求推荐可用会议室。传入日期、可选时段、人数、设备关键词，返回符合条件的空闲会议室列表。")
    public String recommendRoom(
            @ToolParam(description = "日期，格式 yyyy-MM-dd") String date,
            @ToolParam(description = "开始时间，格式 HH:mm", required = false) String startTime,
            @ToolParam(description = "结束时间，格式 HH:mm", required = false) String endTime,
            @ToolParam(description = "参会人数", required = false) Integer capacity,
            @ToolParam(description = "设备关键词，如 投影仪/白板", required = false) String equipment) {
        if (date == null) return "请提供日期";

        LocalDate d;
        try {
            d = LocalDate.parse(date, DATE_FMT);
        } catch (Exception e) {
            return "日期格式有误，请用 yyyy-MM-dd";
        }

        // 1. 按启用状态、容量、设备关键词筛选
        LambdaQueryWrapper<MeetingRoom> wrapper = new LambdaQueryWrapper<MeetingRoom>()
                .eq(MeetingRoom::getStatus, EnableStatusEnum.ENABLED.getCode());
        if (capacity != null && capacity > 0) {
            wrapper.ge(MeetingRoom::getCapacity, capacity);
        }
        if (equipment != null && !equipment.isBlank()) {
            wrapper.like(MeetingRoom::getEquipment, equipment);
        }
        List<MeetingRoom> rooms = meetingRoomRepository.selectList(wrapper);
        if (rooms.isEmpty()) return "未找到符合条件的会议室";

        // 2. 如果有时段，进一步过滤掉冲突的会议室
        LocalDateTime rangeStart = null;
        LocalDateTime rangeEnd = null;
        if (startTime != null && endTime != null && !startTime.isBlank() && !endTime.isBlank()) {
            try {
                rangeStart = LocalDateTime.of(d, LocalTime.parse(startTime, TIME_FMT));
                rangeEnd = LocalDateTime.of(d, LocalTime.parse(endTime, TIME_FMT));
            } catch (Exception e) {
                return "时间格式有误，请用 HH:mm";
            }
        }

        StringBuilder sb = new StringBuilder("推荐会议室：\n");
        int matched = 0;
        for (MeetingRoom r : rooms) {
            boolean free = true;
            if (rangeStart != null && rangeEnd != null) {
                Long conflict = reservationRepository.selectCount(
                        new LambdaQueryWrapper<MeetingRoomReservation>()
                                .eq(MeetingRoomReservation::getRoomId, r.getId())
                                .notIn(MeetingRoomReservation::getStatus, ReservationStatusEnum.EXCLUDED_CODES)
                                .lt(MeetingRoomReservation::getStartTime, rangeEnd)
                                .gt(MeetingRoomReservation::getEndTime, rangeStart)
                );
                free = conflict == 0;
            }
            if (free) {
                sb.append(String.format("- %s（%s，容纳%d人，设备：%s）\n",
                        r.getName(), r.getLocation(), r.getCapacity(),
                        r.getEquipment() != null ? r.getEquipment() : "无"));
                matched++;
            }
        }
        if (matched == 0) {
            return String.format("在 %s %s~%s 没有符合条件的空闲会议室",
                    date, startTime != null ? startTime : "", endTime != null ? endTime : "");
        }
        return sb.toString();
    }

    @Tool(description = "查询某个会议室某天的空闲时段。返回该日所有空闲时间段列表。")
    public String findFreeSlots(
            @ToolParam(description = "会议室名称，支持模糊匹配") String roomName,
            @ToolParam(description = "日期，格式 yyyy-MM-dd") String date) {
        if (roomName == null || date == null) return "请提供会议室名称和日期";

        List<MeetingRoom> rooms = meetingRoomRepository.selectList(
                new LambdaQueryWrapper<MeetingRoom>()
                        .like(MeetingRoom::getName, roomName)
                        .eq(MeetingRoom::getStatus, EnableStatusEnum.ENABLED.getCode())
        );
        if (rooms.isEmpty()) return "未找到名为「" + roomName + "」的可用会议室";
        if (rooms.size() > 1) {
            StringBuilder sb = new StringBuilder("匹配到多个会议室，请明确指定：\n");
            for (MeetingRoom r : rooms) {
                sb.append(String.format("- %s（%s）\n", r.getName(), r.getLocation()));
            }
            return sb.toString();
        }
        MeetingRoom room = rooms.get(0);

        LocalDate d;
        try {
            d = LocalDate.parse(date, DATE_FMT);
        } catch (Exception e) {
            return "日期格式有误，请用 yyyy-MM-dd";
        }

        // 该日已生效的预约列表（按开始时间升序）
        List<MeetingRoomReservation> reservations = reservationRepository.selectList(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .eq(MeetingRoomReservation::getRoomId, room.getId())
                        .notIn(MeetingRoomReservation::getStatus, ReservationStatusEnum.EXCLUDED_CODES)
                        .between(MeetingRoomReservation::getStartTime, d.atStartOfDay(), d.atTime(LocalTime.MAX))
                        .orderByAsc(MeetingRoomReservation::getStartTime)
        );

        // 计算空闲段：默认工作时段 09:00 ~ 21:00（覆盖常规会议时间）
        LocalTime dayStart = LocalTime.of(9, 0);
        LocalTime dayEnd = LocalTime.of(21, 0);
        // 若会议室配置了可预约时段，优先使用配置
        if (room.getBookableStart() != null && !room.getBookableStart().isBlank()) {
            try { dayStart = LocalTime.parse(room.getBookableStart(), TIME_FMT); } catch (Exception ignored) {}
        }
        if (room.getBookableEnd() != null && !room.getBookableEnd().isBlank()) {
            try { dayEnd = LocalTime.parse(room.getBookableEnd(), TIME_FMT); } catch (Exception ignored) {}
        }

        LocalDateTime cursor = LocalDateTime.of(d, dayStart);
        LocalDateTime dayLimit = LocalDateTime.of(d, dayEnd);
        List<String> freeSlots = new ArrayList<>();
        for (MeetingRoomReservation r : reservations) {
            if (r.getStartTime().isAfter(cursor)) {
                freeSlots.add(cursor.format(TIME_FMT) + "~" + r.getStartTime().format(TIME_FMT));
            }
            if (r.getEndTime().isAfter(cursor)) {
                cursor = r.getEndTime();
            }
        }
        if (cursor.isBefore(dayLimit)) {
            freeSlots.add(cursor.format(TIME_FMT) + "~" + dayLimit.format(TIME_FMT));
        }

        if (freeSlots.isEmpty()) {
            return String.format("%s 在 %s 没有空闲时段", room.getName(), date);
        }
        return String.format("%s 在 %s 的空闲时段：%s",
                room.getName(), date, String.join("、", freeSlots));
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
        LocalDateTime rangeStart = null;
        LocalDateTime rangeEnd = null;
        if (startDate != null && !startDate.isBlank()) {
            try { rangeStart = LocalDate.parse(startDate, DATE_FMT).atStartOfDay(); } catch (Exception ignored) {}
        }
        if (endDate != null && !endDate.isBlank()) {
            try { rangeEnd = LocalDate.parse(endDate, DATE_FMT).atTime(LocalTime.MAX); } catch (Exception ignored) {}
        }
        if (rangeStart != null) wrapper.ge(MeetingRoomReservation::getStartTime, rangeStart);
        if (rangeEnd != null) wrapper.le(MeetingRoomReservation::getStartTime, rangeEnd);

        List<MeetingRoomReservation> list = reservationRepository.selectList(wrapper);
        if (list.isEmpty()) return "您在指定时间段内没有预约记录";

        long total = list.size();
        long confirmed = list.stream().filter(r -> r.getStatus().equals(ReservationStatusEnum.CONFIRMED.getCode())).count();
        long cancelled = list.stream().filter(r -> r.getStatus().equals(ReservationStatusEnum.CANCELLED.getCode())).count();
        long pending = list.stream().filter(r -> r.getStatus().equals(ReservationStatusEnum.PENDING.getCode())).count();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("共 %d 条预约记录（已确认 %d、待确认 %d、已取消 %d）：\n",
                total, confirmed, pending, cancelled));
        // 仅展示前 10 条，避免回复过长
        List<MeetingRoomReservation> showList = list.stream().limit(10).collect(Collectors.toList());
        for (MeetingRoomReservation r : showList) {
            String statusDesc;
            switch (r.getStatus()) {
                case 0: statusDesc = "待确认"; break;
                case 1: statusDesc = "已确认"; break;
                case 2: statusDesc = "已取消"; break;
                default: statusDesc = "未知";
            }
            sb.append(String.format("- %s %s~%s %s（%s）\n",
                    r.getStartTime().format(DATE_FMT),
                    r.getStartTime().format(TIME_FMT),
                    r.getEndTime().format(TIME_FMT),
                    r.getSubject() != null ? r.getSubject() : "未命名",
                    statusDesc));
        }
        if (total > 10) {
            sb.append(String.format("（仅展示前 10 条，共 %d 条）\n", total));
        }
        return sb.toString();
    }

    @Tool(description = "查询所有部门列表（用于邀请参会人时按部门选择）。返回部门ID和名称。")
    public String listDepartments() {
        try {
            var result = departmentFeignClient.listFlat();
            if (result == null || result.getData() == null || result.getData().isEmpty()) {
                return "当前系统未配置部门";
            }
            StringBuilder sb = new StringBuilder("部门列表：\n");
            for (DepartmentBriefDTO d : result.getData()) {
                sb.append(String.format("- [%d] %s\n", d.getId(), d.getName()));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("listDepartments 调用失败", e);
            return "查询部门列表失败，请稍后重试";
        }
    }

    @Tool(description = "按部门邀请参会人。传入预约ID和部门ID，将该部门所有成员加入参会人列表。仅预约创建者可操作。")
    public String inviteDepartmentAttendees(
            ToolContext toolContext,
            @ToolParam(description = "预约记录ID") Long reservationId,
            @ToolParam(description = "部门ID") Long departmentId) {
        Long userId = ToolAuthHelper.requireUserId(toolContext);
        if (reservationId == null || departmentId == null) {
            return "请提供预约ID和部门ID";
        }
        try {
            int count = attendeeService.inviteDepartment(reservationId, userId, departmentId);
            return String.format("已成功邀请 %d 位部门成员加入预约 %d", count, reservationId);
        } catch (BusinessException e) {
            return "邀请失败：" + e.getMessage();
        } catch (Exception e) {
            log.error("inviteDepartmentAttendees 调用失败, reservationId={}, departmentId={}",
                    reservationId, departmentId, e);
            return "邀请参会人失败，请稍后重试";
        }
    }

    @Tool(description = "查询某预约的参会人列表。返回参会人姓名、部门、参会状态。")
    public String listReservationAttendees(
            @ToolParam(description = "预约记录ID") Long reservationId) {
        if (reservationId == null) return "请提供预约ID";
        var attendees = attendeeService.listAttendees(reservationId);
        if (attendees.isEmpty()) return "该预约暂无参会人";

        StringBuilder sb = new StringBuilder("参会人列表：\n");
        for (var a : attendees) {
            String statusDesc;
            switch (a.getStatus()) {
                case 0: statusDesc = "待查阅"; break;
                case 1: statusDesc = "已查阅"; break;
                case 2: statusDesc = "已拒绝"; break;
                default: statusDesc = "未知";
            }
            String name = a.getRealName() != null && !a.getRealName().isBlank()
                    ? a.getRealName() : a.getUsername();
            sb.append(String.format("- %s（%s） %s\n",
                    name,
                    a.getDepartmentName() != null ? a.getDepartmentName() : "未分配部门",
                    statusDesc));
        }
        return sb.toString();
    }
}
