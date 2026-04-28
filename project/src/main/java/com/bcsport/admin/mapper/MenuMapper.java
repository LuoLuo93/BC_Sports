package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 菜单Mapper接口
 */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {
    
    @Select("SELECT DISTINCT m.permission FROM bc_sports_sys_menu m " +
            "INNER JOIN bc_sports_sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN bc_sports_sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.status = 1 AND m.deleted = 0 AND m.permission IS NOT NULL")
    List<String> getPermissionsByUserId(String userId);

    @Select("SELECT DISTINCT m.* FROM bc_sports_sys_menu m " +
            "WHERE m.id IN (" +
            "  SELECT id FROM bc_sports_sys_menu " +
            "  START WITH id IN (" +
            "    SELECT rm.menu_id FROM bc_sports_sys_role_menu rm " +
            "    INNER JOIN bc_sports_sys_user_role ur ON rm.role_id = ur.role_id " +
            "    INNER JOIN bc_sports_sys_user u ON ur.user_id = u.id " +
            "    WHERE u.id = #{userId} OR u.username = #{userId}" +
            "  ) " +
            "  CONNECT BY PRIOR parent_id = id" +
            ") AND m.status = 1 AND m.deleted = 0 " +
            "ORDER BY m.sort")
    List<Menu> selectMenusByUserId(String userId);

    @Select("SELECT DISTINCT m.* FROM bc_sports_sys_menu m " +
            "WHERE m.id IN (" +
            "  SELECT id FROM bc_sports_sys_menu " +
            "  START WITH id IN (" +
            "    SELECT rm.menu_id FROM bc_sports_sys_role_menu rm " +
            "    WHERE rm.role_id = #{roleId}" +
            "  ) " +
            "  CONNECT BY PRIOR parent_id = id" +
            ") AND m.status = 1 AND m.deleted = 0 " +
            "ORDER BY m.sort")
    List<Menu> selectMenusByRoleId(String roleId);
}
