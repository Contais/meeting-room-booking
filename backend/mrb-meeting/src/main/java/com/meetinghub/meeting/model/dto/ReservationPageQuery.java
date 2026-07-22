package com.meetinghub.meeting.model.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class ReservationPageQuery implements Serializable {
    private Integer page = 1;
    private Integer size = 10;
    private Long roomId;
    private Long userId;
    private Integer status;
}
