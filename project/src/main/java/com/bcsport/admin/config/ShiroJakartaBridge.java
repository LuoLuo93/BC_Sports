package com.bcsport.admin.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.web.servlet.AbstractShiroFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Bridges Shiro's javax.servlet.Filter to Jakarta Servlet API for Spring Boot 3.x compatibility.
 * Shiro 1.x depends on javax.servlet, but Spring Boot 3.x only registers jakarta.servlet.Filter.
 */
public class ShiroJakartaBridge {

    public static FilterRegistrationBean<Filter> register(AbstractShiroFilter shiroFilter) {
        Filter jakartaFilter = new Filter() {
            @Override
            public void init(FilterConfig filterConfig) {
                // ShiroFilterFactoryBean already initializes the filter
            }

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
                    chain.doFilter(request, response);
                    return;
                }

                HttpServletRequest httpReq = (HttpServletRequest) request;
                HttpServletResponse httpResp = (HttpServletResponse) response;

                javax.servlet.http.HttpServletRequest javaxReq = wrapRequest(httpReq);
                javax.servlet.http.HttpServletResponse javaxResp = wrapResponse(httpResp);
                javax.servlet.FilterChain javaxChain = wrapChain(chain, request, response);

                try {
                    shiroFilter.doFilter(javaxReq, javaxResp, javaxChain);
                } catch (javax.servlet.ServletException e) {
                    throw new ServletException(e.getMessage(), e);
                }
            }

            @Override
            public void destroy() {
                shiroFilter.destroy();
            }
        };

        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(jakartaFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setEnabled(true);
        return registration;
    }

    private static javax.servlet.http.HttpServletRequest wrapRequest(HttpServletRequest req) {
        return (javax.servlet.http.HttpServletRequest) Proxy.newProxyInstance(
                javax.servlet.http.HttpServletRequest.class.getClassLoader(),
                new Class[]{javax.servlet.http.HttpServletRequest.class},
                new RequestInvocationHandler(req));
    }

    private static javax.servlet.http.HttpServletResponse wrapResponse(HttpServletResponse resp) {
        return (javax.servlet.http.HttpServletResponse) Proxy.newProxyInstance(
                javax.servlet.http.HttpServletResponse.class.getClassLoader(),
                new Class[]{javax.servlet.http.HttpServletResponse.class},
                new ResponseInvocationHandler(resp));
    }

    private static javax.servlet.FilterChain wrapChain(FilterChain chain,
                                                       ServletRequest jakartaRequest,
                                                       ServletResponse jakartaResponse) {
        return new javax.servlet.FilterChain() {
            @Override
            public void doFilter(javax.servlet.ServletRequest request, javax.servlet.ServletResponse response)
                    throws IOException, javax.servlet.ServletException {
                try {
                    chain.doFilter(jakartaRequest, jakartaResponse);
                } catch (ServletException e) {
                    throw new javax.servlet.ServletException(e.getMessage(), e);
                }
            }
        };
    }

    private static javax.servlet.http.Cookie toJavaxCookie(Cookie jc) {
        javax.servlet.http.Cookie cookie = new javax.servlet.http.Cookie(jc.getName(), jc.getValue());
        if (jc.getPath() != null) cookie.setPath(jc.getPath());
        if (jc.getDomain() != null) cookie.setDomain(jc.getDomain());
        cookie.setMaxAge(jc.getMaxAge());
        cookie.setHttpOnly(jc.isHttpOnly());
        cookie.setSecure(jc.getSecure());
        if (jc.getComment() != null) cookie.setComment(jc.getComment());
        return cookie;
    }

    private static Cookie toJakartaCookie(javax.servlet.http.Cookie jc) {
        Cookie cookie = new Cookie(jc.getName(), jc.getValue());
        if (jc.getPath() != null) cookie.setPath(jc.getPath());
        if (jc.getDomain() != null) cookie.setDomain(jc.getDomain());
        cookie.setMaxAge(jc.getMaxAge());
        cookie.setHttpOnly(jc.isHttpOnly());
        cookie.setSecure(jc.getSecure());
        return cookie;
    }

    private static class RequestInvocationHandler implements InvocationHandler {
        private final HttpServletRequest jakartaReq;

        RequestInvocationHandler(HttpServletRequest jakartaReq) {
            this.jakartaReq = jakartaReq;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();

            if ("getCookies".equals(name) && method.getParameterCount() == 0) {
                Cookie[] jc = jakartaReq.getCookies();
                if (jc == null) return null;
                javax.servlet.http.Cookie[] result = new javax.servlet.http.Cookie[jc.length];
                for (int i = 0; i < jc.length; i++) {
                    result[i] = toJavaxCookie(jc[i]);
                }
                return result;
            }

            if ("getSession".equals(name)) {
                return null;
            }

            if ("isRequestedSessionIdFromCookie".equals(name) && method.getParameterCount() == 0) {
                return jakartaReq.isRequestedSessionIdFromCookie();
            }

            if ("isRequestedSessionIdFromURL".equals(name) && method.getParameterCount() == 0) {
                return jakartaReq.isRequestedSessionIdFromURL();
            }

            // Default: delegate via reflection to jakarta request
            try {
                Method jakartaMethod = jakartaReq.getClass().getMethod(name, method.getParameterTypes());
                return jakartaMethod.invoke(jakartaReq, args);
            } catch (NoSuchMethodException e) {
                // Try interfaces
                for (Class<?> iface : jakartaReq.getClass().getInterfaces()) {
                    try {
                        Method m = iface.getMethod(name, method.getParameterTypes());
                        return m.invoke(jakartaReq, args);
                    } catch (NoSuchMethodException ignored) {
                    }
                }
                throw new UnsupportedOperationException("Unsupported method: " + name, e);
            }
        }
    }

    private static class ResponseInvocationHandler implements InvocationHandler {
        private final HttpServletResponse jakartaResp;

        ResponseInvocationHandler(HttpServletResponse jakartaResp) {
            this.jakartaResp = jakartaResp;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();

            if ("addCookie".equals(name) && method.getParameterCount() == 1
                    && method.getParameterTypes()[0] == javax.servlet.http.Cookie.class) {
                jakartaResp.addCookie(toJakartaCookie((javax.servlet.http.Cookie) args[0]));
                return null;
            }

            // Default: delegate via reflection to jakarta response
            try {
                Method jakartaMethod = jakartaResp.getClass().getMethod(name, method.getParameterTypes());
                return jakartaMethod.invoke(jakartaResp, args);
            } catch (NoSuchMethodException e) {
                for (Class<?> iface : jakartaResp.getClass().getInterfaces()) {
                    try {
                        Method m = iface.getMethod(name, method.getParameterTypes());
                        return m.invoke(jakartaResp, args);
                    } catch (NoSuchMethodException ignored) {
                    }
                }
                throw new UnsupportedOperationException("Unsupported method: " + name, e);
            }
        }
    }
}
