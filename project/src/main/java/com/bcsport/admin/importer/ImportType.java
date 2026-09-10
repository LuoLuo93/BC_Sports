package com.bcsport.admin.importer;

/**
 * 导入模块标识：落库 BC_SPORTS_SYS_IMPORT_LOG.import_type 的值即枚举名。
 * 新增导入模块在此加一项即可，日志页按此过滤。
 */
public enum ImportType {

    BAS_FIRST_ADD("客户首次添加记录"),
    GOODS_OLD_NEW("货品新旧"),
    SALES_BUDGET("店铺日预算"),
    DW_SALES("数仓销售主明细"),
    ESTIMATED_COST("预估成本"),
    STICKER_DATA("贴纸货品数据"),
    STICKER_SIZE_GROUP("贴纸尺码组"),
    BCP_SPORT_POINTS("BC好玩家运动积分");

    private final String label;

    ImportType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getCode() {
        return name();
    }
}
