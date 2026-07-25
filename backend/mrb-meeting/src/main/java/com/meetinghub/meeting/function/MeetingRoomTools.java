package com.meetinghub.meeting.function;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meetinghub.common.enums.EnableStatusEnum;
import com.meetinghub.common.enums.ReservationStatusEnum;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.repository.MeetingRoomRepository;
import com.meetinghub.meeting.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 聊天助手工具类 - 提供会议室查询功能
 */
@Component
@RequiredArgsConstructor
public class MeetingRoomTools {

    private final MeetingRoomRepository meetingRoomRepository;
    private final ReservationRepository reservationRepository;

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

    @Tool(description = "查询指定日期某个会议室的预约情况，传入会议室名称和日期(yyyy-MM-dd格式)")
    public String queryRoomReservations(Map<String, String> params) {
        String roomName = params.get("roomName");
        String date = params.get("date");
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
                    r.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                    r.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")),
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
}
