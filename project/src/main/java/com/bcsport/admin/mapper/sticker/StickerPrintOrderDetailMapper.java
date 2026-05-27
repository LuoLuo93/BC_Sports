package com.bcsport.admin.mapper.sticker;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.sticker.StickerPrintOrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StickerPrintOrderDetailMapper extends BaseMapper<StickerPrintOrderDetail> {
    void batchInsert(@Param("list") List<StickerPrintOrderDetail> list);
}
