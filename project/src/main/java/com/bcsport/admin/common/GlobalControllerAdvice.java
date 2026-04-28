package com.bcsport.admin.common;

import com.bcsport.admin.entity.User;
import com.bcsport.admin.service.MenuService;
import com.bcsport.admin.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * 全局控制器通知
 * 用于在所有 @Controller 控制器的 Model 中注入公共属性 */
@Slf4j
@ControllerAdvice(annotations = Controller.class)
public class GlobalControllerAdvice {

    @Autowired
    private UserService userService;

    @Autowired
    private MenuService menuService;

    /**
     * 在执行控制器方法前，往 Model 中注入当前用户信息
     */
    @ModelAttribute
    public void addCommonAttributes(Model model) {
        Subject subject = SecurityUtils.getSubject();

        // 仅对已认证用户处理
        if (subject.isAuthenticated()) {
            String username = (String) subject.getPrincipal();
            if (username != null) {
                User user = userService.getByUsername(username);
                if (user != null) {
                    List<String> permissions = menuService.getPermissionsByUserId(user.getId());

                    model.addAttribute("username", username);
                    model.addAttribute("nickname", user.getNickname());
                    model.addAttribute("permissions", permissions);

                    // 为个人中心等页面提供 user 对象
                    model.addAttribute("user", user);
                    model.addAttribute("loginUser", user);

                    log.debug("成功为用户{} 注入全局 Model 属性", username);
                }
            }
        }
    }
}
