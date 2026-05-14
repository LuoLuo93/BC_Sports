package com.bcsport.admin.config;

import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.apache.shiro.web.util.WebUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * SPA 认证过滤器
 * 对 API 请求返回 401 JSON，对页面请求重定向到 SPA 入口
 */
public class SpaAuthFilter extends FormAuthenticationFilter {

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest req = (HttpServletRequest) request;
        String uri = req.getRequestURI();

        // API 请求：返回 401 JSON
        if (uri.startsWith("/api/") || uri.equals("/doLogin") || uri.equals("/doLogout")) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或会话已过期\",\"data\":null,\"timestamp\":" + System.currentTimeMillis() + "}");
            return false;
        }

        // 静态资源请求：放行
        if (uri.startsWith("/assets/") || uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/")) {
            return true;
        }

        // 页面请求：重定向到 SPA 入口（Vue Router 处理前端路由）
        WebUtils.issueRedirect(request, response, "/");
        return false;
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        HttpServletRequest req = (HttpServletRequest) request;
        String uri = req.getRequestURI();

        // 静态资源放行
        if (uri.startsWith("/assets/") || uri.endsWith(".js") || uri.endsWith(".css")
                || uri.endsWith(".ico") || uri.endsWith(".png") || uri.endsWith(".jpg")
                || uri.endsWith(".svg") || uri.endsWith(".woff") || uri.endsWith(".woff2")
                || uri.endsWith(".ttf") || uri.endsWith(".eot")) {
            return true;
        }

        return super.isAccessAllowed(request, response, mappedValue);
    }
}
