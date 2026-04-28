package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bcsport.admin.dto.UserQueryDTO;
import com.bcsport.admin.entity.User;
import com.bcsport.admin.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 分页查询用户列表（关联部门和角色，解决N+1问题)
     */
    @Select("SELECT u.*, d.dept_name, " +
            "       (SELECT LISTAGG(r.role_name, ',') WITHIN GROUP (ORDER BY r.role_name) " +
            "        FROM bc_sports_sys_role r " +
            "        INNER JOIN bc_sports_sys_user_role ur ON r.id = ur.role_id " +
            "        WHERE ur.user_id = u.id AND r.deleted = 0) as role_names " +
            "FROM bc_sports_sys_user u " +
            "LEFT JOIN bc_sports_sys_dept d ON u.dept_id = d.id AND d.deleted = 0 " +
            "WHERE u.deleted = 0 " +
            "ORDER BY u.sort ASC, u.create_time DESC")
    List<UserVO> selectUserPage();

    /**
     * 根据条件查询用户列表（关联部门和角色名
     */
    List<UserVO> selectUserListWithDetails(@Param("queryUser") User queryUser);

    /**
     * 分页条件查询用户列表（关联部门和角色，数据库层过滤）
     * 使用 MyBatis-Plus 分页插件进行物理分页
     */
    List<UserVO> selectUserPageWithConditions(IPage<UserVO> page, @Param("queryUser") UserQueryDTO queryUser);
}
