-- ============================================================
-- PRINT_FIELD_MAPPING 表增加 DEFAULT_VALUE 字段
-- 用于模板级固定值（如标题、检验员等不从货品资料取的字段）
-- ============================================================

ALTER TABLE PRINT_FIELD_MAPPING ADD (
    DEFAULT_VALUE VARCHAR2(500)
);

COMMENT ON COLUMN PRINT_FIELD_MAPPING.DEFAULT_VALUE IS '默认值（固定值）：DB_FIELD 为空时直接使用此值；DB_FIELD 取值为空时回退到此值';
