package com.bcsport.admin.service.sticker;

import com.bcsport.admin.dto.sticker.StickerDataQueryDTO;
import com.bcsport.admin.exporter.ExcelExportRunner;
import com.bcsport.admin.exporter.ExcelExportSpec;
import com.bcsport.admin.exporter.ExportColumn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 贴纸资料 Excel 导出（与导入侧 StickerDataImportService 对称）：本类只声明导出规格，
 * 分批捞数/流式写/行数熔断的流程骨架在 ExcelExportRunner。
 * 查询条件与列表页共用，全部为空 = 全量导出。
 */
@Service
public class StickerDataExportService {

    @Autowired
    private StickerPrintService stickerPrintService;

    @Autowired
    private ExcelExportRunner excelExportRunner;

    /**
     * 导出到输出流，返回实际写出数据行数（不含表头）。
     * 列与列表页一致；可编辑列的列名与导入模板对齐（货号/执行标准/EAN13/产地/安全类别/
     * 面料成分1、2/辅料成分1、2），导出的文件删掉只读列、改完可直接作为导入文件回传。
     */
    public long exportTo(StickerDataQueryDTO queryDTO, OutputStream out) throws IOException {
        String materialNumber = queryDTO.getMaterialNumber();
        String kindId = queryDTO.getKindId();
        String materialName = queryDTO.getMaterialName();
        String brandId = queryDTO.getBrandId();

        ExcelExportSpec<Map<String, Object>> spec = new ExcelExportSpec<>() {
            @Override
            public String logLabel() {
                return "贴纸资料导出";
            }

            @Override
            public long count() {
                return stickerPrintService.countProductsForExport(materialNumber, kindId, materialName, brandId);
            }

            @Override
            public List<Map<String, Object>> fetch(long offset, int limit) {
                return stickerPrintService.fetchProductsForExport(materialNumber, kindId, materialName, brandId, offset, limit);
            }

            @Override
            public List<ExportColumn<Map<String, Object>>> columns() {
                return COLUMNS;
            }
        };
        return excelExportRunner.write(spec, out);
    }

    private static final List<ExportColumn<Map<String, Object>>> COLUMNS = List.of(
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
            ExportColumn.of("矫正尺码组", r -> str(r.get("SIZE_GROUP_NAME"))),
            ExportColumn.of("尺码组", r -> str(r.get("SIZES")))
    );

    /** EAN13 等长数字列统一按文本写出，避免 Excel 显示科学计数法/丢前导零 */
    private static String str(Object v) {
        return v == null ? null : v.toString();
    }
}
