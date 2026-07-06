package com.bcsport.admin.entity.hkerp;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 店铺-仓库-运动城 映射（VO，从 Bas_Shop 表按 ShopName 查询）
 * <p>
 * 对应源项目 shopStockSportCity 实体。
 */
@Data
@TableName("Bas_Shop")
public class HkShopStockSportCity implements Serializable {
    private static final long serialVersionUID = 1L;

    private String shopId;
    private String stockId;
    private String sportCityId;
}
