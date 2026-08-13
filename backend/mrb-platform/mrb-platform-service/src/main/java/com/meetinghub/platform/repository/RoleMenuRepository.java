package com.meetinghub.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meetinghub.platform.model.entity.RoleMenu;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMenuRepository extends BaseMapper<RoleMenu> {
}
