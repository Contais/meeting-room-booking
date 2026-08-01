package com.meetinghub.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meetinghub.platform.model.entity.SysDictItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysDictItemRepository extends BaseMapper<SysDictItem> {
}
