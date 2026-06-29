package com.bcsport.admin.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.springframework.context.annotation.Configuration;

/**
 * Apache POI 配置
 * 调高单记录字节数上限（默认 100MB），避免解析较大的 Excel 时触发 RecordFormatException
 */
@Slf4j
@Configuration
public class PoiConfig {

    /** 单记录最大字节数：300MB（未超过 int 上限 ~2.1GB） */
    private static final int BYTE_ARRAY_MAX_OVERRIDE = 300 * 1024 * 1024;

    @PostConstruct
    public void init() {
        IOUtils.setByteArrayMaxOverride(BYTE_ARRAY_MAX_OVERRIDE);
        log.info("Apache POI 单记录上限已设置为 {} MB", BYTE_ARRAY_MAX_OVERRIDE / 1024 / 1024);
    }
}
