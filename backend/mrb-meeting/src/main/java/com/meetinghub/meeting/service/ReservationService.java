package com.meetinghub.meeting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.meeting.model.dto.ReservationCreateDTO;
import com.meetinghub.meeting.model.dto.ReservationPageQuery;
import com.meetinghub.meeting.model.vo.ReservationVO;

import java.util.List;

/**
 * 预约服务接口
 */
public interface ReservationService {

    /** 创建预约（含规则校验 + 冲突检测） */
    void createReservation(Long userId, ReservationCreateDTO dto);

    /** 取消预约（仅限本人） */
    void cancelReservation(Long userId, Long reservationId);

    /** 查询我的预约列表 */
    IPage<ReservationVO> listMyReservations(Long userId, ReservationPageQuery query);

    /** 按会议室+日期查询预约（详情页今日预约） */
    List<ReservationVO> listByRoomAndDate(Long roomId, String date);

    /** 查询全部预约（管理端） */
    IPage<ReservationVO> listAllReservations(ReservationPageQuery query);

    /** 审批通过 */
    void approveReservation(Long reservationId);

    /** 审批拒绝 */
    void rejectReservation(Long reservationId);
}
