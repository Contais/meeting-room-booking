package com.meetinghub.meeting.model.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class ChatRequest {
    @Schema(description = "对话消息")
    private String message;
}
