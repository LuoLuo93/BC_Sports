package com.bcsport.admin.util;

import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.entity.User;
import com.bcsport.admin.vo.UserVO;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BeanCopyUtilsTest {

    @Test
    public void testCopySingleObject() {
        User user = new User();
        user.setId("1");
        user.setUsername("admin");
        user.setNickname("Administrator");
        user.setPassword("secret"); // UserVO has no password field

        UserVO vo = BeanCopyUtils.copy(user, UserVO.class);

        assertNotNull(vo);
        assertEquals("1", vo.getId());
        assertEquals("admin", vo.getUsername());
        assertEquals("Administrator", vo.getNickname());
    }

    @Test
    public void testCopyList() {
        User u1 = new User();
        u1.setId("1");
        User u2 = new User();
        u2.setId("2");

        List<UserVO> voList = BeanCopyUtils.copyList(Arrays.asList(u1, u2), UserVO.class);

        assertEquals(2, voList.size());
        assertEquals("1", voList.get(0).getId());
        assertEquals("2", voList.get(1).getId());
    }

    @Test
    public void testCopyPage() {
        User u1 = new User();
        u1.setId("1");
        
        PageResult<User> pageSource = new PageResult<>();
        pageSource.setRecords(Arrays.asList(u1));
        pageSource.setTotal(100L);
        pageSource.setPageNum(1L);
        pageSource.setPageSize(10L);

        PageResult<UserVO> pageTarget = BeanCopyUtils.copyPage(pageSource, UserVO.class);

        assertNotNull(pageTarget);
        assertEquals(100L, pageTarget.getTotal());
        assertEquals(1, pageTarget.getPageNum());
        assertEquals("1", pageTarget.getRecords().get(0).getId());
    }

    @Test
    public void testCopyNull() {
        assertNull(BeanCopyUtils.copy(null, UserVO.class));
        assertTrue(BeanCopyUtils.copyList(null, UserVO.class).isEmpty());
        assertNull(BeanCopyUtils.copyPage(null, UserVO.class));
    }
}
