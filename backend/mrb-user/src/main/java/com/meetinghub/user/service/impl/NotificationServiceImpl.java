package com.meetinghub.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.common.model.dto.NotificationSendDTO;
import com.meetinghub.user.model.entity.Notification;
import com.meetinghub.user.model.vo.NotificationVO;
import com.meetinghub.user.repository.NotificationRepository;
import com.meetinghub.user.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 站内信通知服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationRepository, Notification> implements NotificationService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(NotificationSendDTO dto) {
        Notification n = new Notification();
        n.setUserId(dto.getUserId());
        n.setType(dto.getType());
        n.setTitle(dto.getTitle());
        n.setContent(dto.getContent());
        n.setRefType(dto.getRefType());
        n.setRefId(dto.getRefId());
        n.setIsRead(0);
        save(n);
        log.info("通知已发送, userId={}, type={}, title={}", dto.getUserId(), dto.getType(), dto.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendBatch(List<Long> userIds, NotificationSendDTO template) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<Notification> notifications = userIds.stream().map(uid -> {
            Notification n = new Notification();
            n.setUserId(uid);
            n.setType(template.getType());
            n.setTitle(template.getTitle());
            n.setContent(template.getContent());
            n.setRefType(template.getRefType());
            n.setRefId(template.getRefId());
            n.setIsRead(0);
            return n;
        }).toList();
        saveBatch(notifications);
        log.info("批量通知已发送, userCount={}, type={}", userIds.size(), template.getType());
    }

    @Override
    public IPage<NotificationVO> page(Long userId, String type, Integer isRead, int page, int size) {
        Page<NotificationVO> p = new Page<>(page, size);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(type != null && !type.isEmpty(), Notification::getType, type)
                .eq(isRead != null, Notification::getIsRead, isRead)
                .orderByDesc(Notification::getCreateTime);
        Page<Notification> entityPage = new Page<>(page, size);
        IPage<Notification> result = page(entityPage, wrapper);
        return result.convert(this::toVO);
    }

    @Override
    public long unreadCount(Long userId) {
        return count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long userId, Long notificationId) {
        Notification n = getById(notificationId);
        if (n == null || !n.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "通知不存在");
        }
        if (n.getIsRead() == 1) {
            return;
        }
        update(new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        update(new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long notificationId) {
        Notification n = getById(notificationId);
        if (n == null || !n.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "通知不存在");
        }
        removeById(notificationId);
    }

    private NotificationVO toVO(Notification n) {
        NotificationVO vo = new NotificationVO();
        vo.setId(n.getId());
        vo.setType(n.getType());
        vo.setTitle(n.getTitle());
        vo.setContent(n.getContent());
        vo.setRefType(n.getRefType());
        vo.setRefId(n.getRefId());
        vo.setIsRead(n.getIsRead());
        vo.setCreateTime(n.getCreateTime());
        return vo;
    }
}
