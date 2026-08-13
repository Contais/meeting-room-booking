package com.meetinghub.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.meetinghub.platform.model.dto.MenuCreateDTO;
import com.meetinghub.platform.model.dto.MenuUpdateDTO;
import com.meetinghub.platform.model.entity.Menu;
import com.meetinghub.platform.model.vo.MenuVO;

import java.util.List;

public interface MenuService extends IService<Menu> {

    List<MenuVO> listTree();

    List<MenuVO> listByRole(String roleCode);

    void create(MenuCreateDTO dto);

    void update(MenuUpdateDTO dto);

    void delete(Long id);
}
