package com.meetinghub.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.meetinghub.platform.model.dto.RoleCreateDTO;
import com.meetinghub.platform.model.dto.RoleMenuAssignDTO;
import com.meetinghub.platform.model.dto.RoleUpdateDTO;
import com.meetinghub.platform.model.entity.Role;
import com.meetinghub.platform.model.vo.RoleVO;

import java.util.List;

public interface RoleService extends IService<Role> {

    IPage<RoleVO> pageRoles(long pageNum, long pageSize, String keyword);

    List<RoleVO> listAllRoles();

    RoleVO getRoleDetail(Long id);

    void createRole(RoleCreateDTO dto);

    void updateRole(RoleUpdateDTO dto);

    void deleteRole(Long id);

    void toggleRoleStatus(Long id);

    void assignMenus(RoleMenuAssignDTO dto);

    List<Long> getRoleMenuIds(Long roleId);
}
