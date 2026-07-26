package com.meetinghub.meeting.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meetinghub.common.context.UserContext;
import com.meetinghub.common.enums.EnableStatusEnum;
import com.meetinghub.common.enums.ReservationStatusEnum;
import com.meetinghub.meeting.model.dto.ReservationCreateDTO;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.repository.MeetingRoomRepository;
import com.meetinghub.meeting.repository.ReservationRepository;
import com.meetinghub.meeting.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 聊天助手工具类 - 提供会议室查询功能
 */
@Component
@RequiredArgsConstructor
public class MeetingRoomTool {

    private final ReservationService reservationService;
    private final MeetingRoomRepository meetingRoomRepository;
    private final ReservationRepository reservationRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Tool(description = "查询所有可用的会议室列表，返回名称、位置、容量、设备信息")
    public String listAvailableRooms() {
        List<MeetingRoom> rooms = meetingRoomRepository.selectList(
                new LambdaQueryWrapper<MeetingRoom>().eq(MeetingRoom::getStatus, EnableStatusEnum.ENABLED.getCode())
        );
        if (rooms.isEmpty()) return "当前没有可用的会议室";
        StringBuilder sb = new StringBuilder("可用会议室列表：\n");
        for (MeetingRoom r : rooms) {
            sb.append(String.format("- %s（%s，容纳%d人，设备：%s）\n",
                    r.getName(), r.getLocation(), r.getCapacity(), r.getEquipment()));
        }
        return sb.toString();
    }

    @Tool(description = "查询指定日期某个会议室的预约情况")
    public String queryRoomReservations(
            @ToolParam(description = "会议室名称，支持模糊匹配") String roomName,
            @ToolParam(description = "日期，格式 yyyy-MM-dd") String date) {
        if (roomName == null || date == null) return "请提供会议室名称和日期";

        // 查找会议室
        List<MeetingRoom> rooms = meetingRoomRepository.selectList(
                new LambdaQueryWrapper<MeetingRoom>().like(MeetingRoom::getName, roomName).eq(MeetingRoom::getStatus, EnableStatusEnum.ENABLED.getCode())
        );
        if (rooms.isEmpty()) return "未找到名为" + roomName + "的会议室";

        MeetingRoom room = rooms.get(0);
        LocalDate d = LocalDate.parse(date);
        List<MeetingRoomReservation> reservations = reservationRepository.selectList(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .eq(MeetingRoomReservation::getRoomId, room.getId())
                        .ne(MeetingRoomReservation::getStatus, ReservationStatusEnum.CANCELLED.getCode())
                        .between(MeetingRoomReservation::getStartTime, d.atStartOfDay(), d.atTime(LocalTime.MAX))
                        .orderByAsc(MeetingRoomReservation::getStartTime)
        );

        if (reservations.isEmpty()) return room.getName() + " 在 " + date + " 没有预约，全天可用";

        StringBuilder sb = new StringBuilder(room.getName() + " 在 " + date + " 的预约情况：\n");
        for (MeetingRoomReservation r : reservations) {
            String status = r.getStatus().equals(ReservationStatusEnum.PENDING.getCode()) ? "待确认" : "已确认";
            sb.append(String.format("- %s ~ %s  %s（%s）\n",
                    r.getStartTime().format(TIME_FMT),
                    r.getEndTime().format(TIME_FMT),
                    r.getSubject() != null ? r.getSubject() : "未命名",
                    status));
        }
        return sb.toString();
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
                            .ne(MeetingRoomReservation::getStatus, ReservationStatusEnum.CANCELLED.getCode())
                            .between(MeetingRoomReservation::getStartTime, today.atStartOfDay(), today.atTime(LocalTime.MAX))
            );
            sb.append(String.format("- %s：%d个预约\n", r.getName(), count));
        }
        return sb.toString();
    }

    @Tool(description = "创建会议室预约。传入会议室名称（支持模糊匹配，但需能唯一确定）、日期、开始/结束时间、会议主题。返回预约结果。")
    public String createReservation(
            @ToolParam(description = "会议室名称") String roomName,
            @ToolParam(description = "预约日期，格式 yyyy-MM-dd") String date,
            @ToolParam(description = "开始时间，格式 HH:mm") String startTime,
            @ToolParam(description = "结束时间，格式 HH:mm") String endTime,
            @ToolParam(description = "会议主题") String subject,
            @ToolParam(description = "参会人数", required = false) Integer attendeeCount) {
        Long userId = UserContext.getCurrentUserId();

        if (roomName == null || roomName.isBlank()) return "请提供会议室名称";
        if (date == null || startTime == null || endTime == null) return "请提供日期和开始/结束时间";

        // 模糊查询会议室，多条匹配时返回候选列表让用户明确
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

        // 解析日期时间
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
        dto.setAttendeeCount(attendeeCount);
        dto.setStartTime(start);
        dto.setEndTime(end);

        reservationService.createReservation(userId, dto);
        return String.format("预约创建成功：%s %s ~ %s「%s」",
                room.getName(), start.format(DATETIME_FMT), end.format(DATETIME_FMT), subject);
    }

    @Tool(description = "取消本人的会议室预约，传入预约记录ID。仅可取消本人创建的预约，无法取消他人的预约。")
    public String cancelMyReservation(
            @ToolParam(description = "预约记录ID") Long reservationId) {
        Long userId = UserContext.getCurrentUserId();

        // 防御性校验：在调用 Service 前先确认预约归属，防止通过 AI 误删他人数据
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

    @Tool(description = "查看本人未结束的预约（未进行或正在进行的），返回会议室名称、日期、时段、主题、状态")
    public String listMyUpcomingReservations() {
        Long userId = UserContext.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        List<MeetingRoomReservation> reservations = reservationRepository.selectList(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .eq(MeetingRoomReservation::getUserId, userId)
                        .ne(MeetingRoomReservation::getStatus, ReservationStatusEnum.CANCELLED.getCode())
                        .ge(MeetingRoomReservation::getEndTime, now)
                        .orderByAsc(MeetingRoomReservation::getStartTime)
        );

        if (reservations.isEmpty()) return "您当前没有未结束的预约";

        // 批量查询会议室名称
        Set<Long> roomIds = reservations.stream()
                .map(MeetingRoomReservation::getRoomId)
                .collect(Collectors.toSet());
        Map<Long, String> roomNameMap = meetingRoomRepository.selectBatchIds(roomIds).stream()
                .collect(Collectors.toMap(MeetingRoom::getId, MeetingRoom::getName));

        StringBuilder sb = new StringBuilder("您的未结束预约：\n");
        for (MeetingRoomReservation r : reservations) {
            String status = r.getStatus().equals(ReservationStatusEnum.PENDING.getCode()) ? "待确认" : "已确认";
            String roomName = roomNameMap.getOrDefault(r.getRoomId(), "未知会议室");
            sb.append(String.format("- %s | %s %s~%s | %s | %s\n",
                    roomName,
                    r.getStartTime().format(DATE_FMT),
                    r.getStartTime().format(TIME_FMT),
                    r.getEndTime().format(TIME_FMT),
                    r.getSubject() != null ? r.getSubject() : "未命名",
                    status));
        }
        return sb.toString();
    }
}
