-- ============================================================
-- Agent 配置：将 agent.api-key 纳入系统配置表，支持界面管理
-- ============================================================

INSERT INTO bc_sports_sys_config (id, config_key, config_value, config_name, config_group, sort, remark, create_time, create_by, deleted)
VALUES ('CFG_AGENT_API_KEY', 'agent.api-key', 'local-dev-key', 'Agent API Key', 'agent', 1,
        'C# 客户端通过 X-API-Key 请求头携带；点击"重新生成"可轮换密钥',
        SYSTIMESTAMP, 'admin', 0);

COMMIT;
