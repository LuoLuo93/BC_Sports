-- ERP 仓库管理表结构初始化脚本
-- 适用于 Oracle 数据库

-- 删除已存在的表（如果存在）
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE bc_sports_sys_erp_warehouse CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;
/

-- 删除已存在的序列（如果存在）
BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE bc_sports_seq_erp_warehouse';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -2289 THEN
         RAISE;
      END IF;
END;
/

-- 创建 ERP 仓库表
CREATE SEQUENCE bc_sports_seq_erp_warehouse START WITH 100 INCREMENT BY 1;

CREATE TABLE bc_sports_sys_erp_warehouse (
    id VARCHAR2(64) NOT NULL,
    warehouse_code VARCHAR2(50) NOT NULL,
    warehouse_name VARCHAR2(100) NOT NULL,
    warehouse_type VARCHAR2(20) DEFAULT 'normal',
    province VARCHAR2(50),
    city VARCHAR2(50),
    district VARCHAR2(50),
    address VARCHAR2(200),
    manager VARCHAR2(50),
    contact_phone VARCHAR2(20),
    capacity NUMBER(10) DEFAULT 0,
    used_capacity NUMBER(10) DEFAULT 0,
    status NUMBER(1) DEFAULT 1,
    sort NUMBER(10) DEFAULT 0,
    remark VARCHAR2(500),
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0,
    CONSTRAINT bc_sports_pk_sys_erp_warehouse PRIMARY KEY (id),
    CONSTRAINT bc_sports_uk_sys_erp_warehouse_code UNIQUE (warehouse_code)
);

-- 添加表注释
COMMENT ON TABLE bc_sports_sys_erp_warehouse IS 'ERP 仓库管理表';
COMMENT ON COLUMN bc_sports_sys_erp_warehouse.id IS '仓库ID';
COMMENT ON COLUMN bc_sports_sys_erp_warehouse.warehouse_code IS '仓库编码';
COMMENT ON COLUMN bc_sports_sys_erp_warehouse.warehouse_name IS '仓库名称';
COMMENT ON COLUMN bc_sports_sys_erp_warehouse.warehouse_type IS '仓库类型：normal-普通仓，cold-冷链仓，bonded-保税仓';
COMMENT ON COLUMN bc_sports_sys_erp_warehouse.province IS '省份';
COMMENT ON COLUMN bc_sports_sys_erp_warehouse.city IS '城市';
COMMENT ON COLUMN bc_sports_sys_erp_warehouse.district IS '区县';
COMMENT ON COLUMN bc_sports_sys_erp_warehouse.address IS '详细地址';
COMMENT ON COLUMN bc_sports_sys_erp_warehouse.manager IS '仓库负责人';
COMMENT ON COLUMN bc_sports_sys_erp_warehouse.contact_phone IS '联系电话';
COMMENT ON COLUMN bc_sports_sys_erp_warehouse.capacity IS '仓库容量';
COMMENT ON COLUMN bc_sports_sys_erp_warehouse.used_capacity IS '已使用容量';
COMMENT ON COLUMN bc_sports_sys_erp_warehouse.status IS '状态：1-启用，0-禁用';
COMMENT ON COLUMN bc_sports_sys_erp_warehouse.sort IS '排序';
COMMENT ON COLUMN bc_sports_sys_erp_warehouse.remark IS '备注';
COMMENT ON COLUMN bc_sports_sys_erp_warehouse.create_time IS '创建时间';
COMMENT ON COLUMN bc_sports_sys_erp_warehouse.update_time IS '更新时间';
COMMENT ON COLUMN bc_sports_sys_erp_warehouse.create_by IS '创建者';
COMMENT ON COLUMN bc_sports_sys_erp_warehouse.update_by IS '更新者';
COMMENT ON COLUMN bc_sports_sys_erp_warehouse.deleted IS '删除标记：0-未删除，1-已删除';

-- 插入 BI_DIR 目录下的 ERP 仓库管理菜单（如果不存在）
-- 菜单类型: 0-目录, 1-菜单, 2-按钮

-- 主菜单
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, create_time, update_time, deleted)
SELECT 'BI_ERP_WAREHOUSE', 'BI_DIR', 'ERP 仓库管理', 'bi-box-seam', 1, '/bi/erp-warehouse', 'bi:erpWarehouse:query', 11, 1, 1, 'ERP 仓库信息管理', SYSTIMESTAMP, SYSTIMESTAMP, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'BI_ERP_WAREHOUSE');

-- 新增按钮权限
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, create_time, update_time, deleted)
SELECT 'BI_ERP_WAREHOUSE_ADD', 'BI_ERP_WAREHOUSE', '仓库新增', NULL, 2, NULL, 'bi:erpWarehouse:add', 0, 1, 0, SYSTIMESTAMP, SYSTIMESTAMP, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'BI_ERP_WAREHOUSE_ADD');

-- 修改按钮权限
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, create_time, update_time, deleted)
SELECT 'BI_ERP_WAREHOUSE_EDIT', 'BI_ERP_WAREHOUSE', '仓库修改', NULL, 2, NULL, 'bi:erpWarehouse:edit', 0, 1, 0, SYSTIMESTAMP, SYSTIMESTAMP, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'BI_ERP_WAREHOUSE_EDIT');

-- 删除按钮权限
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, create_time, update_time, deleted)
SELECT 'BI_ERP_WAREHOUSE_DEL', 'BI_ERP_WAREHOUSE', '仓库删除', NULL, 2, NULL, 'bi:erpWarehouse:delete', 0, 1, 0, SYSTIMESTAMP, SYSTIMESTAMP, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'BI_ERP_WAREHOUSE_DEL');

-- 为超级管理员角色分配 ERP 仓库管理菜单权限
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'BI_ERP_WAREHOUSE', SYSTIMESTAMP, 'admin'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'BI_ERP_WAREHOUSE');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'BI_ERP_WAREHOUSE_ADD', SYSTIMESTAMP, 'admin'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'BI_ERP_WAREHOUSE_ADD');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'BI_ERP_WAREHOUSE_EDIT', SYSTIMESTAMP, 'admin'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'BI_ERP_WAREHOUSE_EDIT');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'BI_ERP_WAREHOUSE_DEL', SYSTIMESTAMP, 'admin'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'BI_ERP_WAREHOUSE_DEL');

-- 插入示例数据
INSERT INTO bc_sports_sys_erp_warehouse (id, warehouse_code, warehouse_name, warehouse_type, province, city, district, address, manager, contact_phone, capacity, used_capacity, status, sort, remark, create_time, update_time, deleted)
VALUES ('1', 'WH001', '南京中央仓库', 'normal', '江苏省', '南京市', '江宁区', '秣周东路1号', '张三', '13800138001', 10000, 6500, 1, 1, '主仓库', SYSTIMESTAMP, SYSTIMESTAMP, 0);

INSERT INTO bc_sports_sys_erp_warehouse (id, warehouse_code, warehouse_name, warehouse_type, province, city, district, address, manager, contact_phone, capacity, used_capacity, status, sort, remark, create_time, update_time, deleted)
VALUES ('2', 'WH002', '上海冷链仓', 'cold', '上海市', '上海市', '浦东新区', '外高桥保税区', '李四', '13800138002', 5000, 3200, 1, 2, '冷链专用', SYSTIMESTAMP, SYSTIMESTAMP, 0);

INSERT INTO bc_sports_sys_erp_warehouse (id, warehouse_code, warehouse_name, warehouse_type, province, city, district, address, manager, contact_phone, capacity, used_capacity, status, sort, remark, create_time, update_time, deleted)
VALUES ('3', 'WH003', '杭州分仓', 'normal', '浙江省', '杭州市', '萧山区', '萧山国际机场货运区', '王五', '13800138003', 8000, 4100, 1, 3, '区域分仓', SYSTIMESTAMP, SYSTIMESTAMP, 0);

COMMIT;
