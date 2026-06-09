package com.bcsport.admin.config.csrf;

import cn.hutool.json.JSONUtil;
import com.bcsport.admin.common.Result;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;

import java.io.IOException;
import java.util.Set;

/**
 * CSRF 过滤器
 *
 * 验证 POST/PUT/DELETE 请求的 CSRF Token
 */
@Slf4j
public class CsrfFilter implements Filter {

    /** CSRF Token 在 Session 中的属性名 */
    public static final String CSRF_TOKEN_ATTR = "CSRF_TOKEN";

    /** CSRF Token 请求头名称 */
    public static final String CSRF_TOKEN_HEADER = "X-CSRF-Token";

    /** 豁免的路径（不需要 CSRF 验证） */
    private static final Set<String> EXEMPT_PATHS = Set.of(
            "/doLogin",
            "/api/captcha",
            "/api/config/public"
    );

    /** 豁免的路径前缀 */
    private static final Set<String> EXEMPT_PREFIXES = Set.of(
            "/api/csrf"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod().toUpperCase();
        String uri = httpRequest.getRequestURI();

        // GET/HEAD/OPTIONS 请求豁免
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            chain.doFilter(request, response);
            return;
        }

        // 豁免路径
        if (isExempt(uri)) {
            chain.doFilter(request, response);
            return;
        }

        // 文件上传请求豁免（multipart/form-data）
        String contentType = httpRequest.getContentType();
        if (contentType != null && contentType.startsWith("multipart/")) {
            chain.doFilter(request, response);
            return;
        }

        // 获取 Shiro Session（避免使用 Servlet Session，防止 JSESSIONID Cookie 被覆盖）
        String sessionToken = null;
        try {
            Session session = SecurityUtils.getSubject().getSession(false);
            if (session != null) {
                sessionToken = (String) session.getAttribute(CSRF_TOKEN_ATTR);
            }
        } catch (Exception e) {
            log.debug("获取 Shiro Session 失败: {}", e.getMessage());
        }

        // 获取请求头中的 Token
        String requestToken = httpRequest.getHeader(CSRF_TOKEN_HEADER);

        // 如果 Session 中没有 Token，说明用户尚未获取 CSRF Token
        // 对于已登录用户的请求，要求必须携带有效的 CSRF Token
        if (sessionToken == null) {
            // 检查用户是否已登录（通过检查 Session 中是否有 Shiro 的认证信息）
            // 如果是已登录用户但没有 CSRF Token，要求先获取 Token
            if (requestToken != null) {
                // 有请求头但 Session 中没有 Token，说明 Token 已过期或无效
                log.warn("CSRF Token 已过期: uri={}, method={}", uri, method);
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.getWriter().write(JSONUtil.toJsonStr(Result.error(403, "CSRF Token 已过期，请刷新页面")));
                return;
            }
            // 没有请求头也没有 Session Token，跳过验证（可能是首次请求或未登录）
            chain.doFilter(request, response);
            return;
        }

        // Session 中有 Token，验证请求头中的 Token
        if (requestToken == null || !requestToken.equals(sessionToken)) {
            log.warn("CSRF Token 验证失败: uri={}, method={}, sessionToken={}, requestToken={}",
                    uri, method, maskToken(sessionToken), maskToken(requestToken));

            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write(JSONUtil.toJsonStr(Result.error(403, "CSRF Token 无效，请刷新页面")));
            return;
        }

        // Token 验证通过
        chain.doFilter(request, response);
    }

    /**
     * 掩码 Token，仅显示前4位和后4位，防止 Token 在日志中泄露
     */
    private String maskToken(String token) {
        if (token == null) {
            return "null";
        }
        if (token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }

    /**
     * 判断是否豁免该路径
     */
    private boolean isExempt(String uri) {
        // 检查精确匹配
        if (EXEMPT_PATHS.contains(uri)) {
            return true;
        }

        // 检查前缀匹配
        for (String prefix : EXEMPT_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("CSRF 过滤器初始化");
    }

    @Override
    public void destroy() {
        log.info("CSRF 过滤器销毁");
    }
}
