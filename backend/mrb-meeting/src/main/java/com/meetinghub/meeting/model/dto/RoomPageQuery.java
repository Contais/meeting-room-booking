package com.meetinghub.meeting.model.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class RoomPageQuery implements Serializable {
    private Integer page = 1;
    private Integer size = 10;
    private String keyword;
    private Integer status;
    private Integer minCapacity;
}
