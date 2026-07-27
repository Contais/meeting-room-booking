package com.meetinghub.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meetinghub.user.model.entity.Role;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleRepository extends BaseMapper<Role> {
}
