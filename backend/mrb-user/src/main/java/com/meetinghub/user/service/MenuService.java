package com.meetinghub.user.service;

import com.meetinghub.user.model.dto.MenuCreateDTO;
import com.meetinghub.user.model.dto.MenuUpdateDTO;
import com.meetinghub.user.model.vo.MenuVO;

import java.util.List;

public interface MenuService {

    /**
     * 管理端：获取全部菜单树
     */
    List<MenuVO> listTree();

    /**
     * 用户端：根据角色获取可见菜单树
     */
    List<MenuVO> listByRole(String role);

    /**
     * 新增菜单
     */
    void create(MenuCreateDTO dto);

    /**
     * 更新菜单
     */
    void update(MenuUpdateDTO dto);

    /**
     * 删除菜单
     */
    void delete(Long id);

    /**
     * 保存角色菜单权限
     */
    void saveRoleMenus(String role, List<Long> menuIds);
}
