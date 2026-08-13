package com.meetinghub.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meetinghub.common.enums.DeletedEnum;
import com.meetinghub.common.enums.EnableStatusEnum;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.platform.model.dto.RoleCreateDTO;
import com.meetinghub.platform.model.dto.RoleMenuAssignDTO;
import com.meetinghub.platform.model.dto.RoleUpdateDTO;
import com.meetinghub.platform.model.entity.Role;
import com.meetinghub.platform.model.entity.RoleMenu;
import com.meetinghub.platform.model.vo.RoleVO;
import com.meetinghub.platform.repository.RoleMenuRepository;
import com.meetinghub.platform.repository.RoleRepository;
import com.meetinghub.platform.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleRepository, Role> implements RoleService {

    private final RoleMenuRepository roleMenuRepository;

    @Override
    public IPage<RoleVO> pageRoles(long pageNum, long pageSize, String keyword) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getDeleted, DeletedEnum.NOT_DELETED.getCode());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Role::getRoleName, keyword)
                    .or().like(Role::getRoleCode, keyword));
        }
        wrapper.orderByAsc(Role::getSort).orderByDesc(Role::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper).convert(this::toVO);
    }

    @Override
    public List<RoleVO> listAllRoles() {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getDeleted, DeletedEnum.NOT_DELETED.getCode());
        wrapper.eq(Role::getStatus, EnableStatusEnum.ENABLED.getCode());
        wrapper.orderByAsc(Role::getSort);
        return list(wrapper).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public RoleVO getRoleDetail(Long id) {
        Role role = getById(id);
        if (role == null || DeletedEnum.DELETED.getCode().equals(role.getDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        RoleVO vo = toVO(role);
        vo.setMenuIds(getRoleMenuIds(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(RoleCreateDTO dto) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getRoleCode, dto.getRoleCode());
        wrapper.eq(Role::getDeleted, DeletedEnum.NOT_DELETED.getCode());
        if (count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "角色编码已存在");
        }
        Role role = new Role();
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        role.setSort(dto.getSort() != null ? dto.getSort() : 0);
        role.setStatus(EnableStatusEnum.ENABLED.getCode());
        role.setIsSystem(0);
        save(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleUpdateDTO dto) {
        Role role = getById(dto.getId());
        if (role == null || DeletedEnum.DELETED.getCode().equals(role.getDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (role.getIsSystem() != null && role.getIsSystem() == 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "系统角色不可修改");
        }
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        if (dto.getSort() != null) {
            role.setSort(dto.getSort());
        }
        updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        Role role = getById(id);
        if (role == null || DeletedEnum.DELETED.getCode().equals(role.getDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (role.getIsSystem() != null && role.getIsSystem() == 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "系统角色不可删除");
        }
        removeById(id);
        roleMenuRepository.delete(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, id)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleRoleStatus(Long id) {
        Role role = getById(id);
        if (role == null || DeletedEnum.DELETED.getCode().equals(role.getDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (role.getIsSystem() != null && role.getIsSystem() == 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "系统角色不可禁用");
        }
        if (EnableStatusEnum.ENABLED.getCode().equals(role.getStatus())) {
            role.setStatus(EnableStatusEnum.DISABLED.getCode());
        } else {
            role.setStatus(EnableStatusEnum.ENABLED.getCode());
        }
        updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(RoleMenuAssignDTO dto) {
        Role role = getById(dto.getRoleId());
        if (role == null || DeletedEnum.DELETED.getCode().equals(role.getDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        roleMenuRepository.delete(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, dto.getRoleId())
        );
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            for (Long menuId : dto.getMenuIds()) {
                RoleMenu rm = new RoleMenu();
                rm.setRoleId(dto.getRoleId());
                rm.setMenuId(menuId);
                roleMenuRepository.insert(rm);
            }
        }
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        return roleMenuRepository.selectList(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId)
        ).stream().map(RoleMenu::getMenuId).collect(Collectors.toList());
    }

    private RoleVO toVO(Role role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setDescription(role.getDescription());
        vo.setStatus(role.getStatus());
        vo.setIsSystem(role.getIsSystem());
        vo.setSort(role.getSort());
        vo.setCreateTime(role.getCreateTime());
        return vo;
    }
}
