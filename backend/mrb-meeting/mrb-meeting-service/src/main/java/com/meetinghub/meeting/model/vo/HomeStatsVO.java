package com.meetinghub.meeting.model.vo;

import lombok.Data;

@Data
public class HomeStatsVO {
    private Long todayCount;
    private Long weekCount;
    private Long totalCount;
    private Long pendingCount;
}
