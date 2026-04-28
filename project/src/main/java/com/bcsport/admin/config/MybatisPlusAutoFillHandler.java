package com.bcsport.admin.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.bcsport.admin.util.ShiroSecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 */
@Slf4j
@Component
public class MybatisPlusAutoFillHandler implements MetaObjectHandler {
    
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("插入操作填充字段");
        LocalDateTime now = LocalDateTime.now();
        String currentUser = ShiroSecurityUtils.getCurrentUsername();
        String operator = (currentUser != null) ? currentUser : "unknown";

        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createBy", String.class, operator);
        this.strictInsertFill(metaObject, "updateBy", String.class, operator);
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("更新操作填充字段");
        LocalDateTime now = LocalDateTime.now();
        String currentUser = ShiroSecurityUtils.getCurrentUsername();
        String operator = (currentUser != null) ? currentUser : "unknown";

        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictUpdateFill(metaObject, "updateBy", String.class, operator);
    }
}
