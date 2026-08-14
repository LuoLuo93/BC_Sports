-- ============================================================
-- 品牌表新增「品牌编码」列
-- 背景：前端 Management.vue 品牌弹窗早已存在 brandCode 字段，
--      但后端 Entity/DTO/VO 与数据库表此前均缺失，导致前端提交被丢弃、
--      编辑时该字段显示上一次的残留值（疑似缓存，实为字段缺失 + 表单未重置）。
-- 注意：不加唯一约束；历史行该列为 NULL，允许空，不影响。
-- ============================================================

ALTER TABLE BC_SPORTS_SYS_BRAND ADD (BRAND_CODE VARCHAR2(50));

COMMENT ON COLUMN BC_SPORTS_SYS_BRAND.BRAND_CODE IS '品牌编码';
