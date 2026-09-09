package com.bcsport.admin.importer;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 引擎语义固化测试：每条断言对应方案文档 §7 的一条行为不变量。
 */
public class ExcelImportRunnerTest {

    record Person(String id, String name) {
    }

    /** 可调参数的测试 Spec，onBatch 只收集不落库 */
    static class PersonSpec implements ExcelImportSpec<Person> {
        final List<List<Person>> batches = new ArrayList<>();
        int batchSize = 500;
        int maxRows = 2_000_000;
        boolean dedup = false;
        boolean failBatch = false;
        boolean throwRuntime = false;
        boolean throwNumberFormat = false;

        @Override
        public String logLabel() {
            return "Test";
        }

        @Override
        public ImportType type() {
            return ImportType.BAS_FIRST_ADD;
        }

        @Override
        public Map<String, String> headerAlias() {
            return Map.of("客户id", "id", "姓名", "name");
        }

        @Override
        public Person mapRow(RowCtx ctx) {
            if (throwRuntime) throw new RuntimeException("boom");
            if (throwNumberFormat) throw new NumberFormatException("For input string: \"abc\"");
            String id = ctx.str("id", 0);
            if (id != null && id.startsWith("skip")) return null;
            if (id == null) throw new IllegalArgumentException("客户id不能为空");
            return new Person(id, ctx.str("name", 1));
        }

        @Override
        public String dedupKey(Person p) {
            return dedup ? p.id() : null;
        }

        @Override
        public int batchSize() {
            return batchSize;
        }

        @Override
        public int maxRows() {
            return maxRows;
        }

        @Override
        public void onBatch(List<Person> batch, BatchCtx ctx) {
            if (failBatch) throw new RuntimeException("db down");
            batches.add(new ArrayList<>(batch));
            ctx.success(batch.size());
        }
    }

    static class CapturingRecorder extends ImportLogRecorder {
        final List<ImportType> types = new ArrayList<>();
        final List<ImportOutcome> outcomes = new ArrayList<>();

        CapturingRecorder() {
            super(null);
        }

        @Override
        public void record(ImportType type, ImportOutcome outcome, MultipartFile file) {
            types.add(type);
            outcomes.add(outcome);
        }
    }

    private static final String[] HEADER = {"客户id", "姓名"};

    private static MultipartFile xlsx(String[] header, List<String[]> rows) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("data");
            int r = 0;
            if (header != null) {
                Row h = sheet.createRow(r++);
                for (int i = 0; i < header.length; i++) h.createCell(i).setCellValue(header[i]);
            }
            for (String[] row : rows) {
                Row x = sheet.createRow(r++);
                for (int i = 0; i < row.length; i++) {
                    if (row[i] != null) x.createCell(i).setCellValue(row[i]);
                }
            }
            wb.write(out);
            return new MockMultipartFile("file", "t.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    private static List<String[]> rows(String[]... rows) {
        return Arrays.asList(rows);
    }

    @Test
    void headerMappedRowsAreImportedInBatchesAndResultMapKeepsKeyOrder() throws Exception {
        PersonSpec spec = new PersonSpec();
        spec.batchSize = 2;
        CapturingRecorder recorder = new CapturingRecorder();

        ImportOutcome outcome = new ExcelImportRunner(recorder).run(xlsx(HEADER, rows(
                new String[]{"a", "A"}, new String[]{"b", "B"}, new String[]{"c", "C"},
                new String[]{"d", "D"}, new String[]{"e", "E"})), spec);

        assertEquals(5, outcome.getTotal());
        assertEquals(5, outcome.getSuccess());
        assertEquals(0, outcome.getFail());
        assertEquals(ImportOutcome.STATUS_SUCCESS, outcome.getStatus());
        assertTrue(outcome.getErrors().isEmpty());
        assertEquals(List.of(2, 2, 1), spec.batches.stream().map(List::size).toList());
        assertEquals("a", spec.batches.get(0).get(0).id());
        assertEquals("A", spec.batches.get(0).get(0).name());

        assertEquals(List.of("total", "success", "fail", "errors"), new ArrayList<>(outcome.toResultMap().keySet()));
        assertEquals(List.of(ImportType.BAS_FIRST_ADD), recorder.types);
        assertSame(outcome, recorder.outcomes.get(0));
    }

    @Test
    void dedupKeepsLastRowWithinUnflushedBufferAndCountsRawRowsInTotal() throws Exception {
        PersonSpec spec = new PersonSpec();
        spec.dedup = true;

        ImportOutcome outcome = new ExcelImportRunner(new CapturingRecorder()).run(xlsx(HEADER, rows(
                new String[]{"a", "first"}, new String[]{"b", "B"}, new String[]{"a", "last"})), spec);

        assertEquals(1, spec.batches.size());
        List<Person> batch = spec.batches.get(0);
        assertEquals(2, batch.size());
        assertEquals("b", batch.get(0).id());
        assertEquals("a", batch.get(1).id());
        assertEquals("last", batch.get(1).name());
        // 与现状一致：total 计原始行数，success 计入库行数，被去重覆盖的行体现在 fail 里但不产生错误文案
        assertEquals(3, outcome.getTotal());
        assertEquals(2, outcome.getSuccess());
        assertEquals(1, outcome.getFail());
        assertEquals(ImportOutcome.STATUS_PARTIAL, outcome.getStatus());
        assertTrue(outcome.getErrors().isEmpty());
    }

    @Test
    void illegalArgumentBecomesBusinessMessageAndOtherExceptionBecomesParseError() throws Exception {
        PersonSpec spec = new PersonSpec();
        ImportOutcome outcome = new ExcelImportRunner(new CapturingRecorder()).run(xlsx(HEADER, rows(
                new String[]{"a", "A"}, new String[]{"", "no-id"})), spec);
        assertEquals(List.of("第3行：客户id不能为空"), outcome.getErrors());
        assertEquals(2, outcome.getTotal());
        assertEquals(1, outcome.getSuccess());
        assertEquals(1, outcome.getFail());
        assertEquals(ImportOutcome.STATUS_PARTIAL, outcome.getStatus());

        PersonSpec boom = new PersonSpec();
        boom.throwRuntime = true;
        ImportOutcome outcome2 = new ExcelImportRunner(new CapturingRecorder()).run(
                xlsx(HEADER, rows(new String[]{"a", "A"})), boom);
        assertEquals(List.of("第2行：解析异常 - boom"), outcome2.getErrors());

        // NumberFormatException 是 IllegalArgumentException 的子类，但必须走"解析异常"文案，不能裸露 JDK 信息
        PersonSpec nfe = new PersonSpec();
        nfe.throwNumberFormat = true;
        ImportOutcome outcome3 = new ExcelImportRunner(new CapturingRecorder()).run(
                xlsx(HEADER, rows(new String[]{"a", "A"})), nfe);
        assertEquals(List.of("第2行：解析异常 - For input string: \"abc\""), outcome3.getErrors());
    }

    @Test
    void nonExcelIsRejectedBeforeReadingAndStillRecordsFailedLog() throws Exception {
        PersonSpec spec = new PersonSpec();
        CapturingRecorder recorder = new CapturingRecorder();
        MultipartFile html = new MockMultipartFile("file", "fake.xlsx", "text/html",
                "<html><table><tr><td>1</td></tr></table></html>".getBytes(StandardCharsets.UTF_8));

        ImportOutcome outcome = new ExcelImportRunner(recorder).run(html, spec);

        assertEquals(0, outcome.getTotal());
        assertEquals(ImportOutcome.STATUS_FAILED, outcome.getStatus());
        assertEquals(1, outcome.getErrors().size());
        assertTrue(outcome.getErrors().get(0).startsWith("文件不是标准的 Excel 格式（检测为 HTML/XML（伪Excel））"));
        assertTrue(spec.batches.isEmpty());
        assertEquals(1, recorder.outcomes.size());
        assertEquals(ImportOutcome.STATUS_FAILED, recorder.outcomes.get(0).getStatus());
    }

    @Test
    void batchFailureIsRecordedOnceAndRowsCountAsFailed() throws Exception {
        PersonSpec spec = new PersonSpec();
        spec.failBatch = true;

        ImportOutcome outcome = new ExcelImportRunner(new CapturingRecorder()).run(xlsx(HEADER, rows(
                new String[]{"a", "A"}, new String[]{"b", "B"})), spec);

        assertEquals(List.of("批量入库失败: db down"), outcome.getErrors());
        assertEquals(2, outcome.getTotal());
        assertEquals(0, outcome.getSuccess());
        assertEquals(2, outcome.getFail());
        assertEquals(ImportOutcome.STATUS_PARTIAL, outcome.getStatus());
    }

    @Test
    void rowErrorsAreCappedAt100AndSummaryLineAppended() throws Exception {
        PersonSpec spec = new PersonSpec();
        List<String[]> bad = new ArrayList<>();
        for (int i = 0; i < 150; i++) bad.add(new String[]{"", "x" + i});

        ImportOutcome outcome = new ExcelImportRunner(new CapturingRecorder()).run(xlsx(HEADER, bad), spec);

        assertEquals(150, outcome.getTotal());
        assertEquals(0, outcome.getSuccess());
        assertEquals(150, outcome.getFail());
        assertEquals(101, outcome.getErrors().size());
        assertEquals("第2行：客户id不能为空", outcome.getErrors().get(0));
        assertEquals("...共 150 条未导入，仅显示前 100 条", outcome.getErrors().get(100));
    }

    @Test
    void rowsBeyondMaxRowsAreCountedButNotParsed() throws Exception {
        PersonSpec spec = new PersonSpec();
        spec.maxRows = 2;

        ImportOutcome outcome = new ExcelImportRunner(new CapturingRecorder()).run(xlsx(HEADER, rows(
                new String[]{"a", "A"}, new String[]{"b", "B"}, new String[]{"c", "C"}, new String[]{"d", "D"})), spec);

        assertEquals(4, outcome.getTotal());
        assertEquals(2, outcome.getSuccess());
        assertEquals(2, outcome.getFail());
        assertEquals(List.of("数据超过 2 行上限，已停止处理", "数据超过 2 行上限，已停止处理"), outcome.getErrors());
        assertEquals(1, spec.batches.size());
        assertEquals(2, spec.batches.get(0).size());
    }

    @Test
    void unrecognizedHeaderFallsBackToFixedColumnOrder() throws Exception {
        PersonSpec spec = new PersonSpec();

        ImportOutcome outcome = new ExcelImportRunner(new CapturingRecorder()).run(xlsx(
                new String[]{"x", "y"}, rows(new String[]{"id-1", "Name-1"})), spec);

        assertEquals(1, outcome.getSuccess());
        assertEquals("id-1", spec.batches.get(0).get(0).id());
        assertEquals("Name-1", spec.batches.get(0).get(0).name());
    }

    @Test
    void nullFromMapRowSkipsRowWithoutCounting() throws Exception {
        PersonSpec spec = new PersonSpec();

        ImportOutcome outcome = new ExcelImportRunner(new CapturingRecorder()).run(xlsx(HEADER, rows(
                new String[]{"skip-1", "S"}, new String[]{"a", "A"})), spec);

        assertEquals(1, outcome.getTotal());
        assertEquals(1, outcome.getSuccess());
        assertEquals(ImportOutcome.STATUS_SUCCESS, outcome.getStatus());
    }

    /** POI 生成的 xlsx 里 sheet 的 rId 从 3 起（rId1 styles / rId2 sharedStrings），必须按工作簿顺序而非拼 rId 读 */
    @Test
    void allSheetsAreReadInWorkbookOrderAndFirstRecognizedHeaderWins() throws Exception {
        PersonSpec spec = new PersonSpec();
        byte[] bytes;
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet summary = wb.createSheet("汇总");
            Row sh = summary.createRow(0);
            sh.createCell(0).setCellValue("客户id");
            sh.createCell(1).setCellValue("姓名");
            Row sr = summary.createRow(1);
            sr.createCell(0).setCellValue("s1");
            sr.createCell(1).setCellValue("S1");
            XSSFSheet detail = wb.createSheet("明细");
            Row dh = detail.createRow(0);
            dh.createCell(0).setCellValue("客户id");
            dh.createCell(1).setCellValue("姓名");
            Row dr = detail.createRow(1);
            dr.createCell(0).setCellValue("d1");
            dr.createCell(1).setCellValue("D1");
            wb.write(out);
            bytes = out.toByteArray();
        }

        ImportOutcome outcome = new ExcelImportRunner(new CapturingRecorder()).run(
                new MockMultipartFile("file", "t.xlsx", "application/octet-stream", bytes), spec);

        assertEquals(2, outcome.getTotal());
        assertEquals(2, outcome.getSuccess());
        assertEquals(List.of("s1", "d1"), spec.batches.get(0).stream().map(Person::id).toList());
    }

    @Test
    void xlsIsReadThroughSaxPathToo() throws Exception {
        PersonSpec spec = new PersonSpec();
        byte[] bytes;
        try (HSSFWorkbook wb = new HSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("data");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("客户id");
            h.createCell(1).setCellValue("姓名");
            Row r = sheet.createRow(1);
            r.createCell(0).setCellValue("x1");
            r.createCell(1).setCellValue("X1");
            wb.write(out);
            bytes = out.toByteArray();
        }

        ImportOutcome outcome = new ExcelImportRunner(new CapturingRecorder()).run(
                new MockMultipartFile("file", "t.xls", "application/vnd.ms-excel", bytes), spec);

        assertEquals(1, outcome.getSuccess());
        assertEquals("x1", spec.batches.get(0).get(0).id());
        assertEquals("X1", spec.batches.get(0).get(0).name());
    }
}
