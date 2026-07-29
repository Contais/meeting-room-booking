package com.meetinghub.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.meetinghub.platform.model.entity.SysConfig;

/**
 * 系统配置服务
 */
public interface SysConfigService extends IService<SysConfig> {

    /**
     * 按 key 查询配置值
     */
    String getValueByKey(String key);

    /**
     * 按 key 查询配置实体
     */
    SysConfig getByKey(String key);
}
