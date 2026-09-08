package com.bcsport.admin.util;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.sax.Excel07SaxReader;
import cn.hutool.poi.excel.sax.handler.RowHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

/**
 * Excel SAX 流式读取公共入口（导入骨架公共化的第一步）。
 * 此前六个导入服务的 readAllSheets 是逐字拷贝，"修一处漏五处"的根源
 * （OPCPackage.open(InputStream) 的 OOM 问题就是只修了数仓销售一处的例证）。
 *
 * xlsx 必须先落临时文件再以 File 方式打开：OPCPackage.open(InputStream) 会把每个 zip entry
 * (1GB级 sheet XML)整块读进内存字节数组，触发 POI 单数组 300MB 上限；
 * File 方式走 ZipFile 流式读取，SAX 逐行解析，内存恒定不随文件大小增长。
 */
@Slf4j
public final class ExcelSaxUtils {

    private static final Pattern SHEET_PART_PATTERN = Pattern.compile("/xl/worksheets/.*\\.xml");

    /** xls 逐 sheet 尝试的尝试上限（越界异常即停止） */
    private static final int XLS_MAX_SHEET_TRY = 20;

    private ExcelSaxUtils() {
    }

    /**
     * 读取全部 sheet：xlsx 复用同一个 OPCPackage；xls 逐个尝试到越界。
     *
     * @param logLabel 日志里的模块名（如 "Bas_FirstAdd"），仅用于日志区分
     */
    public static void readAllSheets(MultipartFile file, String format, RowHandler handler, String logLabel)
            throws Exception {
        if ("xlsx".equals(format)) {
            File tempFile = File.createTempFile("excel-import-", ".xlsx");
            try {
                Files.copy(file.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                org.apache.poi.openxml4j.opc.OPCPackage pkg = org.apache.poi.openxml4j.opc.OPCPackage.open(
                        tempFile.getAbsolutePath(), org.apache.poi.openxml4j.opc.PackageAccess.READ);
                try {
                    int sheetCount = pkg.getPartsByName(SHEET_PART_PATTERN).size();
                    log.info("{} xlsx 共 {} 个 sheet", logLabel, sheetCount);
                    Excel07SaxReader saxReader = new Excel07SaxReader(handler);
                    for (int s = 0; s < sheetCount; s++) {
                        saxReader.read(pkg, s);
                    }
                } finally {
                    pkg.revert();
                }
            } finally {
                if (!tempFile.delete()) {
                    tempFile.deleteOnExit();
                }
            }
        } else {
            for (int s = 0; s < XLS_MAX_SHEET_TRY; s++) {
                try {
                    ExcelUtil.readBySax(file.getInputStream(), s, handler);
                } catch (Exception e) {
                    break;
                }
            }
        }
    }
}
