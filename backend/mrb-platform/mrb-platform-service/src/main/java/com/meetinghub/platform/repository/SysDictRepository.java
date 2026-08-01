package com.meetinghub.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meetinghub.platform.model.entity.SysDict;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysDictRepository extends BaseMapper<SysDict> {
}
