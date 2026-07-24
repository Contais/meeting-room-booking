package com.meetinghub.meeting.service;

import com.meetinghub.meeting.model.vo.PeakHourVO;
import com.meetinghub.meeting.model.vo.RoomUsageVO;
import java.util.List;
import java.util.Map;

/**
 * 首页统计服务接口
 */
public interface HomeService {

    /** 获取首页统计数据（会议室总数、今日预约数、待审批数） */
    Map<String, Object> getStats();

    /** 会议室使用率 */
    List<RoomUsageVO> getRoomUsage();

    /** 高峰时段分布 */
    List<PeakHourVO> getPeakHours();
}
