package com.meetinghub.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meetinghub.common.enums.EnableStatusEnum;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.user.model.dto.DepartmentCreateDTO;
import com.meetinghub.user.model.dto.DepartmentUpdateDTO;
import com.meetinghub.user.model.entity.Department;
import com.meetinghub.user.model.entity.User;
import com.meetinghub.user.model.vo.DepartmentVO;
import com.meetinghub.user.repository.DepartmentRepository;
import com.meetinghub.user.repository.UserRepository;
import com.meetinghub.user.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门服务实现
 */
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl extends ServiceImpl<DepartmentRepository, Department> implements DepartmentService {

    /**
     * 顶级部门的父节点 ID
     */
    private static final Long ROOT_PARENT_ID = 0L;

    private final UserRepository userRepository;

    @Override
    public List<DepartmentVO> listTree() {
        List<Department> all = list(
                new LambdaQueryWrapper<Department>().orderByAsc(Department::getSortOrder)
        );
        List<DepartmentVO> voList = all.stream().map(this::toVO).collect(Collectors.toList());
        return buildTree(voList, ROOT_PARENT_ID);
    }

    @Override
    public List<DepartmentVO> listFlat() {
        List<Department> all = list(
                new LambdaQueryWrapper<Department>()
                        .eq(Department::getStatus, EnableStatusEnum.ENABLED.getCode())
                        .orderByAsc(Department::getSortOrder)
        );
        return all.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(DepartmentCreateDTO dto) {
        if (dto.getParentId() == null) {
            dto.setParentId(ROOT_PARENT_ID);
        }
        // 校验父部门存在
        if (!ROOT_PARENT_ID.equals(dto.getParentId())) {
            Department parent = getById(dto.getParentId());
            if (parent == null) {
                throw new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND);
            }
        }
        // 校验同级名称唯一
        checkNameUnique(dto.getName(), dto.getParentId(), null);
        Department dept = new Department();
        dept.setName(dto.getName());
        dept.setParentId(dto.getParentId());
        dept.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        dept.setStatus(EnableStatusEnum.ENABLED.getCode());
        save(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DepartmentUpdateDTO dto) {
        Department dept = getById(dto.getId());
        if (dept == null) {
            throw new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }
        Long newParentId = dto.getParentId() != null ? dto.getParentId() : ROOT_PARENT_ID;
        // 不能移动到自身
        if (dto.getId().equals(newParentId)) {
            throw new BusinessException(ErrorCode.DEPARTMENT_CIRCULAR);
        }
        // 校验循环引用
        if (!ROOT_PARENT_ID.equals(newParentId) && isDescendant(dto.getId(), newParentId)) {
            throw new BusinessException(ErrorCode.DEPARTMENT_CIRCULAR);
        }
        // 校验同级名称唯一
        checkNameUnique(dto.getName(), newParentId, dto.getId());
        dept.setName(dto.getName());
        dept.setParentId(newParentId);
        if (dto.getSortOrder() != null) {
            dept.setSortOrder(dto.getSortOrder());
        }
        updateById(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Department dept = getById(id);
        if (dept == null) {
            throw new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }
        // 校验无子部门
        Long childCount = count(
                new LambdaQueryWrapper<Department>().eq(Department::getParentId, id)
        );
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.DEPARTMENT_HAS_CHILDREN);
        }
        // 校验无关联用户
        Long userCount = userRepository.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getDepartmentId, id)
        );
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.DEPARTMENT_HAS_USERS);
        }
        removeById(id);
    }

    private void checkNameUnique(String name, Long parentId, Long excludeId) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<Department>()
                .eq(Department::getName, name)
                .eq(Department::getParentId, parentId);
        if (excludeId != null) {
            wrapper.ne(Department::getId, excludeId);
        }
        Long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.DEPARTMENT_NAME_DUPLICATE);
        }
    }

    private boolean isDescendant(Long ancestorId, Long targetId) {
        if (targetId.equals(ancestorId)) return true;
        Department target = getById(targetId);
        if (target == null || ROOT_PARENT_ID.equals(target.getParentId())) return false;
        if (target.getParentId().equals(ancestorId)) return true;
        return isDescendant(ancestorId, target.getParentId());
    }

    private List<DepartmentVO> buildTree(List<DepartmentVO> all, Long parentId) {
        return all.stream()
                .filter(d -> parentId.equals(d.getParentId()))
                .peek(d -> d.setChildren(buildTree(all, d.getId())))
                .collect(Collectors.toList());
    }

    private DepartmentVO toVO(Department dept) {
        DepartmentVO vo = new DepartmentVO();
        vo.setId(dept.getId());
        vo.setName(dept.getName());
        vo.setParentId(dept.getParentId());
        vo.setSortOrder(dept.getSortOrder());
        vo.setStatus(dept.getStatus());
        vo.setCreateTime(dept.getCreateTime());
        return vo;
    }
}
