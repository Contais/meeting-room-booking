package com.meetinghub.meeting.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.meeting.model.dto.ReservationPageQuery;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.model.vo.ReservationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 预约数据访问层
 */
@Mapper
public interface ReservationRepository extends BaseMapper<MeetingRoomReservation> {

    /**
     * 我的预约分页查询（JOIN 会议室表获取名称，复杂多条件过滤，故下沉到 XML）
     *
     * @param page   分页参数
     * @param query  过滤条件
     * @param userId 当前用户ID
     */
    IPage<ReservationVO> selectMyPage(IPage<ReservationVO> page,
                                      @Param("query") ReservationPageQuery query,
                                      @Param("userId") Long userId);

    /**
     * 管理端预约分页查询（JOIN 会议室表获取名称，复杂多条件过滤，故下沉到 XML）
     *
     * @param page  分页参数
     * @param query 过滤条件
     */
    IPage<ReservationVO> selectAllPage(IPage<ReservationVO> page,
                                       @Param("query") ReservationPageQuery query);
}
