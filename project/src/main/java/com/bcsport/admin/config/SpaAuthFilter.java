package com.bcsport.admin.config;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * SPA 认证过滤器
 * 对 API 请求返回 401 JSON，对页面请求重定向到 SPA 入口
 */
public class SpaAuthFilter extends FormAuthenticationFilter {

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String uri = req.getRequestURI();

        // API 请求：返回 401 JSON
        if (uri.startsWith("/api/") || uri.equals("/doLogin") || uri.equals("/doLogout")
                || uri.contains("/api/")) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":401,\"message\":\"未登录或会话过期\",\"data\":null,\"timestamp\":" + System.currentTimeMillis() + "}");
            return false;
        }

        // 静态资源请求：放行(构建产物目录；prod 下 URI 带 /bcsports 前缀，用 contains 兼容)
        if (uri.contains("/assets/") || uri.contains("/css/") || uri.contains("/js/")) {
            return true;
        }
        // 注意 /images/** 是上传的业务文件，不在匿名放行之列，落到下方登录/AJAX/重定向逻辑

        // AJAX 请求：返回 401 JSON
        String xRequestedWith = req.getHeader("X-Requested-With");
        String accept = req.getHeader("Accept");
        if ("XMLHttpRequest".equals(xRequestedWith)
                || (accept != null && accept.contains("application/json"))) {
            resp.setContentType("application/json;charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"code\":401,\"message\":\"未登录或会话已过期\",\"data\":null}");
            return false;
        }

        // 页面请求：重定向到登录页
        resp.setStatus(HttpServletResponse.SC_FOUND);
        resp.setHeader("Location", req.getContextPath() + "/login");
        return false;
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        HttpServletRequest req = (HttpServletRequest) request;
        String uri = req.getRequestURI();

        // 静态资源放行(扩展名白名单)。这是 prod(context-path=/bcsports)下未登录加载
        // 登录页 JS/CSS 的主通道，不能整体删除；但 /images/ 上传目录虽以图片扩展名结尾
        // 也属业务数据，必须显式排除，否则任何 .png 结尾的 URL 都构成认证旁路
        if (!uri.contains("/images/")
                && (uri.startsWith("/assets/") || uri.endsWith(".js") || uri.endsWith(".css")
                || uri.endsWith(".ico") || uri.endsWith(".png") || uri.endsWith(".jpg")
                || uri.endsWith(".avif") || uri.endsWith(".webp")
                || uri.endsWith(".svg") || uri.endsWith(".woff") || uri.endsWith(".woff2")
                || uri.endsWith(".ttf") || uri.endsWith(".eot"))) {
            return true;
        }

        // 支持 Remember-Me：已记住的用户视为已认证
        Subject subject = getSubject(request, response);
        if (subject != null && subject.isRemembered()) {
            return true;
        }

        return super.isAccessAllowed(request, response, mappedValue);
    }
}
