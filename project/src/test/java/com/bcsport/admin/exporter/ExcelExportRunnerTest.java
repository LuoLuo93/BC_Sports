package com.bcsport.admin.exporter;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 导出骨架语义测试 + 连正式库的全量冒烟。
 * 冒烟用 -Dexport.smoke.db=true 触发（默认跳过，避免常规 test 误连生产 ERP）。
 */
public class ExcelExportRunnerTest {

    private final ExcelExportRunner runner = new ExcelExportRunner();

    /** 列头与取值与 StickerDataExportService 对齐的最小版本 */
    private static List<ExportColumn<Map<String, Object>>> miniColumns() {
        return List.of(
                ExportColumn.of("货号", r -> str(r.get("MATERIAL_NUMBER"))),
                ExportColumn.of("价格", r -> r.get("PRICE"))
        );
    }

    private static String str(Object v) {
        return v == null ? null : v.toString();
    }

    static List<Map<String, Object>> rows(int n) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("MATERIAL_NUMBER", "NLM" + i);
            r.put("PRICE", i * 1.5);
            list.add(r);
        }
        return list;
    }

    /** 内存版规格：fetch 语义 = 返回 [offset, offset+limit) */
    static class MemSpec implements ExcelExportSpec<Map<String, Object>> {
        final List<Map<String, Object>> data;
        final int batchSize;
        final long maxRows;

        MemSpec(List<Map<String, Object>> data, int batchSize, long maxRows) {
            this.data = data;
            this.batchSize = batchSize;
            this.maxRows = maxRows;
        }

        @Override
        public String logLabel() {
            return "测试导出";
        }

        @Override
        public long count() {
            return data.size();
        }

        @Override
        public List<Map<String, Object>> fetch(long offset, int limit) {
            int from = (int) Math.min(offset, data.size());
            int to = (int) Math.min(offset + limit, data.size());
            return data.subList(from, to);
        }

        @Override
        public List<ExportColumn<Map<String, Object>>> columns() {
            return miniColumns();
        }

        @Override
        public int batchSize() {
            return batchSize;
        }

        @Override
        public long maxRows() {
            return maxRows;
        }
    }

    @Test
    public void 正常导出_表头与数据行写出() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long written = runner.write(new MemSpec(rows(205), 50, 100_000), out);

        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Sheet sheet = book.getSheetAt(0);
            assertEquals(206, sheet.getPhysicalNumberOfRows(), "1 表头 + 205 数据行");
            assertEquals(written, 205);
            Row header = sheet.getRow(0);
            assertEquals("货号", header.getCell(0).getStringCellValue());
            assertEquals("价格", header.getCell(1).getStringCellValue());
            Row first = sheet.getRow(1);
            assertEquals("NLM1", first.getCell(0).getStringCellValue());
            assertEquals(1.5, first.getCell(1).getNumericCellValue(), 1e-9);
            Row last = sheet.getRow(205);
            assertEquals("NLM205", last.getCell(0).getStringCellValue());
        }
    }

    @Test
    public void 空数据_仅表头() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long written = runner.write(new MemSpec(rows(0), 50, 100_000), out);
        assertEquals(0, written);
        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertEquals(1, book.getSheetAt(0).getPhysicalNumberOfRows());
        }
    }

    @Test
    public void 行数超熔断上限_拒绝() {
        assertThrows(IllegalArgumentException.class,
                () -> runner.write(new MemSpec(rows(100), 50, 99), new ByteArrayOutputStream()));
    }

    @Test
    public void 导出中途数据被删_按实际取到的收尾不死循环() throws Exception {
        // fetch 永远返回空（模拟 count 后数据被清空）
        ExcelExportSpec<Map<String, Object>> spec = new MemSpec(rows(10), 5, 100_000) {
            @Override
            public List<Map<String, Object>> fetch(long offset, int limit) {
                return new ArrayList<>();
            }
        };
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long written = runner.write(spec, out);
        assertEquals(0, written);
    }

    /**
     * 连生产 ERP(10.0.1.22) 的全量导出冒烟：真实 count + ROWNUM 分批取数 + 流式写 5.8w 行,
     * 验证 SQL 可跑、MADEIN 列存在、生成耗时与文件读回行数。
     * 只读查询。-Dexport.smoke.db=true 触发。
     */
    @Test
    @EnabledIfSystemProperty(named = "export.smoke.db", matches = "true")
    public void 正式库全量导出冒烟() throws Exception {
        String url = "jdbc:oracle:thin:@//10.0.1.22:1521/orcl?oracle.net.CONNECT_TIMEOUT=10000";
        String sql = """
                SELECT paged.*,
                       (SELECT LISTAGG(c.VALUE2_CODE, ',') WITHIN GROUP (ORDER BY c.VALUE2_CODE)
                        FROM M_PRODUCT_ALIAS a
                        LEFT JOIN M_ATTRIBUTESETINSTANCE c ON a.M_ATTRIBUTESETINSTANCE_ID = c.id
                        WHERE a.M_PRODUCT_ID = paged.PRODUCT_ID AND c.VALUE2_CODE IS NOT NULL) AS SIZES
                FROM (
                    SELECT TMP.*, ROW_NUMBER() OVER (ORDER BY product_name) AS RN FROM (
                        SELECT product.ID AS PRODUCT_ID,
                               product.name AS PRODUCT_NAME,
                               product.name AS MATERIAL_NUMBER,
                               product.VALUE AS MATERIAL_NAME,
                               product.STYLE_NO AS STYLE_NUMBER,
                               product.M_DIM1_ID AS BRAND_ID,
                               brand.ATTRIBNAME AS BRAND_NAME,
                               product.M_DIM4_ID AS KIND_ID,
                               Kind.ATTRIBNAME AS KIND_NAME,
                               product.colorsalias AS COLOR,
                               product.PRICELIST AS PRICE,
                               product.THEME_NAME AS EXECUTION_STANDARD,
                               product.INTSCODE AS EAN13,
                               product.MADEIN AS MADEIN,
                               product.FABCODE AS FAB_CODE,
                               product.FABELEMENT AS FAB_ELEMENT,
                               product.ACCODE AS AC_CODE,
                               product.ACCELEMENT AS ACC_ELEMENT,
                               product.SAFETY_CATEGORY AS SAFETY_CATEGORY,
                               product.BOX_QTY_NEW AS SIZE_GROUP_ID
                        FROM M_PRODUCT product
                        LEFT JOIN M_DIM brand ON product.M_DIM1_ID = brand.ID
                        LEFT JOIN M_DIM Kind ON product.M_DIM4_ID = Kind.ID
                    ) TMP
                ) paged
                WHERE RN > ? AND RN <= ? + ?
                """;

        try (Connection conn = DriverManager.getConnection(url, "bosnds3", "abc123")) {
            long total;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM M_PRODUCT");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                total = rs.getLong(1);
            }
            assertTrue(total > 0, "M_PRODUCT 应有数据");

            ExcelExportSpec<Map<String, Object>> spec = new ExcelExportSpec<>() {
                @Override
                public String logLabel() {
                    return "贴纸资料导出冒烟";
                }

                @Override
                public long count() {
                    return total;
                }

                @Override
                public List<Map<String, Object>> fetch(long offset, int limit) {
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setLong(1, offset);
                        ps.setLong(2, offset);
                        ps.setInt(3, limit);
                        try (ResultSet rs = ps.executeQuery()) {
                            List<Map<String, Object>> batch = new ArrayList<>(limit);
                            int cols = rs.getMetaData().getColumnCount();
                            while (rs.next()) {
                                Map<String, Object> row = new LinkedHashMap<>();
                                for (int i = 1; i <= cols; i++) {
                                    row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                                }
                                batch.add(row);
                            }
                            return batch;
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public List<ExportColumn<Map<String, Object>>> columns() {
                    return List.of(
                            ExportColumn.of("货号", r -> str(r.get("MATERIAL_NUMBER"))),
                            ExportColumn.of("款号", r -> str(r.get("STYLE_NUMBER"))),
                            ExportColumn.of("货品名称", r -> str(r.get("MATERIAL_NAME"))),
                            ExportColumn.of("品牌", r -> str(r.get("BRAND_NAME"))),
                            ExportColumn.of("类别", r -> str(r.get("KIND_NAME"))),
                            ExportColumn.of("颜色", r -> str(r.get("COLOR"))),
                            ExportColumn.of("价格", r -> r.get("PRICE")),
                            ExportColumn.of("执行标准", r -> str(r.get("EXECUTION_STANDARD"))),
                            ExportColumn.of("EAN13", r -> str(r.get("EAN13"))),
                            ExportColumn.of("产地", r -> str(r.get("MADEIN"))),
                            ExportColumn.of("安全类别", r -> str(r.get("SAFETY_CATEGORY"))),
                            ExportColumn.of("面料成分1", r -> str(r.get("FAB_CODE"))),
                            ExportColumn.of("面料成分2", r -> str(r.get("FAB_ELEMENT"))),
                            ExportColumn.of("辅料成分1", r -> str(r.get("AC_CODE"))),
                            ExportColumn.of("辅料成分2", r -> str(r.get("ACC_ELEMENT"))),
                            ExportColumn.of("尺码组", r -> str(r.get("SIZES")))
                    );
                }
            };

            Path out = Path.of("target", "tmp_sticker_export_smoke.xlsx");
            long start = System.currentTimeMillis();
            long written;
            try (java.io.OutputStream os = Files.newOutputStream(out)) {
                written = runner.write(spec, os);
            }
            long cost = System.currentTimeMillis() - start;

            // 读回校验：总行数=表头+数据，首行有货号
            try (InputStream is = Files.newInputStream(out);
                 XSSFWorkbook book = new XSSFWorkbook(is)) {
                Sheet sheet = book.getSheetAt(0);
                assertEquals(total + 1, sheet.getPhysicalNumberOfRows(), "全量行数(含表头)");
                assertNotNull(sheet.getRow(1).getCell(0), "首行货号非空");
                assertEquals(written, total);
            }
            Files.deleteIfExists(out);

            System.out.println("全量导出冒烟: total=" + total + " 行, 耗时 " + cost + "ms, 内存="
                    + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024 + "MB");
        }
    }
}
