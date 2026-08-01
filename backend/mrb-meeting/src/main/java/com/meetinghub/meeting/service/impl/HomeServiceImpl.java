package com.meetinghub.meeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meetinghub.meeting.api.enums.AttendeeStatusEnum;
import com.meetinghub.common.enums.EnableStatusEnum;
import com.meetinghub.meeting.api.enums.ReservationStatusEnum;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.model.entity.ReservationAttendee;
import com.meetinghub.meeting.model.vo.PeakHourVO;
import com.meetinghub.meeting.model.vo.RoomUsageVO;
import com.meetinghub.meeting.repository.MeetingRoomRepository;
import com.meetinghub.meeting.repository.ReservationAttendeeRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final MeetingRoomRepository meetingRoomRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationAttendeeRepository attendeeRepository;

    @Override
    public Map<String, Object> getStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        long roomCount = meetingRoomRepository.selectCount(
                new LambdaQueryWrapper<MeetingRoom>().eq(MeetingRoom::getStatus, EnableStatusEnum.ENABLED.getCode())
        );
        stats.put("roomCount", roomCount);

        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(LocalTime.MAX);

        // 今日预约：用户作为预约人创建的、今日开始的、已确认的预约数
        // 与「我的预约」页面口径一致（/reservation/my 仅展示用户创建的预约）
        long todayReservations = 0;
        if (userId != null) {
            todayReservations = reservationRepository.selectCount(
                    new LambdaQueryWrapper<MeetingRoomReservation>()
                            .eq(MeetingRoomReservation::getUserId, userId)
                            .eq(MeetingRoomReservation::getStatus, ReservationStatusEnum.CONFIRMED.getCode())
                            .between(MeetingRoomReservation::getStartTime, dayStart, dayEnd)
            );
        } else {
            todayReservations = reservationRepository.selectCount(
                    new LambdaQueryWrapper<MeetingRoomReservation>()
                            .notIn(MeetingRoomReservation::getStatus, ReservationStatusEnum.EXCLUDED_CODES)
                            .between(MeetingRoomReservation::getStartTime, dayStart, dayEnd)
            );
        }
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

        // 我的会议统计：作为参会人参加的会议数量
        if (userId != null) {
            // 查询用户参加的所有参会记录（含参会状态）
            List<ReservationAttendee> myAttendees = attendeeRepository.selectList(
                    new LambdaQueryWrapper<ReservationAttendee>()
                            .eq(ReservationAttendee::getUserId, userId)
            );
            if (!myAttendees.isEmpty()) {
                List<Long> reservationIds = myAttendees.stream()
                        .map(ReservationAttendee::getReservationId)
                        .distinct()
                        .collect(Collectors.toList());
                // 即将到来的会议数（已确认 + 开始时间在未来）
                long myUpcomingMeetings = reservationRepository.selectCount(
                        new LambdaQueryWrapper<MeetingRoomReservation>()
                                .in(MeetingRoomReservation::getId, reservationIds)
                                .eq(MeetingRoomReservation::getStatus, ReservationStatusEnum.CONFIRMED.getCode())
                                .gt(MeetingRoomReservation::getStartTime, LocalDateTime.now())
                );
                stats.put("myUpcomingMeetings", myUpcomingMeetings);
                // 待响应邀请数：参会状态为待响应 AND 预约已确认
                List<Long> pendingReservationIds = myAttendees.stream()
                        .filter(a -> AttendeeStatusEnum.PENDING.getCode().equals(a.getStatus()))
                        .map(ReservationAttendee::getReservationId)
                        .distinct()
                        .collect(Collectors.toList());
                long myPendingMeetings = 0;
                if (!pendingReservationIds.isEmpty()) {
                    myPendingMeetings = reservationRepository.selectCount(
                            new LambdaQueryWrapper<MeetingRoomReservation>()
                                    .in(MeetingRoomReservation::getId, pendingReservationIds)
                                    .eq(MeetingRoomReservation::getStatus, ReservationStatusEnum.CONFIRMED.getCode())
                    );
                }
                stats.put("myPendingMeetings", myPendingMeetings);
            } else {
                stats.put("myUpcomingMeetings", 0L);
                stats.put("myPendingMeetings", 0L);
            }
        }

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
