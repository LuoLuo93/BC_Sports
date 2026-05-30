package com.bcsport.admin.aspect;

import com.bcsport.admin.annotation.OperLog;
import com.bcsport.admin.entity.SysLog;
import com.bcsport.admin.service.SysLogService;
import com.bcsport.admin.util.ShiroSecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
public class OperLogAspect {

    @Autowired
    private SysLogService sysLogService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Around("@annotation(com.bcsport.admin.annotation.OperLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        log.info("[OperLog] AOP 切面触发: {}", point.getSignature().toShortString());
        SysLog sysLog = buildLog(point);

        try {
            Object result = point.proceed();
            sysLog.setStatus(1);
            return result;
        } catch (Exception e) {
            sysLog.setStatus(0);
            String msg = e.getMessage();
            if (msg != null && msg.length() > 2000) {
                msg = msg.substring(0, 2000);
            }
            sysLog.setErrorMsg(msg);
            throw e;
        } finally {
            sysLog.setOperationTime(LocalDateTime.now());
            // 登录操作：方法执行前用户未认证，执行后才认证，需要重新获取
            if (sysLog.getUsername() == null || sysLog.getUsername().isEmpty()) {
                try {
                    String username = ShiroSecurityUtils.getCurrentUsername();
                    String userId = ShiroSecurityUtils.getCurrentUserId();
                    if (username != null) {
                        sysLog.setUsername(username);
                        sysLog.setUserId(userId);
                    }
                } catch (Exception ignored) {
                }
            }
            log.info("[OperLog] 保存日志: module={}, operation={}, username={}", sysLog.getModule(), sysLog.getOperation(), sysLog.getUsername());
            sysLogService.saveLog(sysLog);
        }
    }

    private SysLog buildLog(ProceedingJoinPoint point) {
        SysLog sysLog = new SysLog();

        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        OperLog annotation = method.getAnnotation(OperLog.class);

        sysLog.setModule(annotation.module());
        sysLog.setOperation(annotation.operation());

        String className = point.getTarget().getClass().getSimpleName();
        String methodName = method.getName();
        sysLog.setMethod(className + "." + methodName);

        if (annotation.saveParams()) {
            try {
                Object[] args = point.getArgs();
                if (args != null && args.length > 0) {
                    String params = OBJECT_MAPPER.writeValueAsString(args);
                    if (params.length() > 2000) {
                        params = params.substring(0, 2000);
                    }
                    sysLog.setParams(params);
                }
            } catch (Exception e) {
                sysLog.setParams("参数序列化失败");
            }
        }

        // Current user
        try {
            String username = ShiroSecurityUtils.getCurrentUsername();
            String userId = ShiroSecurityUtils.getCurrentUserId();
            sysLog.setUsername(username);
            sysLog.setUserId(userId);
        } catch (Exception e) {
            sysLog.setUsername("anonymous");
        }

        // Request IP
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                sysLog.setIp(getRemoteIp(request));
            }
        } catch (Exception ignored) {
        }

        return sysLog;
    }

    private String getRemoteIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
