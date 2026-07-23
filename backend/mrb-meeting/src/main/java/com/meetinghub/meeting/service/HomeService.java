package com.meetinghub.meeting.service;

import java.util.Map;

/**
 * 首页统计服务接口
 */
public interface HomeService {

    /** 获取首页统计数据（会议室总数、今日预约数、待审批数） */
    Map<String, Object> getStats();
}
