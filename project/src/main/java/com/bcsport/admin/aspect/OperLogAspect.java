package com.bcsport.admin.aspect;

import com.bcsport.admin.annotation.OperLog;
import com.bcsport.admin.common.Result;
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
            // 方法正常返回不代表业务成功：登录失败等场景 catch 后 return Result.error，
            // 之前一律记 status=1 会把失败登录伪装成"成功"，按统一响应体 code 判定真实结果
            String bizError = resolveBizError(result);
            if (bizError != null) {
                sysLog.setStatus(0);
                sysLog.setErrorMsg(truncate(bizError));
            } else {
                sysLog.setStatus(1);
            }
            return result;
        } catch (Exception e) {
            sysLog.setStatus(0);
            sysLog.setErrorMsg(truncate(e.getMessage()));
            throw e;
        } finally {
            sysLog.setOperationTime(LocalDateTime.now());
            fillOperator(sysLog, point);
            log.info("[OperLog] 保存日志: module={}, operation={}, username={}", sysLog.getModule(), sysLog.getOperation(), sysLog.getUsername());
            sysLogService.saveLog(sysLog);
        }
    }

    /**
     * 返回体是统一响应 Result 且 code 非 200 时视为业务失败，返回其 message；成功返回 null
     */
    private String resolveBizError(Object result) {
        if (result instanceof Result) {
            Result<?> r = (Result<?>) result;
            if (r.getCode() == null || r.getCode() != 200) {
                return r.getMessage();
            }
        }
        return null;
    }

    private String truncate(String msg) {
        if (msg != null && msg.length() > 2000) {
            return msg.substring(0, 2000);
        }
        return msg;
    }

    /**
     * 补齐操作人：
     * 1) 登录操作：方法执行前用户未认证，成功登录后此处能重新取到
     * 2) 登录失败：始终未认证，取不到，回退到方法参数里的用户名（只调 getUsername，密码不落库）
     * 3) 兜底 anonymous
     */
    private void fillOperator(SysLog sysLog, ProceedingJoinPoint point) {
        if (sysLog.getUsername() != null && !sysLog.getUsername().isEmpty()) {
            return;
        }
        try {
            String username = ShiroSecurityUtils.getCurrentUsername();
            if (username != null && !username.isEmpty()) {
                sysLog.setUsername(username);
                sysLog.setUserId(ShiroSecurityUtils.getCurrentUserId());
                return;
            }
        } catch (Exception ignored) {
        }
        String argUsername = extractUsernameFromArgs(point.getArgs());
        sysLog.setUsername(argUsername != null ? argUsername : "anonymous");
    }

    private String extractUsernameFromArgs(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            try {
                Method getter = arg.getClass().getMethod("getUsername");
                if (getter.getReturnType() == String.class) {
                    Object value = getter.invoke(arg);
                    if (value instanceof String && !((String) value).isEmpty()) {
                        return (String) value;
                    }
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception ignored) {
            }
        }
        return null;
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
