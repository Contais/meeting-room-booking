package com.meetinghub.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meetinghub.common.enums.EnableStatusEnum;
import com.meetinghub.common.enums.VisibleEnum;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.platform.model.dto.MenuCreateDTO;
import com.meetinghub.platform.model.dto.MenuUpdateDTO;
import com.meetinghub.platform.model.entity.Menu;
import com.meetinghub.platform.model.entity.Role;
import com.meetinghub.platform.model.entity.RoleMenu;
import com.meetinghub.platform.model.vo.MenuVO;
import com.meetinghub.platform.repository.MenuRepository;
import com.meetinghub.platform.repository.RoleMenuRepository;
import com.meetinghub.platform.repository.RoleRepository;
import com.meetinghub.platform.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单服务实现
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<MenuRepository, Menu> implements MenuService {

    private static final Long ROOT_PARENT_ID = 0L;

    private final RoleMenuRepository roleMenuRepository;
    private final RoleRepository roleRepository;

    @Override
    public List<MenuVO> listTree() {
        List<Menu> all = list(
                new LambdaQueryWrapper<Menu>().orderByAsc(Menu::getSortOrder)
        );
        List<MenuVO> voList = all.stream().map(this::toVO).collect(Collectors.toList());
        return buildTree(voList, ROOT_PARENT_ID);
    }

    @Override
    public List<MenuVO> listByRole(String roleCode) {
        Role role = roleRepository.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, roleCode)
        );
        if (role == null) return List.of();

        List<RoleMenu> roleMenus = roleMenuRepository.selectList(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, role.getId())
        );
        if (roleMenus.isEmpty()) return List.of();

        List<Long> menuIds = roleMenus.stream().map(RoleMenu::getMenuId).collect(Collectors.toList());
        List<Menu> menus = list(
                new LambdaQueryWrapper<Menu>()
                        .in(Menu::getId, menuIds)
                        .eq(Menu::getStatus, EnableStatusEnum.ENABLED.getCode())
                        .eq(Menu::getVisible, VisibleEnum.VISIBLE.getCode())
                        .orderByAsc(Menu::getSortOrder)
        );
        List<MenuVO> voList = menus.stream().map(this::toVO).collect(Collectors.toList());
        return buildTree(voList, ROOT_PARENT_ID);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(MenuCreateDTO dto) {
        if (dto.getParentId() == null) dto.setParentId(ROOT_PARENT_ID);
        if (!ROOT_PARENT_ID.equals(dto.getParentId())) {
            Menu parent = getById(dto.getParentId());
            if (parent == null) throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }
        Menu menu = new Menu();
        menu.setName(dto.getName());
        menu.setPath(dto.getPath());
        menu.setIcon(dto.getIcon() != null ? dto.getIcon() : "Document");
        menu.setParentId(dto.getParentId());
        menu.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        menu.setVisible(dto.getVisible() != null ? dto.getVisible() : VisibleEnum.VISIBLE.getCode());
        menu.setStatus(dto.getStatus() != null ? dto.getStatus() : EnableStatusEnum.ENABLED.getCode());
        save(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(MenuUpdateDTO dto) {
        Menu menu = getById(dto.getId());
        if (menu == null) throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        menu.setName(dto.getName());
        menu.setPath(dto.getPath());
        if (dto.getIcon() != null) menu.setIcon(dto.getIcon());
        menu.setParentId(dto.getParentId() != null ? dto.getParentId() : ROOT_PARENT_ID);
        if (dto.getSortOrder() != null) menu.setSortOrder(dto.getSortOrder());
        if (dto.getVisible() != null) menu.setVisible(dto.getVisible());
        if (dto.getStatus() != null) menu.setStatus(dto.getStatus());
        updateById(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Menu menu = getById(id);
        if (menu == null) throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        Long childCount = count(
                new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, id)
        );
        if (childCount > 0) throw new BusinessException(ErrorCode.MENU_HAS_CHILDREN);
        removeById(id);
        roleMenuRepository.delete(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getMenuId, id)
        );
    }

    private List<MenuVO> buildTree(List<MenuVO> all, Long parentId) {
        return all.stream()
                .filter(d -> parentId.equals(d.getParentId()))
                .peek(d -> d.setChildren(buildTree(all, d.getId())))
                .collect(Collectors.toList());
    }

    private MenuVO toVO(Menu menu) {
        MenuVO vo = new MenuVO();
        vo.setId(menu.getId());
        vo.setName(menu.getName());
        vo.setPath(menu.getPath());
        vo.setIcon(menu.getIcon());
        vo.setParentId(menu.getParentId());
        vo.setSortOrder(menu.getSortOrder());
        vo.setVisible(menu.getVisible());
        vo.setStatus(menu.getStatus());
        vo.setCreateTime(menu.getCreateTime());
        return vo;
    }
}
