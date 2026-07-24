package com.meetinghub.meeting.model.vo;

import lombok.Data;
import java.util.List;

@Data
public class ScheduleVO {
    private List<ScheduleRoomVO> rooms;
    private List<ScheduleReservationVO> reservations;
}
