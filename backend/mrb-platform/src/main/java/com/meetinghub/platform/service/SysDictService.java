package com.meetinghub.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.meetinghub.platform.model.entity.SysDict;
import com.meetinghub.platform.model.entity.SysDictItem;

import java.util.List;

/**
 * 字典服务
 */
public interface SysDictService extends IService<SysDict> {

    /**
     * 按字典编码查询启用项
     */
    List<SysDictItem> listItemsByDictCode(String code);

    /**
     * 新增字典项
     */
    void addItem(SysDictItem item);

    /**
     * 更新字典项
     */
    void updateItem(SysDictItem item);

    /**
     * 删除字典项
     */
    void removeItem(Long itemId);
}
