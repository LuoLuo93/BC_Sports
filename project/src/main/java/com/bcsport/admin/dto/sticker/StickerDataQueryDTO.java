package com.bcsport.admin.dto.sticker;

import lombok.Data;

/**
 * 贴纸资料查询传输对象
 */
@Data
public class StickerDataQueryDTO {

    private String materialNumber;

    private String styleNumber;

    private String materialName;

    private String brandId;
}
