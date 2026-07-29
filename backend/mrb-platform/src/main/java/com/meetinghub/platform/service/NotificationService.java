package com.meetinghub.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.meetinghub.common.model.dto.NotificationSendDTO;
import com.meetinghub.platform.model.entity.Notification;
import com.meetinghub.platform.model.vo.NotificationVO;

import java.util.List;

/**
 * 站内信通知服务
 */
public interface NotificationService extends IService<Notification> {

    /**
     * 发送通知给单个用户
     */
    void send(NotificationSendDTO dto);

    /**
     * 批量发送通知给多个用户
     */
    void sendBatch(List<Long> userIds, NotificationSendDTO template);

    /**
     * 分页查询当前用户通知
     */
    IPage<NotificationVO> page(Long userId, String type, Integer isRead, int page, int size);

    /**
     * 未读消息数
     */
    long unreadCount(Long userId);

    /**
     * 标记单条已读
     */
    void markAsRead(Long userId, Long notificationId);

    /**
     * 全部已读
     */
    void markAllAsRead(Long userId);

    /**
     * 删除通知（仅本人）
     */
    void delete(Long userId, Long notificationId);
}
