package com.meetinghub.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meetinghub.platform.model.entity.SysConfig;
import com.meetinghub.platform.repository.SysConfigRepository;
import com.meetinghub.platform.service.SysConfigService;
import com.meetinghub.common.constant.RedisKeyConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 系统配置服务实现
 * <p>
 * 配置读取走 Redis 缓存（前缀 {@code mrb:}），减轻 DB 压力；写操作清除缓存。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl extends ServiceImpl<SysConfigRepository, SysConfig> implements SysConfigService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String CACHE_PREFIX = RedisKeyConstant.PREFIX + "sys:config:";
    private static final long CACHE_TTL_MINUTES = 30L;

    @Override
    public String getValueByKey(String key) {
        SysConfig config = getByKey(key);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public SysConfig getByKey(String key) {
        String cacheKey = CACHE_PREFIX + key;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            SysConfig vo = new SysConfig();
            vo.setConfigKey(key);
            vo.setConfigValue(cached);
            return vo;
        }
        SysConfig config = getOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        if (config != null) {
            stringRedisTemplate.opsForValue().set(cacheKey, config.getConfigValue(), CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        }
        return config;
    }
}
