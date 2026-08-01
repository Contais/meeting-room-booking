package com.meetinghub.meeting.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meetinghub.common.constant.DateTimePatternConstant;
import com.meetinghub.common.enums.EnableStatusEnum;
import com.meetinghub.meeting.api.enums.ReservationStatusEnum;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.model.vo.tool.ReservationToolResults;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.FreeSlotResult;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.RoomRecommendResult;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.RoomReservationResult;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.RoomStat;
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

/**
 * AI 聊天助手工具类 - 会议室域。
 * <p>
 * 仅提供会议室相关的查询能力：会议室列表、按日期查询会议室预约、当日预约统计、
 * 会议室推荐、空闲时段查询。预约/参会人/部门相关能力见 {@link ReservationTool}。
 * </p>
 * <p>
 * 工具方法直接返回结构化 record / List，由 Spring AI 的
 * {@code DefaultToolCallResultConverter} 序列化为 JSON 回传模型，
 * 展示文案交由 system prompt 与模型组织，不在工具内拼接展示字符串。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingRoomTool {

    private final MeetingRoomRepository meetingRoomRepository;
    private final ReservationRepository reservationRepository;
    private final RoomResolver roomResolver;

    private static final DateTimeFormatter DATE_FMT = DateTimePatternConstant.DATE_FMT;
    private static final DateTimeFormatter TIME_FMT = DateTimePatternConstant.TIME_FMT;

    @Tool(description = "查询所有可用的会议室列表。返回 JSON 数组，每项字段：name 会议室名称、location 位置、capacity 容量、equipment 设备")
    public List<RoomSummary> listAvailableRooms() {
        List<MeetingRoom> rooms = meetingRoomRepository.selectList(
                new LambdaQueryWrapper<MeetingRoom>().eq(MeetingRoom::getStatus, EnableStatusEnum.ENABLED.getCode())
        );
        return rooms.stream().map(RoomResolver::toSummary).toList();
    }

    @Tool(description = "查询指定日期某个会议室的预约情况。返回 JSON 对象，字段：roomName 会议室名称、date 日期、reservations 预约列表（含 reservationCode 预约编号、subject 主题、startTime/endTime 时间 yyyy-MM-dd HH:mm:ss、status 状态中文）")
    public ToolResult queryRoomReservations(
            @ToolParam(description = "会议室名称，支持模糊匹配") String roomName,
            @ToolParam(description = "日期，格式 yyyy-MM-dd") String date) {
        RoomMatch match = roomResolver.resolveByName(roomName);
        if (!(match instanceof RoomMatch.Single s)) {
            return RoomResolver.toErrorResult(match);
        }
        MeetingRoom room = s.room();

        LocalDate d;
        try {
            d = LocalDate.parse(date, DATE_FMT);
        } catch (Exception e) {
            return new ToolResult.ErrorResult("日期格式有误，请用 yyyy-MM-dd");
        }

        List<MeetingRoomReservation> reservations = reservationRepository.selectList(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .eq(MeetingRoomReservation::getRoomId, room.getId())
                        .notIn(MeetingRoomReservation::getStatus, ReservationStatusEnum.EXCLUDED_CODES)
                        .between(MeetingRoomReservation::getStartTime, d.atStartOfDay(), d.atTime(LocalTime.MAX))
                        .orderByAsc(MeetingRoomReservation::getStartTime)
        );
        List<ReservationToolResults.ReservationBrief> briefs = ReservationToolResults.toBriefList(
                reservations, Map.of(room.getId(), room.getName()));
        return new RoomReservationResult(room.getName(), date, briefs);
    }

    @Tool(description = "查询所有会议室今天的整体预约统计。返回 JSON 数组，每项字段：name 会议室名称、count 当日预约数量")
    public List<RoomStat> todayReservationStats() {
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
        return stats;
    }

    @Tool(description = "根据需求推荐可用会议室。传入日期、可选时段、人数、设备关键词，返回符合条件的空闲会议室列表。返回 JSON 对象，字段：date 日期、startTime/endTime 时段 HH:mm、rooms 会议室列表（name、location、capacity、equipment）")
    public ToolResult recommendRoom(
            @ToolParam(description = "日期，格式 yyyy-MM-dd") String date,
            @ToolParam(description = "开始时间，格式 HH:mm", required = false) String startTime,
            @ToolParam(description = "结束时间，格式 HH:mm", required = false) String endTime,
            @ToolParam(description = "参会人数", required = false) Integer capacity,
            @ToolParam(description = "设备关键词，如 投影仪/白板", required = false) String equipment) {
        if (date == null) {
            return new ToolResult.ErrorResult("请提供日期");
        }
        LocalDate d;
        try {
            d = LocalDate.parse(date, DATE_FMT);
        } catch (Exception e) {
            return new ToolResult.ErrorResult("日期格式有误，请用 yyyy-MM-dd");
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

        // 2. 若有时段，进一步过滤掉冲突的会议室
        LocalDateTime rangeStart = null;
        LocalDateTime rangeEnd = null;
        if (startTime != null && endTime != null && !startTime.isBlank() && !endTime.isBlank()) {
            try {
                rangeStart = LocalDateTime.of(d, LocalTime.parse(startTime, TIME_FMT));
                rangeEnd = LocalDateTime.of(d, LocalTime.parse(endTime, TIME_FMT));
            } catch (Exception e) {
                return new ToolResult.ErrorResult("时间格式有误，请用 HH:mm");
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
        return new RoomRecommendResult(date, startTime, endTime, freeRooms);
    }

    @Tool(description = "查询某个会议室某天的空闲时段。返回 JSON 对象，字段：roomName 会议室名称、date 日期、slots 空闲时段列表（start/end 均为 HH:mm）")
    public ToolResult findFreeSlots(
            @ToolParam(description = "会议室名称，支持模糊匹配") String roomName,
            @ToolParam(description = "日期，格式 yyyy-MM-dd") String date) {
        if (roomName == null || date == null) {
            return new ToolResult.ErrorResult("请提供会议室名称和日期");
        }
        RoomMatch match = roomResolver.resolveByName(roomName);
        if (!(match instanceof RoomMatch.Single s)) {
            return RoomResolver.toErrorResult(match);
        }
        MeetingRoom room = s.room();

        LocalDate d;
        try {
            d = LocalDate.parse(date, DATE_FMT);
        } catch (Exception e) {
            return new ToolResult.ErrorResult("日期格式有误，请用 yyyy-MM-dd");
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
                slots.add(new TimeSlot(cursor.toLocalTime().format(TIME_FMT),
                        r.getStartTime().toLocalTime().format(TIME_FMT)));
            }
            if (r.getEndTime().isAfter(cursor)) {
                cursor = r.getEndTime();
            }
        }
        if (cursor.isBefore(dayLimit)) {
            slots.add(new TimeSlot(cursor.toLocalTime().format(TIME_FMT), dayEnd.format(TIME_FMT)));
        }

        return new FreeSlotResult(room.getName(), date, slots);
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
