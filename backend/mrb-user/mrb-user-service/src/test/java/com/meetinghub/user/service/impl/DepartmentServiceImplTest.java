package com.meetinghub.user.service.impl;

import com.meetinghub.user.model.entity.Department;
import com.meetinghub.user.repository.DepartmentRepository;
import com.meetinghub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 部门服务启用子树能力单元测试。
 */
@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(departmentService, "baseMapper", departmentRepository);
    }

    @Test
    void should_returnSelfAndEnabledDescendants_when_childrenExist() {
        when(departmentRepository.selectList(any())).thenReturn(List.of(
                department(3L, "研发部", 2L, 1),
                department(4L, "前端组", 3L, 1),
                department(5L, "后端组", 3L, 1),
                department(6L, "测试组", 4L, 1),
                department(7L, "运维部", 2L, 1)
        ));

        Set<Long> result = departmentService.listEnabledDescendantIds(3L);

        assertThat(result).containsExactlyInAnyOrder(3L, 4L, 5L, 6L);
    }

    @Test
    void should_excludeDisabledDescendantAndItsSubtree() {
        when(departmentRepository.selectList(any())).thenReturn(List.of(
                department(3L, "研发部", 2L, 1),
                department(4L, "前端组", 3L, 1)
        ));

        Set<Long> result = departmentService.listEnabledDescendantIds(3L);

        assertThat(result).containsExactlyInAnyOrder(3L, 4L);
    }

    @Test
    void should_returnEmpty_when_rootDisabled() {
        when(departmentRepository.selectList(any())).thenReturn(List.of(
                department(4L, "前端组", 3L, 1)
        ));

        Set<Long> result = departmentService.listEnabledDescendantIds(3L);

        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_rootMissing() {
        when(departmentRepository.selectList(any())).thenReturn(List.of(
                department(4L, "前端组", 3L, 1)
        ));

        Set<Long> result = departmentService.listEnabledDescendantIds(3L);

        assertThat(result).isEmpty();
    }

    private Department department(Long id, String name, Long parentId, Integer status) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        department.setParentId(parentId);
        department.setStatus(status);
        return department;
    }
}
