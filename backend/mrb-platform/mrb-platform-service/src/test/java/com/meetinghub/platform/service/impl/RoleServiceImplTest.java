package com.meetinghub.platform.service.impl;

import com.meetinghub.common.enums.DeletedEnum;
import com.meetinghub.platform.model.dto.RoleMenuAssignDTO;
import com.meetinghub.platform.model.entity.Role;
import com.meetinghub.platform.model.entity.RoleMenu;
import com.meetinghub.platform.repository.RoleMenuRepository;
import com.meetinghub.platform.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 角色服务核心逻辑单元测试（重点覆盖角色-菜单关联重建）。
 */
class RoleServiceImplTest {

    private RoleMenuRepository roleMenuRepository;
    private RoleRepository roleRepository;
    private RoleServiceImpl service;

    @BeforeEach
    void setUp() {
        roleMenuRepository = mock(RoleMenuRepository.class);
        roleRepository = mock(RoleRepository.class);
        service = new RoleServiceImpl(roleMenuRepository);
        ReflectionTestUtils.setField(service, "baseMapper", roleRepository);
    }

    @Test
    void should_physicallyDeleteAndRebuild_when_assignMenus() {
        Role role = new Role();
        role.setId(1L);
        role.setDeleted(DeletedEnum.NOT_DELETED.getCode());
        when(roleRepository.selectById(1L)).thenReturn(role);

        RoleMenuAssignDTO dto = new RoleMenuAssignDTO();
        dto.setRoleId(1L);
        dto.setMenuIds(List.of(1L, 2L));

        service.assignMenus(dto);

        verify(roleMenuRepository).deletePhysicallyByRoleId(1L);
        verify(roleMenuRepository, times(2)).insert(any(RoleMenu.class));
    }
}
