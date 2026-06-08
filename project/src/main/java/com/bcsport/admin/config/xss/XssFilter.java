package com.bcsport.admin.config.xss;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * XSS 过滤器
 *
 * 拦截所有 API 请求，对参数和请求体进行 XSS 清洗
 */
@Slf4j
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 包装请求，进行 XSS 清洗
        XssHttpServletRequestWrapper xssRequest = new XssHttpServletRequestWrapper(httpRequest);
        chain.doFilter(xssRequest, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("XSS 过滤器初始化");
    }

    @Override
    public void destroy() {
        log.info("XSS 过滤器销毁");
    }
}
