package com.meetinghub.meeting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.meetinghub.meeting.model.entity.ReservationAttendee;
import com.meetinghub.meeting.model.vo.AttendeeVO;

import java.util.List;

/**
 * 预约参会人服务接口
 */
public interface ReservationAttendeeService extends IService<ReservationAttendee> {

    /**
     * 邀请参会人（覆盖式：旧 invitee 保留，新 userIds 追加；已存在的跳过）
     */
    void inviteAttendees(Long reservationId, Long inviterId, List<Long> userIds);

    /**
     * 按部门邀请：将部门所有成员加入参会人列表
     */
    int inviteDepartment(Long reservationId, Long inviterId, Long departmentId);

    /**
     * 查询预约的参会人列表（含用户基础信息）
     */
    List<AttendeeVO> listAttendees(Long reservationId);

    /**
     * 移除参会人
     */
    void removeAttendee(Long reservationId, Long userId, Long operatorId);
}
