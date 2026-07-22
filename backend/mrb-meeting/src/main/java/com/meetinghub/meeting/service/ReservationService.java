package com.meetinghub.meeting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.meeting.model.dto.ReservationCreateDTO;
import com.meetinghub.meeting.model.dto.ReservationPageQuery;
import com.meetinghub.meeting.model.vo.ReservationVO;

import java.util.List;

public interface ReservationService {

    void createReservation(Long userId, ReservationCreateDTO dto);

    void cancelReservation(Long userId, Long reservationId);

    IPage<ReservationVO> listMyReservations(Long userId, ReservationPageQuery query);

    List<ReservationVO> listByRoomAndDate(Long roomId, String date);

    // 管理接口
    IPage<ReservationVO> listAllReservations(ReservationPageQuery query);

    void approveReservation(Long reservationId);

    void rejectReservation(Long reservationId);
}
