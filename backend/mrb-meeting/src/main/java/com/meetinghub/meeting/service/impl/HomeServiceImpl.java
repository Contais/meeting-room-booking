package com.meetinghub.meeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.repository.MeetingRoomRepository;
import com.meetinghub.meeting.repository.ReservationRepository;
import com.meetinghub.meeting.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 首页统计服务实现
 */
@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final MeetingRoomRepository meetingRoomRepository;
    private final ReservationRepository reservationRepository;

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 会议室总数（启用状态）
        long roomCount = meetingRoomRepository.selectCount(
                new LambdaQueryWrapper<MeetingRoom>().eq(MeetingRoom::getStatus, 1)
        );
        stats.put("roomCount", roomCount);

        // 今日预约数
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(LocalTime.MAX);
        long todayReservations = reservationRepository.selectCount(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .ne(MeetingRoomReservation::getStatus, 2)
                        .between(MeetingRoomReservation::getStartTime, dayStart, dayEnd)
        );
        stats.put("todayReservations", todayReservations);

        // 待审批数
        long pendingApproval = reservationRepository.selectCount(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .eq(MeetingRoomReservation::getStatus, 0)
        );
        stats.put("pendingApproval", pendingApproval);

        return stats;
    }
}
