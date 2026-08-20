-- ============================================================
-- 首张打印模板 字典类型 + 字典数据
-- 下发打印时,若申请单填写了联系人/联系电话/收货地址,
-- 先用此字典第一条启用项的模板打印一张"首张"(收货信息),再打印贴纸明细。
-- 打印机沿用第一张明细的品牌模板匹配,保证同机先后出纸。
-- 幂等:重复执行不会重复插入
-- 如需换模板(如 print_head_6X6.btw),在管理后台「字典管理」里改即可,无需动库
-- ============================================================

-- 1. 字典类型:sticker_first_template
INSERT INTO bc_sports_sys_dict_type (id, dict_name, dict_type, status, remark, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), '贴纸首张打印模板', 'sticker_first_template', 1, '首张(收货信息)标签模板,取第一条启用项',
       SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_type WHERE dict_type = 'sticker_first_template' AND deleted = 0);

-- 2. 字典数据:默认 print_head.btw(sticker_template 字典中已有该模板)
INSERT INTO bc_sports_sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, create_time, update_time, create_by, update_by, deleted)
SELECT SYS_GUID(), 'sticker_first_template', 'print_head.btw', 'print_head.btw', 1, 1, SYSTIMESTAMP, SYSTIMESTAMP, 'admin', 'admin', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_dict_data WHERE dict_type = 'sticker_first_template' AND dict_value = 'print_head.btw' AND deleted = 0);

COMMIT;
