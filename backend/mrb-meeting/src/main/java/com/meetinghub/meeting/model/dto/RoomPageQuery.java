package com.meetinghub.meeting.model.dto;

import lombok.Data;
import java.io.Serializable;

@Data
/** 会议室分页查询参数 */
public class RoomPageQuery implements Serializable {
    private Integer page = 1;
    private Integer size = 10;
    private String keyword;
    private String location;
    private String equipment;
    private Integer minCapacity;
    private String bookableStart;
    private String bookableEnd;
    private Integer needApproval;
    private Integer status;
}
