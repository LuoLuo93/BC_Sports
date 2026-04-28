package com.bcsport.admin.service;

import com.bcsport.admin.entity.Menu;
import com.bcsport.admin.mapper.MenuMapper;
import com.bcsport.admin.service.impl.MenuServiceImpl;
import com.bcsport.admin.vo.MenuVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MenuRefactorTest {

    @Mock
    private MenuMapper menuMapper;

    @InjectMocks
    private MenuServiceImpl menuService;

    @Test
    public void testMenuTreeStructure() {
        // Prepare flat list of entities
        Menu m1 = new Menu();
        m1.setId("1");
        m1.setParentId("0");
        m1.setMenuName("System");
        m1.setStatus(1);
        m1.setSort(1);

        Menu m1_1 = new Menu();
        m1_1.setId("2");
        m1_1.setParentId("1");
        m1_1.setMenuName("User Management");
        m1_1.setStatus(1);
        m1_1.setSort(1);

        when(menuMapper.selectList(any())).thenReturn(Arrays.asList(m1, m1_1));

        List<MenuVO> tree = menuService.getMenuTree();

        assertNotNull(tree);
        assertEquals(1, tree.size(), "Root should have only 1 node");
        MenuVO root = tree.get(0);
        assertEquals("System", root.getMenuName());
        
        assertNotNull(root.getChildren());
        assertEquals(1, root.getChildren().size(), "System should have 1 child");
        assertEquals("User Management", root.getChildren().get(0).getMenuName());
    }
}
