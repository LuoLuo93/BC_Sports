-- ============================================================
-- 会话 Cookie SameSite=STRICT 配置（CSRF 防护方案）
-- 表: bc_sports_sys_config
-- ============================================================
-- 背景:
--   原 CsrfFilter 的 FilterRegistrationBean URL 模式带了 context-path
--   前缀(/bcsports/api/*)，Servlet 过滤器匹配的是去掉 context-path 后
--   的应用内路径，因此该过滤器从未生效过(日志中仅有初始化记录、
--   无任何拦截记录)。
--
-- 处理决策(方案A):
--   1. 移除 CsrfFilter/CsrfFilterConfig 与 /api/csrf 端点及前端 token 逻辑;
--   2. CSRF 防护改由会话 Cookie 的 SameSite=STRICT 承担:
--      跨站请求(含跨站顶层导航)一律不携带 JSESSIONID，CSRF 无攻击面。
--   3. 代码默认值已同步改为 STRICT(ShiroConfig.sessionIdCookie)，
--      本脚本把配置显式落库，保证各环境一致、且可在管理界面查改。
--
-- 生效方式:
--   ConfigService 启动时全量加载 sys_config 进内存缓存，
--   执行本脚本后需重启服务生效(或在 系统管理-参数配置 中修改触发缓存刷新)。
--
-- 副作用说明:
--   STRICT 下从外部链接/邮件首次点入系统会显示未登录，站内刷新即可。
--   内部系统均为收藏夹/直连访问，无实际影响。
--
-- 幂等性: MERGE 按 config_key 判断，重复执行安全。
-- 创建时间: 2026-08-25
-- ============================================================

MERGE INTO bc_sports_sys_config t
USING (SELECT 'security.cookie.sameSite' AS config_key FROM dual) s
ON (t.config_key = s.config_key)
WHEN MATCHED THEN
  UPDATE SET t.config_value = 'STRICT',
             t.remark       = '会话Cookie SameSite策略: STRICT/LAX/NONE，CSRF防护依赖STRICT，勿改为NONE',
             t.update_time  = SYSTIMESTAMP,
             t.update_by    = 'admin'
WHEN NOT MATCHED THEN
  INSERT (id, config_key, config_value, config_name, config_group, sort, remark, create_by, update_by)
  VALUES ('CFG_COOKIE_SAMESITE', 'security.cookie.sameSite', 'STRICT',
          '会话Cookie SameSite策略', 'security', 8,
          'STRICT/LAX/NONE，CSRF防护依赖STRICT，勿改为NONE', 'admin', 'admin');

-- 验证
SELECT config_key, config_value, update_time
FROM bc_sports_sys_config
WHERE config_key = 'security.cookie.sameSite';
