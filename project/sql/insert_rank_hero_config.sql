-- 徒步值排名移动端顶部头图配置行（管理员代传自定义头图）
-- 执行库：BC_SPORTS 主库（192.168.5.177:1521/orcl，bc_sports）
-- 说明：SysConfigService.updateConfigs 只更新已有行不插新行，此行必须预插；
--       Oracle 空串即 NULL：未设置时 config_value 为 NULL（ConfigService.reload 已加固跳过 null 值行），
--       移动端/前端按空=默认蓝色渐变处理；上传后值为 /images/hero/rank_hero_{ts}.xxx
-- 回滚：DELETE FROM bc_sports_sys_config WHERE config_key = 'mobile.rankHeroUrl';
INSERT INTO bc_sports_sys_config (id, config_key, config_value, config_name, config_group, sort, remark, create_by, update_by)
VALUES ('CFG_RANK_HERO', 'mobile.rankHeroUrl', NULL, '徒步值排名头图', 'mobile', 1, '移动端/points顶部背景图,NULL=默认蓝色渐变,建议960x540横图', 'admin', 'admin');

-- 验证：
SELECT config_key, NVL(config_value, '<NULL=默认渐变>') FROM bc_sports_sys_config WHERE config_key = 'mobile.rankHeroUrl';
