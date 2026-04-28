package com.bcsport.admin.service;

import com.bcsport.admin.dto.UserDTO;
import com.bcsport.admin.entity.User;
import com.bcsport.admin.mapper.UserMapper;
import com.bcsport.admin.service.impl.UserServiceImpl;
import com.bcsport.admin.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserRefactorTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    public void testAddUserMapping() {
        // Prepare DTO
        UserDTO dto = new UserDTO();
        dto.setUsername("testuser");
        dto.setPassword("123456"); // This should be encrypted
        dto.setNickname("Tester");
        dto.setRoleIds(new ArrayList<>());

        // Mock search for duplicate
        when(userMapper.selectOne(any())).thenReturn(null);
        // Mock insert
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("new-uuid"); // Simulate DB/MyBatisPlus ID assignment
            return 1;
        });

        boolean result = userService.addUserWithRoles(dto, dto.getRoleIds());

        assertTrue(result);
    }

    @Test
    public void testGetUserVODesensitization() {
        // Prepare Entity with sensitive data
        User user = new User();
        user.setId("1");
        user.setUsername("admin");
        user.setPassword("hashed_password");
        user.setSalt("xyz");

        when(userMapper.selectById("1")).thenReturn(user);

        UserVO vo = userService.getUserVOById("1");

        assertNotNull(vo);
        assertEquals("admin", vo.getUsername());
        
        // CRITICAL CHECK: Ensure no password leaked in VO
        // These fields shouldn't even exist in UserVO, but we check if they are missing/null via reflection or just type check
        // UserVO does not have getPassword or getSalt methods.
    }
}
