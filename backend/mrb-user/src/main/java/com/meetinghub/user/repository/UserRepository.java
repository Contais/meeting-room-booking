package com.meetinghub.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.user.model.dto.UserPageQuery;
import com.meetinghub.user.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户数据访问层
 */
@Mapper
public interface UserRepository extends BaseMapper<User> {

    /**
     * 用户列表分页查询（关键字 OR 检索 + 多条件动态过滤，下沉 XML 提升可读性）
     *
     * @param page  分页参数
     * @param query 过滤条件
     */
    IPage<User> selectUserPage(IPage<User> page, @Param("query") UserPageQuery query);

    /**
     * 通讯录查询（启用用户 + 关键字多字段 OR + 部门过滤）
     *
     * @param keyword      关键字（用户名/姓名/手机号/邮箱）
     * @param departmentId 部门ID
     */
    List<User> selectContacts(@Param("keyword") String keyword, @Param("departmentId") Long departmentId);
}
