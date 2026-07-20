package com.tulingshop.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tulingshop.user.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRepository extends BaseMapper<User> {
}
