package com.meetinghub.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.platform.model.entity.SysDict;
import com.meetinghub.platform.model.entity.SysDictItem;
import com.meetinghub.platform.repository.SysDictItemRepository;
import com.meetinghub.platform.repository.SysDictRepository;
import com.meetinghub.platform.service.SysDictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字典服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictServiceImpl extends ServiceImpl<SysDictRepository, SysDict> implements SysDictService {

    private final SysDictItemRepository sysDictItemRepository;

    @Override
    public List<SysDictItem> listItemsByDictCode(String code) {
        SysDict dict = getOne(new LambdaQueryWrapper<SysDict>().eq(SysDict::getCode, code));
        if (dict == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "字典不存在: " + code);
        }
        return sysDictItemRepository.selectList(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictId, dict.getId())
                .eq(SysDictItem::getStatus, 1)
                .orderByAsc(SysDictItem::getSort));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addItem(SysDictItem item) {
        sysDictItemRepository.insert(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(SysDictItem item) {
        sysDictItemRepository.updateById(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long itemId) {
        sysDictItemRepository.deleteById(itemId);
    }
}
