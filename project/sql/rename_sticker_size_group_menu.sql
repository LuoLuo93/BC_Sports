-- ============================================================
-- 贴纸打印 - 菜单改名：「本地尺码组维护」→「矫正尺码组维护」
-- 说明：仅更新已存在的菜单行，不影响按钮权限行
-- ============================================================

UPDATE BC_SPORTS_SYS_MENU
   SET MENU_NAME    = '矫正尺码组维护',
       DESCRIPTION = '矫正尺码组维护(按品牌+类别隔离)',
       UPDATE_TIME  = SYSTIMESTAMP,
       UPDATE_BY    = 'admin'
 WHERE ID = 'STICKER_SIZE_GROUP';

COMMIT;
