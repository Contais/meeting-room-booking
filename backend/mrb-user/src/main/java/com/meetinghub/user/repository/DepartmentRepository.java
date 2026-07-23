package com.meetinghub.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meetinghub.user.model.entity.Department;
import org.apache.ibatis.annotations.Mapper;

/**
 * 部门数据访问层
 */
@Mapper
public interface DepartmentRepository extends BaseMapper<Department> {
}
