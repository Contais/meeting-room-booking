package com.meetinghub.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meetinghub.platform.model.entity.RoleMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RoleMenuRepository extends BaseMapper<RoleMenu> {

    /**
     * 物理删除某角色的全部菜单关联。
     * <p>
     * 角色-菜单是纯关联表，重新分配权限时采用「物理删除后重建」，
     * 避免逻辑删除残留行与 {@code uk_role_menu(role_id, menu_id)} 唯一键冲突。
     * </p>
     *
     * @param roleId 角色ID
     * @return 删除行数
     */
    @Delete("DELETE FROM platform_role_menu WHERE role_id = #{roleId}")
    int deletePhysicallyByRoleId(@Param("roleId") Long roleId);

}
