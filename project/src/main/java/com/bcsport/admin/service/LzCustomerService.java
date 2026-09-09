package com.bcsport.admin.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * 揽众客户押金资料服务（数据源：bjerp LZCUSTOMERINFOR）
 */
public interface LzCustomerService {

    /**
     * Excel 批量导入（以店铺代码为业务键，存在则更新，不存在则新增）
     *
     * @param file 上传的 Excel 文件（.xlsx/.xls）
     * @return total/success/fail/errors 导入统计
     * @throws IllegalArgumentException 文件为空或格式不支持
     * @throws IOException              文件读取失败
     */
    Map<String, Object> importExcel(MultipartFile file) throws IOException;
}
