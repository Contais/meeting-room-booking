package com.meetinghub.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meetinghub.user.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问层
 */
@Mapper
public interface UserRepository extends BaseMapper<User> {
}
