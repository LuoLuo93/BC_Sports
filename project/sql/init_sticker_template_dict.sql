-- ============================================================
-- 贴纸打印模板 字典类型 + 字典数据
-- 作为打印模板名的唯一数据源，品牌模板关系和字段映射都从此字典取下拉
-- 幂等：重复执行不会重复插入
-- ============================================================

-- 1. 字典类型：sticker_template
INSERT INTO bc_sports_sys_dict_type (id, dict_name, dict_type, status, remark, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), '贴纸打印模板', 'sticker_template', 1, 'BarTender .btw 模板文件名清单',
       SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_type WHERE dict_type = 'sticker_template' AND deleted = 0);

-- 2. 字典数据：45 个模板文件
INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', '1006.btw', '1006.btw', 1, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = '1006.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'FZ_80X48_1L5C.btw', 'FZ_80X48_1L5C.btw', 2, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'FZ_80X48_1L5C.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'FZ_80X48_1L9C.btw', 'FZ_80X48_1L9C.btw', 3, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'FZ_80X48_1L9C.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'FZ_80X48_2L5C.btw', 'FZ_80X48_2L5C.btw', 4, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'FZ_80X48_2L5C.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'FZ_80X48_2L9C.btw', 'FZ_80X48_2L9C.btw', 5, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'FZ_80X48_2L9C.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'FZ_Shoes_80X48_1L5C.btw', 'FZ_Shoes_80X48_1L5C.btw', 6, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'FZ_Shoes_80X48_1L5C.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'LK_5.5X5.5.btw', 'LK_5.5X5.5.btw', 7, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'LK_5.5X5.5.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'LK_6X6.btw', 'LK_6X6.btw', 8, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'LK_6X6.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'LOWA_Shoes_5.5X5.5_01.btw', 'LOWA_Shoes_5.5X5.5_01.btw', 9, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'LOWA_Shoes_5.5X5.5_01.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'LOWA_Shoes_5.5X5.5_02.btw', 'LOWA_Shoes_5.5X5.5_02.btw', 10, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'LOWA_Shoes_5.5X5.5_02.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'LOWA_Shoes_6X6_01.btw', 'LOWA_Shoes_6X6_01.btw', 11, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'LOWA_Shoes_6X6_01.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'LOWA_Shoes_6X6_02.btw', 'LOWA_Shoes_6X6_02.btw', 12, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'LOWA_Shoes_6X6_02.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'LW_FJ_5.5X5.5_01.btw', 'LW_FJ_5.5X5.5_01.btw', 13, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'LW_FJ_5.5X5.5_01.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'LW_FJ_5.5X5.5_02.btw', 'LW_FJ_5.5X5.5_02.btw', 14, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'LW_FJ_5.5X5.5_02.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NLKIDS_SHOES.btw', 'NLKIDS_SHOES.btw', 15, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NLKIDS_SHOES.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_10X6.btw', 'NL_10X6.btw', 16, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_10X6.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_10X6_1L5C.btw', 'NL_10X6_1L5C.btw', 17, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_10X6_1L5C.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_10X6_1L9C.btw', 'NL_10X6_1L9C.btw', 18, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_10X6_1L9C.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_10X6_2L5C.btw', 'NL_10X6_2L5C.btw', 19, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_10X6_2L5C.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_10X6_2L9C.btw', 'NL_10X6_2L9C.btw', 20, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_10X6_2L9C.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_85X55_1L5C.btw', 'NL_85X55_1L5C.btw', 21, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_85X55_1L5C.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_85X55_1L9C.btw', 'NL_85X55_1L9C.btw', 22, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_85X55_1L9C.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_85X55_2L5C.btw', 'NL_85X55_2L5C.btw', 23, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_85X55_2L5C.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_85X55_2L9C.btw', 'NL_85X55_2L9C.btw', 24, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_85X55_2L9C.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_FJSH_5.5X5.5_01.btw', 'NL_FJSH_5.5X5.5_01.btw', 25, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_FJSH_5.5X5.5_01.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_FJ_5.5X5.5_01.btw', 'NL_FJ_5.5X5.5_01.btw', 26, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_FJ_5.5X5.5_01.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_FJ_5.5X5.5_02.btw', 'NL_FJ_5.5X5.5_02.btw', 27, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_FJ_5.5X5.5_02.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_FJ_6X6_01.btw', 'NL_FJ_6X6_01.btw', 28, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_FJ_6X6_01.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_FJ_6X6_02.btw', 'NL_FJ_6X6_02.btw', 29, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_FJ_6X6_02.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_SHOES_5.5X5.5_01.btw', 'NL_SHOES_5.5X5.5_01.btw', 30, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_SHOES_5.5X5.5_01.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_SHOES_5.5X5.5_02.btw', 'NL_SHOES_5.5X5.5_02.btw', 31, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_SHOES_5.5X5.5_02.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_SHOES_6X6_01.btw', 'NL_SHOES_6X6_01.btw', 32, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_SHOES_6X6_01.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_SHOES_6X6_02.btw', 'NL_SHOES_6X6_02.btw', 33, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_SHOES_6X6_02.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_YR_10X6.btw', 'NL_YR_10X6.btw', 34, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_YR_10X6.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_Z_5.5X5.5_01.btw', 'NL_Z_5.5X5.5_01.btw', 35, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_Z_5.5X5.5_01.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_Z_5.5X5.5_02.btw', 'NL_Z_5.5X5.5_02.btw', 36, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_Z_5.5X5.5_02.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_Z_6X6_01.btw', 'NL_Z_6X6_01.btw', 37, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_Z_6X6_01.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'NL_Z_6X6_02.btw', 'NL_Z_6X6_02.btw', 38, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'NL_Z_6X6_02.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'POC_5.5X5.5.btw', 'POC_5.5X5.5.btw', 39, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'POC_5.5X5.5.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'WZ_55X55.btw', 'WZ_55X55.btw', 40, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'WZ_55X55.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'print_head.btw', 'print_head.btw', 41, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'print_head.btw' AND deleted = 0);

INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_template', 'print_head_6X6.btw', 'print_head_6X6.btw', 42, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_template' AND dict_value = 'print_head_6X6.btw' AND deleted = 0);
