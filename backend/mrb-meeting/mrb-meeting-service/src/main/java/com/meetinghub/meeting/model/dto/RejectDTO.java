package com.meetinghub.meeting.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 拒绝预约请求 DTO
 */
@Data
public class RejectDTO implements Serializable {

    /**
     * 拒绝原因（可选）
     */
    private String reason;
}
