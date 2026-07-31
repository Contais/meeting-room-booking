package com.meetinghub.meeting.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meetinghub.common.enums.EnableStatusEnum;
import com.meetinghub.common.enums.ReservationStatusEnum;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.model.vo.ReservationBriefVO;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.FreeSlotResult;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.RoomListResult;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.RoomRecommendResult;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.RoomReservationResult;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.RoomStat;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.RoomStatsResult;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.RoomSummary;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.TimeSlot;
import com.meetinghub.meeting.model.vo.tool.ToolResult;
import com.meetinghub.meeting.repository.MeetingRoomRepository;
import com.meetinghub.meeting.repository.ReservationRepository;
import com.meetinghub.meeting.tools.RoomResolver.RoomMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * AI 聊天助手工具类 - 会议室域。
 * <p>
 * 仅提供会议室相关的查询能力：会议室列表、按日期查询会议室预约、当日预约统计、
 * 会议室推荐、空闲时段查询。预约/参会人/部门相关能力见 {@link ReservationTool}。
 * </p>
 * <p>
 * 方法体统一遵循「查询 → 组装 {@link ToolResult} → 交由 {@link ToolResponseFormatter} 格式化」的模式，
 * 不在工具内直接拼接展示字符串。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingRoomTool {

    private final MeetingRoomRepository meetingRoomRepository;
    private final ReservationRepository reservationRepository;
    private final RoomResolver roomResolver;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Tool(description = "查询所有可用的会议室列表，返回名称、位置、容量、设备信息")
    public String listAvailableRooms() {
        List<MeetingRoom> rooms = meetingRoomRepository.selectList(
                new LambdaQueryWrapper<MeetingRoom>().eq(MeetingRoom::getStatus, EnableStatusEnum.ENABLED.getCode())
        );
        if (rooms.isEmpty()) {
            return ToolResponseFormatter.format(new ToolResult.TextResult("当前没有可用的会议室"));
        }
        List<RoomSummary> summaries = rooms.stream().map(RoomResolver::toSummary).collect(Collectors.toList());
        return ToolResponseFormatter.format(new RoomListResult("共 " + rooms.size() + " 间会议室：", summaries));
    }

    @Tool(description = "查询指定日期某个会议室的预约情况")
    public String queryRoomReservations(
            @ToolParam(description = "会议室名称，支持模糊匹配") String roomName,
            @ToolParam(description = "日期，格式 yyyy-MM-dd") String date) {
        RoomMatch match = roomResolver.resolveByName(roomName);
        if (!(match instanceof RoomMatch.Single s)) {
            return ToolResponseFormatter.format(RoomResolver.toErrorResult(match));
        }
        MeetingRoom room = s.room();

        LocalDate d;
        try {
            d = LocalDate.parse(date, DATE_FMT);
        } catch (Exception e) {
            return ToolResponseFormatter.format(new ToolResult.TextResult("日期格式有误，请用 yyyy-MM-dd"));
        }

        List<MeetingRoomReservation> reservations = reservationRepository.selectList(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .eq(MeetingRoomReservation::getRoomId, room.getId())
                        .notIn(MeetingRoomReservation::getStatus, ReservationStatusEnum.EXCLUDED_CODES)
                        .between(MeetingRoomReservation::getStartTime, d.atStartOfDay(), d.atTime(LocalTime.MAX))
                        .orderByAsc(MeetingRoomReservation::getStartTime)
        );
        if (reservations.isEmpty()) {
            return ToolResponseFormatter.format(new ToolResult.TextResult(
                    String.format("%s 在 %s 没有预约", room.getName(), date)));
        }
        List<ReservationBriefVO> briefs = ToolResponseFormatter.toBriefVOList(
                reservations, Map.of(room.getId(), room.getName()));
        return ToolResponseFormatter.format(new RoomReservationResult(room.getName(), date, briefs));
    }

    @Tool(description = "查询所有会议室今天的整体预约统计，返回每个会议室的预约数量")
    public String todayReservationStats() {
        LocalDate today = LocalDate.now();
        List<MeetingRoom> rooms = meetingRoomRepository.selectList(
                new LambdaQueryWrapper<MeetingRoom>().eq(MeetingRoom::getStatus, EnableStatusEnum.ENABLED.getCode())
        );
        List<RoomStat> stats = new ArrayList<>();
        for (MeetingRoom r : rooms) {
            long count = reservationRepository.selectCount(
                    new LambdaQueryWrapper<MeetingRoomReservation>()
                            .eq(MeetingRoomReservation::getRoomId, r.getId())
                            .notIn(MeetingRoomReservation::getStatus, ReservationStatusEnum.EXCLUDED_CODES)
                            .between(MeetingRoomReservation::getStartTime, today.atStartOfDay(), today.atTime(LocalTime.MAX))
            );
            stats.add(new RoomStat(r.getName(), count));
        }
        return ToolResponseFormatter.format(new RoomStatsResult(stats));
    }

    @Tool(description = "根据需求推荐可用会议室。传入日期、可选时段、人数、设备关键词，返回符合条件的空闲会议室列表。")
    public String recommendRoom(
            @ToolParam(description = "日期，格式 yyyy-MM-dd") String date,
            @ToolParam(description = "开始时间，格式 HH:mm", required = false) String startTime,
            @ToolParam(description = "结束时间，格式 HH:mm", required = false) String endTime,
            @ToolParam(description = "参会人数", required = false) Integer capacity,
            @ToolParam(description = "设备关键词，如 投影仪/白板", required = false) String equipment) {
        if (date == null) {
            return ToolResponseFormatter.format(new ToolResult.TextResult("请提供日期"));
        }
        LocalDate d;
        try {
            d = LocalDate.parse(date, DATE_FMT);
        } catch (Exception e) {
            return ToolResponseFormatter.format(new ToolResult.TextResult("日期格式有误，请用 yyyy-MM-dd"));
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
        if (rooms.isEmpty()) {
            return ToolResponseFormatter.format(new ToolResult.TextResult("未找到符合条件的会议室"));
        }

        // 2. 若有时段，进一步过滤掉冲突的会议室
        LocalDateTime rangeStart = null;
        LocalDateTime rangeEnd = null;
        if (startTime != null && endTime != null && !startTime.isBlank() && !endTime.isBlank()) {
            try {
                rangeStart = LocalDateTime.of(d, LocalTime.parse(startTime, TIME_FMT));
                rangeEnd = LocalDateTime.of(d, LocalTime.parse(endTime, TIME_FMT));
            } catch (Exception e) {
                return ToolResponseFormatter.format(new ToolResult.TextResult("时间格式有误，请用 HH:mm"));
            }
        }

        List<RoomSummary> freeRooms = new ArrayList<>();
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
                freeRooms.add(RoomResolver.toSummary(r));
            }
        }
        if (freeRooms.isEmpty()) {
            return ToolResponseFormatter.format(new ToolResult.TextResult(
                    String.format("在 %s %s~%s 没有符合条件的空闲会议室",
                            date, startTime != null ? startTime : "", endTime != null ? endTime : "")));
        }
        return ToolResponseFormatter.format(new RoomRecommendResult(date, startTime, endTime, freeRooms));
    }

    @Tool(description = "查询某个会议室某天的空闲时段。返回该日所有空闲时间段列表。")
    public String findFreeSlots(
            @ToolParam(description = "会议室名称，支持模糊匹配") String roomName,
            @ToolParam(description = "日期，格式 yyyy-MM-dd") String date) {
        if (roomName == null || date == null) {
            return ToolResponseFormatter.format(new ToolResult.TextResult("请提供会议室名称和日期"));
        }
        RoomMatch match = roomResolver.resolveByName(roomName);
        if (!(match instanceof RoomMatch.Single s)) {
            return ToolResponseFormatter.format(RoomResolver.toErrorResult(match));
        }
        MeetingRoom room = s.room();

        LocalDate d;
        try {
            d = LocalDate.parse(date, DATE_FMT);
        } catch (Exception e) {
            return ToolResponseFormatter.format(new ToolResult.TextResult("日期格式有误，请用 yyyy-MM-dd"));
        }

        // 该日已生效的预约列表（按开始时间升序）
        List<MeetingRoomReservation> reservations = reservationRepository.selectList(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .eq(MeetingRoomReservation::getRoomId, room.getId())
                        .notIn(MeetingRoomReservation::getStatus, ReservationStatusEnum.EXCLUDED_CODES)
                        .between(MeetingRoomReservation::getStartTime, d.atStartOfDay(), d.atTime(LocalTime.MAX))
                        .orderByAsc(MeetingRoomReservation::getStartTime)
        );

        // 计算空闲段：默认工作时段 09:00 ~ 21:00，若会议室配置了可预约时段则优先使用
        LocalTime dayStart = parseTime(room.getBookableStart(), LocalTime.of(9, 0));
        LocalTime dayEnd = parseTime(room.getBookableEnd(), LocalTime.of(21, 0));

        LocalDateTime cursor = LocalDateTime.of(d, dayStart);
        LocalDateTime dayLimit = LocalDateTime.of(d, dayEnd);
        List<TimeSlot> slots = new ArrayList<>();
        for (MeetingRoomReservation r : reservations) {
            if (r.getStartTime().isAfter(cursor)) {
                slots.add(new TimeSlot(cursor.toLocalTime(), r.getStartTime().toLocalTime()));
            }
            if (r.getEndTime().isAfter(cursor)) {
                cursor = r.getEndTime();
            }
        }
        if (cursor.isBefore(dayLimit)) {
            slots.add(new TimeSlot(cursor.toLocalTime(), dayEnd));
        }

        if (slots.isEmpty()) {
            return ToolResponseFormatter.format(new ToolResult.TextResult(
                    String.format("%s 在 %s 没有空闲时段", room.getName(), date)));
        }
        return ToolResponseFormatter.format(new FreeSlotResult(room.getName(), date, slots));
    }

    /**
     * 解析 HH:mm 时间字符串，失败时返回默认值。
     */
    private LocalTime parseTime(String text, LocalTime defaultValue) {
        if (text == null || text.isBlank()) {
            return defaultValue;
        }
        try {
            return LocalTime.parse(text, TIME_FMT);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
