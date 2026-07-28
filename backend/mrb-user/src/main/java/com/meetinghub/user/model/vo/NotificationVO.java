package com.meetinghub.user.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationVO {
    private Long id;
    private String type;
    private String title;
    private String content;
    private String refType;
    private Long refId;
    private Integer isRead;
    private LocalDateTime createTime;
}
