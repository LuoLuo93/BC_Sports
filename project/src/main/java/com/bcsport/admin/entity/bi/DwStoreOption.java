package com.bcsport.admin.entity.bi;

import lombok.Data;

import java.io.Serializable;

/**
 * 数仓库店仓下拉选项（BI_DW.ODS_C_STORE，主客户 c_customer_id=1 全部店仓）
 */
@Data
public class DwStoreOption implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 店仓ID */
    private Long storeId;

    /** 店仓编码 */
    private String storeCode;

    /** 店仓名称 */
    private String storeName;
}
