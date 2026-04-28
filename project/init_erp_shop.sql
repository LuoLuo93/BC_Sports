-- ERP 店铺管理表结构初始化脚本
-- 适用于 Oracle 数据库

-- 删除已存在的表（如果存在）
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE bc_sports_sys_erp_shop CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;
/

-- 删除已存在的序列（如果存在）
BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE bc_sports_seq_erp_shop';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -2289 THEN
         RAISE;
      END IF;
END;
/

-- 创建 ERP 店铺表
CREATE SEQUENCE bc_sports_seq_erp_shop START WITH 100 INCREMENT BY 1;

CREATE TABLE bc_sports_sys_erp_shop (
    id VARCHAR2(64) NOT NULL,
    shop_code VARCHAR2(50) NOT NULL,
    shop_name VARCHAR2(100) NOT NULL,
    shop_type VARCHAR2(20) DEFAULT 'offline',
    province VARCHAR2(50),
    city VARCHAR2(50),
    district VARCHAR2(50),
    address VARCHAR2(200),
    contact_person VARCHAR2(50),
    contact_phone VARCHAR2(20),
    status NUMBER(1) DEFAULT 1,
    sort NUMBER(10) DEFAULT 0,
    remark VARCHAR2(500),
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0,
    CONSTRAINT bc_sports_pk_sys_erp_shop PRIMARY KEY (id),
    CONSTRAINT bc_sports_uk_sys_erp_shop_code UNIQUE (shop_code)
);

-- 添加表注释
COMMENT ON TABLE bc_sports_sys_erp_shop IS 'ERP 店铺管理表';
COMMENT ON COLUMN bc_sports_sys_erp_shop.id IS '店铺ID';
COMMENT ON COLUMN bc_sports_sys_erp_shop.shop_code IS '店铺编码';
COMMENT ON COLUMN bc_sports_sys_erp_shop.shop_name IS '店铺名称';
COMMENT ON COLUMN bc_sports_sys_erp_shop.shop_type IS '店铺类型：online-线上，offline-线下';
COMMENT ON COLUMN bc_sports_sys_erp_shop.province IS '省份';
COMMENT ON COLUMN bc_sports_sys_erp_shop.city IS '城市';
COMMENT ON COLUMN bc_sports_sys_erp_shop.district IS '区县';
COMMENT ON COLUMN bc_sports_sys_erp_shop.address IS '详细地址';
COMMENT ON COLUMN bc_sports_sys_erp_shop.contact_person IS '联系人';
COMMENT ON COLUMN bc_sports_sys_erp_shop.contact_phone IS '联系电话';
COMMENT ON COLUMN bc_sports_sys_erp_shop.status IS '状态：1-启用，0-禁用';
COMMENT ON COLUMN bc_sports_sys_erp_shop.sort IS '排序';
COMMENT ON COLUMN bc_sports_sys_erp_shop.remark IS '备注';
COMMENT ON COLUMN bc_sports_sys_erp_shop.create_time IS '创建时间';
COMMENT ON COLUMN bc_sports_sys_erp_shop.update_time IS '更新时间';
COMMENT ON COLUMN bc_sports_sys_erp_shop.create_by IS '创建者';
COMMENT ON COLUMN bc_sports_sys_erp_shop.update_by IS '更新者';
COMMENT ON COLUMN bc_sports_sys_erp_shop.deleted IS '删除标记：0-未删除，1-已删除';

-- 插入 BI_DIR 目录下的 ERP 店铺管理菜单（如果不存在）
-- 菜单类型: 0-目录, 1-菜单, 2-按钮

-- 主菜单
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, create_time, update_time, deleted)
SELECT 'BI_ERP_SHOP', 'BI_DIR', 'ERP 店铺管理', 'bi-shop', 1, '/bi/erp-shop', 'bi:erpShop:query', 10, 1, 1, 'ERP 店铺信息管理', SYSTIMESTAMP, SYSTIMESTAMP, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'BI_ERP_SHOP');

-- 新增按钮权限
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, create_time, update_time, deleted)
SELECT 'BI_ERP_SHOP_ADD', 'BI_ERP_SHOP', '店铺新增', NULL, 2, NULL, 'bi:erpShop:add', 0, 1, 0, SYSTIMESTAMP, SYSTIMESTAMP, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'BI_ERP_SHOP_ADD');

-- 修改按钮权限
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, create_time, update_time, deleted)
SELECT 'BI_ERP_SHOP_EDIT', 'BI_ERP_SHOP', '店铺修改', NULL, 2, NULL, 'bi:erpShop:edit', 0, 1, 0, SYSTIMESTAMP, SYSTIMESTAMP, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'BI_ERP_SHOP_EDIT');

-- 删除按钮权限
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, create_time, update_time, deleted)
SELECT 'BI_ERP_SHOP_DEL', 'BI_ERP_SHOP', '店铺删除', NULL, 2, NULL, 'bi:erpShop:delete', 0, 1, 0, SYSTIMESTAMP, SYSTIMESTAMP, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'BI_ERP_SHOP_DEL');

-- 为超级管理员角色分配 ERP 店铺管理菜单权限
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'BI_ERP_SHOP', SYSTIMESTAMP, 'admin'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'BI_ERP_SHOP');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'BI_ERP_SHOP_ADD', SYSTIMESTAMP, 'admin'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'BI_ERP_SHOP_ADD');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'BI_ERP_SHOP_EDIT', SYSTIMESTAMP, 'admin'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'BI_ERP_SHOP_EDIT');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'BI_ERP_SHOP_DEL', SYSTIMESTAMP, 'admin'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'BI_ERP_SHOP_DEL');

-- 插入示例数据
INSERT INTO bc_sports_sys_erp_shop (id, shop_code, shop_name, shop_type, province, city, district, address, contact_person, contact_phone, status, sort, remark, create_time, update_time, deleted)
VALUES ('1', 'SH001', '南京新街口旗舰店', 'offline', '江苏省', '南京市', '玄武区', '中山路1号', '张三', '13800138001', 1, 1, '主力店铺', SYSTIMESTAMP, SYSTIMESTAMP, 0);

INSERT INTO bc_sports_sys_erp_shop (id, shop_code, shop_name, shop_type, province, city, district, address, contact_person, contact_phone, status, sort, remark, create_time, update_time, deleted)
VALUES ('2', 'SH002', '上海陆家嘴店', 'offline', '上海市', '上海市', '浦东新区', '陆家嘴环路1000号', '李四', '13800138002', 1, 2, '高端商圈', SYSTIMESTAMP, SYSTIMESTAMP, 0);

INSERT INTO bc_sports_sys_erp_shop (id, shop_code, shop_name, shop_type, province, city, district, address, contact_person, contact_phone, status, sort, remark, create_time, update_time, deleted)
VALUES ('3', 'ON001', '天猫旗舰店', 'online', NULL, NULL, NULL, 'https://tmall.com/shop1', '王五', '13800138003', 1, 3, '电商平台', SYSTIMESTAMP, SYSTIMESTAMP, 0);

COMMIT;
