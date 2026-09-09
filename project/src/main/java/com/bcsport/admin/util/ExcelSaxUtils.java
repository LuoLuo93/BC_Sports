package com.bcsport.admin.util;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.sax.Excel07SaxReader;
import cn.hutool.poi.excel.sax.handler.RowHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
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

    private ExcelSaxUtils() {
    }

    /** 单元格取字符串：越界/空/空白返回 null，去首尾空白。各导入服务原各自拷贝的同名私有方法收口于此 */
    public static String cellStr(List<Object> cells, Integer idx) {
        if (idx == null || idx < 0 || idx >= cells.size()) return null;
        Object v = cells.get(idx);
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    /**
     * 通过文件头魔数探测真实格式，拒绝伪 Excel（HTML/CSV 改扩展名）。
     * 返回 "xlsx" / "xls"，其余返回可直接拼进提示文案的描述。
     * xlsx: ZIP 头 PK\x03\x04；xls: OLE 复合文档头 D0CF11E0；其余多为 HTML/CSV/XML
     */
    public static String detectFormat(MultipartFile file) throws Exception {
        byte[] head = new byte[8];
        try (java.io.InputStream in = file.getInputStream()) {
            int read = in.read(head);
            if (read < 4) return "unknown(空文件)";
        }
        if ((head[0] & 0xFF) == 0x50 && (head[1] & 0xFF) == 0x4B) {
            return "xlsx";
        }
        if ((head[0] & 0xFF) == 0xD0 && (head[1] & 0xFF) == 0xCF
                && (head[2] & 0xFF) == 0x11 && (head[3] & 0xFF) == 0xE0) {
            return "xls";
        }
        String preview = new String(head, java.nio.charset.StandardCharsets.ISO_8859_1).trim();
        String lower = preview.toLowerCase();
        if (lower.startsWith("<") || preview.contains("<table") || preview.contains("<html")
                || preview.contains("<?xml")) {
            return "HTML/XML（伪Excel）";
        }
        if (lower.contains(",") || lower.contains("\t") || lower.contains(";")) {
            return "CSV/文本（伪Excel）";
        }
        return "未知格式";
    }

    /**
     * 读取全部 sheet（按工作簿顺序）。
     * 不能按"下标+1 拼 rId"逐个读：POI/hutool 生成的 xlsx 里 rId1 是 styles、rId2 是 sharedStrings，
     * sheet 从 rId3 起——按 rId1 读会把样式表当 sheet 解析出 0 行且不报错（系统下载的模板就是这类文件）。
     * hutool 的 rid=-1 走 XSSFReader 的 sheet 迭代器，与 rId 编号无关。
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
                    log.info("{} xlsx 共 {} 个 sheet", logLabel, pkg.getPartsByName(SHEET_PART_PATTERN).size());
                    new Excel07SaxReader(handler).read(pkg, -1);
                } finally {
                    pkg.revert();
                }
            } finally {
                if (!tempFile.delete()) {
                    tempFile.deleteOnExit();
                }
            }
        } else {
            ExcelUtil.readBySax(file.getInputStream(), -1, handler);
        }
    }
}
