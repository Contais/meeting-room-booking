package com.meetinghub.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.meetinghub.user.model.dto.DepartmentCreateDTO;
import com.meetinghub.user.model.dto.DepartmentUpdateDTO;
import com.meetinghub.user.model.entity.Department;
import com.meetinghub.user.model.vo.DepartmentVO;

import java.util.List;

/**
 * 部门服务接口
 */
public interface DepartmentService extends IService<Department> {

    /**
     * 获取部门树
     */
    List<DepartmentVO> listTree();

    /**
     * 获取简单列表（供选择）
     */
    List<DepartmentVO> listFlat();

    /**
     * 新增部门
     */
    void create(DepartmentCreateDTO dto);

    /**
     * 更新部门
     */
    void update(DepartmentUpdateDTO dto);

    /**
     * 删除部门
     */
    void delete(Long id);
}
