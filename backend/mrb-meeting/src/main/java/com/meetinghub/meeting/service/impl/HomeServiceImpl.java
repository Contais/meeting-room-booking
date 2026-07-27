package com.meetinghub.meeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meetinghub.common.enums.EnableStatusEnum;
import com.meetinghub.common.enums.ReservationStatusEnum;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.model.vo.PeakHourVO;
import com.meetinghub.meeting.model.vo.RoomUsageVO;
import com.meetinghub.meeting.repository.MeetingRoomRepository;
import com.meetinghub.meeting.repository.ReservationRepository;
import com.meetinghub.meeting.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final MeetingRoomRepository meetingRoomRepository;
    private final ReservationRepository reservationRepository;

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        long roomCount = meetingRoomRepository.selectCount(
                new LambdaQueryWrapper<MeetingRoom>().eq(MeetingRoom::getStatus, EnableStatusEnum.ENABLED.getCode())
        );
        stats.put("roomCount", roomCount);

        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(LocalTime.MAX);
        long todayReservations = reservationRepository.selectCount(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .notIn(MeetingRoomReservation::getStatus, ReservationStatusEnum.EXCLUDED_CODES)
                        .between(MeetingRoomReservation::getStartTime, dayStart, dayEnd)
        );
        stats.put("todayReservations", todayReservations);

        long pendingApproval = reservationRepository.selectCount(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .eq(MeetingRoomReservation::getStatus, ReservationStatusEnum.PENDING.getCode())
        );
        stats.put("pendingApproval", pendingApproval);

        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long weekReservations = reservationRepository.selectCount(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .notIn(MeetingRoomReservation::getStatus, ReservationStatusEnum.EXCLUDED_CODES)
                        .ge(MeetingRoomReservation::getStartTime, weekStart.atStartOfDay())
                        .le(MeetingRoomReservation::getStartTime, today.atTime(LocalTime.MAX))
        );
        stats.put("weekReservations", weekReservations);

        long totalReservations = reservationRepository.selectCount(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .notIn(MeetingRoomReservation::getStatus, ReservationStatusEnum.EXCLUDED_CODES)
        );
        stats.put("totalReservations", totalReservations);

        return stats;
    }

    @Override
    public List<RoomUsageVO> getRoomUsage() {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(LocalTime.MAX);
        int bookableMinutes = 12 * 60;

        List<MeetingRoom> rooms = meetingRoomRepository.selectList(
                new LambdaQueryWrapper<MeetingRoom>().eq(MeetingRoom::getStatus, EnableStatusEnum.ENABLED.getCode())
        );
        List<RoomUsageVO> result = new ArrayList<>();
        for (MeetingRoom room : rooms) {
            List<MeetingRoomReservation> reservations = reservationRepository.selectList(
                    new LambdaQueryWrapper<MeetingRoomReservation>()
                            .eq(MeetingRoomReservation::getRoomId, room.getId())
                            .notIn(MeetingRoomReservation::getStatus, ReservationStatusEnum.EXCLUDED_CODES)
                            .between(MeetingRoomReservation::getStartTime, dayStart, dayEnd)
            );
            int usedMinutes = 0;
            for (MeetingRoomReservation r : reservations) {
                usedMinutes += (int) Duration.between(r.getStartTime(), r.getEndTime()).toMinutes();
            }
            RoomUsageVO vo = new RoomUsageVO();
            vo.setRoomId(room.getId());
            vo.setRoomName(room.getName());
            vo.setUsedMinutes(usedMinutes);
            vo.setTotalMinutes(bookableMinutes);
            vo.setUsageRate(Math.min(1.0, (double) usedMinutes / bookableMinutes));
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<PeakHourVO> getPeakHours() {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(LocalTime.MAX);

        List<MeetingRoomReservation> reservations = reservationRepository.selectList(
                new LambdaQueryWrapper<MeetingRoomReservation>()
                        .notIn(MeetingRoomReservation::getStatus, ReservationStatusEnum.EXCLUDED_CODES)
                        .between(MeetingRoomReservation::getStartTime, dayStart, dayEnd)
        );

        Map<Integer, Long> hourMap = new TreeMap<>();
        for (int h = 8; h < 20; h++) hourMap.put(h, 0L);

        for (MeetingRoomReservation r : reservations) {
            int startHour = r.getStartTime().getHour();
            int endHour = r.getEndTime().getHour();
            for (int h = startHour; h < endHour && h < 20; h++) {
                hourMap.merge(h, 1L, Long::sum);
            }
        }

        List<PeakHourVO> result = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : hourMap.entrySet()) {
            PeakHourVO vo = new PeakHourVO();
            vo.setHour(entry.getKey());
            vo.setCount(entry.getValue());
            result.add(vo);
        }
        return result;
    }
}
