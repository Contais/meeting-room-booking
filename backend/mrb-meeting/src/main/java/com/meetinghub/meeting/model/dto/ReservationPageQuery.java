package com.meetinghub.meeting.model.dto;

import lombok.Data;
import java.io.Serializable;

@Data
/** 预约分页查询参数 */
public class ReservationPageQuery implements Serializable {
    private Integer page = 1;
    private Integer size = 10;
    private Long roomId;
    private Long userId;
    private Integer status;
}
