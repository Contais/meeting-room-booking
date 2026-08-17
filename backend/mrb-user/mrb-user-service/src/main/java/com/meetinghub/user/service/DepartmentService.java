package com.meetinghub.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.meetinghub.user.model.dto.DepartmentCreateDTO;
import com.meetinghub.user.model.dto.DepartmentUpdateDTO;
import com.meetinghub.user.model.entity.Department;
import com.meetinghub.user.model.vo.DepartmentVO;

import java.util.List;
import java.util.Set;

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
     * 获取指定部门及其所有启用后代部门的 ID 集合。
     *
     * <p>目标部门不存在或已禁用时返回空集合；禁用子部门不会被继续向下遍历。</p>
     *
     * @param departmentId 目标部门 ID
     * @return 包含目标部门在内的启用子树部门 ID 集合
     */
    Set<Long> listEnabledDescendantIds(Long departmentId);

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
