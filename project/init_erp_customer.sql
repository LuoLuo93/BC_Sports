-- ERP 客户管理表结构初始化脚本
-- 适用于 Oracle 数据库

-- 删除已存在的表（如果存在）
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE bc_sports_sys_erp_customer CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;
/

-- 删除已存在的序列（如果存在）
BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE bc_sports_seq_erp_customer';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -2289 THEN
         RAISE;
      END IF;
END;
/

-- 创建 ERP 客户表
CREATE SEQUENCE bc_sports_seq_erp_customer START WITH 100 INCREMENT BY 1;

CREATE TABLE bc_sports_sys_erp_customer (
    id VARCHAR2(64) NOT NULL,
    customer_code VARCHAR2(50) NOT NULL,
    customer_name VARCHAR2(100) NOT NULL,
    customer_type VARCHAR2(20) DEFAULT 'enterprise',
    province VARCHAR2(50),
    city VARCHAR2(50),
    district VARCHAR2(50),
    address VARCHAR2(200),
    contact_person VARCHAR2(50),
    contact_phone VARCHAR2(20),
    email VARCHAR2(100),
    credit_limit NUMBER(15,2) DEFAULT 0,
    credit_period NUMBER(5) DEFAULT 0,
    status NUMBER(1) DEFAULT 1,
    sort NUMBER(10) DEFAULT 0,
    remark VARCHAR2(500),
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP,
    create_by VARCHAR2(64),
    update_by VARCHAR2(64),
    deleted NUMBER(1) DEFAULT 0,
    CONSTRAINT bc_sports_pk_sys_erp_customer PRIMARY KEY (id),
    CONSTRAINT bc_sports_uk_sys_erp_customer_code UNIQUE (customer_code)
);

-- 添加表注释
COMMENT ON TABLE bc_sports_sys_erp_customer IS 'ERP 客户管理表';
COMMENT ON COLUMN bc_sports_sys_erp_customer.id IS '客户ID';
COMMENT ON COLUMN bc_sports_sys_erp_customer.customer_code IS '客户编码';
COMMENT ON COLUMN bc_sports_sys_erp_customer.customer_name IS '客户名称';
COMMENT ON COLUMN bc_sports_sys_erp_customer.customer_type IS '客户类型：enterprise-企业客户，individual-个人客户，dealer-经销商';
COMMENT ON COLUMN bc_sports_sys_erp_customer.province IS '省份';
COMMENT ON COLUMN bc_sports_sys_erp_customer.city IS '城市';
COMMENT ON COLUMN bc_sports_sys_erp_customer.district IS '区县';
COMMENT ON COLUMN bc_sports_sys_erp_customer.address IS '详细地址';
COMMENT ON COLUMN bc_sports_sys_erp_customer.contact_person IS '联系人';
COMMENT ON COLUMN bc_sports_sys_erp_customer.contact_phone IS '联系电话';
COMMENT ON COLUMN bc_sports_sys_erp_customer.email IS '电子邮箱';
COMMENT ON COLUMN bc_sports_sys_erp_customer.credit_limit IS '信用额度';
COMMENT ON COLUMN bc_sports_sys_erp_customer.credit_period IS '信用账期（天）';
COMMENT ON COLUMN bc_sports_sys_erp_customer.status IS '状态：1-启用，0-禁用';
COMMENT ON COLUMN bc_sports_sys_erp_customer.sort IS '排序';
COMMENT ON COLUMN bc_sports_sys_erp_customer.remark IS '备注';
COMMENT ON COLUMN bc_sports_sys_erp_customer.create_time IS '创建时间';
COMMENT ON COLUMN bc_sports_sys_erp_customer.update_time IS '更新时间';
COMMENT ON COLUMN bc_sports_sys_erp_customer.create_by IS '创建者';
COMMENT ON COLUMN bc_sports_sys_erp_customer.update_by IS '更新者';
COMMENT ON COLUMN bc_sports_sys_erp_customer.deleted IS '删除标记：0-未删除，1-已删除';

-- 插入 BI_DIR 目录下的 ERP 客户管理菜单（如果不存在）
-- 菜单类型: 0-目录, 1-菜单, 2-按钮

-- 主菜单
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, description, create_time, update_time, deleted)
SELECT 'BI_ERP_CUSTOMER', 'BI_DIR', 'ERP 客户管理', 'bi-people', 1, '/bi/erp-customer', 'bi:erpCustomer:query', 12, 1, 1, 'ERP 客户信息管理', SYSTIMESTAMP, SYSTIMESTAMP, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'BI_ERP_CUSTOMER');

-- 新增按钮权限
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, create_time, update_time, deleted)
SELECT 'BI_ERP_CUSTOMER_ADD', 'BI_ERP_CUSTOMER', '客户新增', NULL, 2, NULL, 'bi:erpCustomer:add', 0, 1, 0, SYSTIMESTAMP, SYSTIMESTAMP, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'BI_ERP_CUSTOMER_ADD');

-- 修改按钮权限
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, create_time, update_time, deleted)
SELECT 'BI_ERP_CUSTOMER_EDIT', 'BI_ERP_CUSTOMER', '客户修改', NULL, 2, NULL, 'bi:erpCustomer:edit', 0, 1, 0, SYSTIMESTAMP, SYSTIMESTAMP, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'BI_ERP_CUSTOMER_EDIT');

-- 删除按钮权限
INSERT INTO bc_sports_sys_menu (id, parent_id, menu_name, icon, menu_type, path, permission, sort, status, visible, create_time, update_time, deleted)
SELECT 'BI_ERP_CUSTOMER_DEL', 'BI_ERP_CUSTOMER', '客户删除', NULL, 2, NULL, 'bi:erpCustomer:delete', 0, 1, 0, SYSTIMESTAMP, SYSTIMESTAMP, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_menu WHERE id = 'BI_ERP_CUSTOMER_DEL');

-- 为超级管理员角色分配 ERP 客户管理菜单权限
INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'BI_ERP_CUSTOMER', SYSTIMESTAMP, 'admin'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'BI_ERP_CUSTOMER');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'BI_ERP_CUSTOMER_ADD', SYSTIMESTAMP, 'admin'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'BI_ERP_CUSTOMER_ADD');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'BI_ERP_CUSTOMER_EDIT', SYSTIMESTAMP, 'admin'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'BI_ERP_CUSTOMER_EDIT');

INSERT INTO bc_sports_sys_role_menu (id, role_id, menu_id, create_time, create_by)
SELECT SYS_GUID(), '1', 'BI_ERP_CUSTOMER_DEL', SYSTIMESTAMP, 'admin'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM bc_sports_sys_role_menu WHERE role_id = '1' AND menu_id = 'BI_ERP_CUSTOMER_DEL');

-- 插入示例数据
INSERT INTO bc_sports_sys_erp_customer (id, customer_code, customer_name, customer_type, province, city, district, address, contact_person, contact_phone, email, credit_limit, credit_period, status, sort, remark, create_time, update_time, deleted)
VALUES ('1', 'C001', '南京体育用品有限公司', 'enterprise', '江苏省', '南京市', '鼓楼区', '中山路100号', '张经理', '13800138001', 'zhang@njty.com', 1000000.00, 30, 1, 1, 'VIP客户', SYSTIMESTAMP, SYSTIMESTAMP, 0);

INSERT INTO bc_sports_sys_erp_customer (id, customer_code, customer_name, customer_type, province, city, district, address, contact_person, contact_phone, email, credit_limit, credit_period, status, sort, remark, create_time, update_time, deleted)
VALUES ('2', 'C002', '上海运动器材经销商', 'dealer', '上海市', '上海市', '静安区', '南京西路500号', '李总', '13800138002', 'li@shyd.com', 500000.00, 15, 1, 2, '一级经销商', SYSTIMESTAMP, SYSTIMESTAMP, 0);

INSERT INTO bc_sports_sys_erp_customer (id, customer_code, customer_name, customer_type, province, city, district, address, contact_person, contact_phone, email, credit_limit, credit_period, status, sort, remark, create_time, update_time, deleted)
VALUES ('3', 'C003', '杭州户外俱乐部', 'individual', '浙江省', '杭州市', '西湖区', '文三路200号', '王先生', '13800138003', 'wang@hzhw.com', 50000.00, 7, 1, 3, '团体客户', SYSTIMESTAMP, SYSTIMESTAMP, 0);

COMMIT;
