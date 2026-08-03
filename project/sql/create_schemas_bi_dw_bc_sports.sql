-- =====================================================================
-- 在 192.168.5.177:1521/ORCL (Oracle 19c 非CDB) 上创建两套数据环境
--   BI_DW      : BI 数仓   (表空间 BI_DW_DATA)
--   BC_SPORTS  : 业务表    (表空间 BC_SPORTS_DATA)
--
-- 设计: 双 schema + 双表空间(物理+逻辑双重隔离)
-- 字符集: AL32UTF8 (UTF-8, 中文无忧)
-- 用 SYSDBA 执行: sqlplus sys/oracle@192.168.5.177:1521/ORCL as sysdba @本文件
-- =====================================================================
WHENEVER SQLERROR EXIT SQL.SQLCODE;
SET DEFINE OFF;

PROMPT >>> 1/5 创建表空间 (数据文件放 /u01/app/oracle/oradata/ORCL/)
-- BI 数仓表空间: 起始 500M, 自动扩展, 每次扩 100M, 不设上限
CREATE TABLESPACE bi_dw_data
    DATAFILE '/u01/app/oracle/oradata/ORCL/bi_dw_data01.dbf'
    SIZE 500M
    AUTOEXTEND ON NEXT 100M MAXSIZE UNLIMITED
    EXTENT MANAGEMENT LOCAL AUTOALLOCATE
    SEGMENT SPACE MANAGEMENT AUTO;

-- 业务表表空间: 起始 200M, 自动扩展
CREATE TABLESPACE bc_sports_data
    DATAFILE '/u01/app/oracle/oradata/ORCL/bc_sports_data01.dbf'
    SIZE 200M
    AUTOEXTEND ON NEXT 50M MAXSIZE UNLIMITED
    EXTENT MANAGEMENT LOCAL AUTOALLOCATE
    SEGMENT SPACE MANAGEMENT AUTO;

PROMPT >>> 2/5 创建用户 BI_DW (BI 数仓)
-- profile=DEFAULT 但把密码有效期/锁定策略放宽, 避免业务被默认 180 天到期锁住
CREATE USER bi_dw IDENTIFIED BY "123456"
    DEFAULT TABLESPACE bi_dw_data
    TEMPORARY TABLESPACE temp
    PROFILE default
    ACCOUNT UNLOCK;
ALTER USER bi_dw DEFAULT ROLE ALL;
ALTER USER bi_dw QUOTA UNLIMITED ON bi_dw_data;

PROMPT >>> 3/5 创建用户 BC_SPORTS (业务表)
CREATE USER bc_sports IDENTIFIED BY "123456"
    DEFAULT TABLESPACE bc_sports_data
    TEMPORARY TABLESPACE temp
    PROFILE default
    ACCOUNT UNLOCK;
ALTER USER bc_sports DEFAULT ROLE ALL;
ALTER USER bc_sports QUOTA UNLIMITED ON bc_sports_data;

PROMPT >>> 4/5 授权
-- BI_DW: 数仓角色, 可建表/视图/物化视图/过程, 可查字典
GRANT CREATE SESSION, UNLIMITED TABLESPACE TO bi_dw;
GRANT CREATE TABLE, CREATE VIEW, CREATE MATERIALIZED VIEW TO bi_dw;
GRANT CREATE SEQUENCE, CREATE SYNONYM, CREATE PROCEDURE, CREATE TRIGGER TO bi_dw;
GRANT CREATE TYPE, CREATE JOB TO bi_dw;
-- 便于数仓跨 schema 抽数, 允许查询数据字典(只读, 不给 DBA)
GRANT SELECT ANY DICTIONARY TO bi_dw;

-- BC_SPORTS: 业务表角色, 标准 DDL 权限
GRANT CREATE SESSION, UNLIMITED TABLESPACE TO bc_sports;
GRANT CREATE TABLE, CREATE VIEW, CREATE SEQUENCE, CREATE SYNONYM TO bc_sports;
GRANT CREATE PROCEDURE, CREATE TRIGGER, CREATE TYPE, CREATE JOB TO bc_sports;

-- 互相不可见(默认就如此, 这里只是显式不授权)

PROMPT >>> 5/5 密码策略放宽(可选但推荐, 避免默认 180 天到期锁号)
ALTER PROFILE default LIMIT PASSWORD_LIFE_TIME UNLIMITED;
ALTER PROFILE default LIMIT FAILED_LOGIN_ATTEMPTS UNLIMITED;

PROMPT >>> 完成。连接测试串:
PROMPT     BI数仓:  sqlplus bi_dw/123456@192.168.5.177:1521/ORCL
PROMPT     业务表:  sqlplus bc_sports/123456@192.168.5.177:1521/ORCL
EXIT;
