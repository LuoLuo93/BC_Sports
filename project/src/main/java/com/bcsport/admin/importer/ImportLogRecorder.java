package com.bcsport.admin.importer;

import com.bcsport.admin.entity.SysImportLog;
import com.bcsport.admin.mapper.SysImportLogMapper;
import com.bcsport.admin.util.ShiroSecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * 统一导入日志写入器：替代各模块的 saveImportLog。
 * 与历史行为一致——日志写入失败只 warn，不影响导入结果。
 */
@Slf4j
@Component
public class ImportLogRecorder {

    /** errorMsg 截断长度，与旧各表一致 */
    static final int ERROR_MSG_MAX = 4000;
    /** 与建表 file_name VARCHAR2(500 CHAR) 对齐 */
    static final int FILE_NAME_MAX = 500;

    private final SysImportLogMapper sysImportLogMapper;

    public ImportLogRecorder(SysImportLogMapper sysImportLogMapper) {
        this.sysImportLogMapper = sysImportLogMapper;
    }

    public void record(ImportType type, ImportOutcome outcome, MultipartFile file) {
        try {
            SysImportLog entity = new SysImportLog();
            entity.setImportType(type.getCode());
            if (file != null) {
                String name = file.getOriginalFilename();
                entity.setFileName(name != null && name.length() > FILE_NAME_MAX ? name.substring(0, FILE_NAME_MAX) : name);
                entity.setFileSize(file.getSize());
            }
            entity.setTotalCount(outcome.getTotal());
            entity.setSuccessCount(outcome.getSuccess());
            entity.setFailCount(outcome.getFail());
            entity.setStatus(outcome.getStatus());
            if (!outcome.getErrors().isEmpty()) {
                String msg = String.join("\n", outcome.getErrors());
                entity.setErrorMsg(msg.length() > ERROR_MSG_MAX ? msg.substring(0, ERROR_MSG_MAX) : msg);
            }
            entity.setCreateBy(ShiroSecurityUtils.getCurrentUsername());
            entity.setCreateTime(LocalDateTime.now());
            sysImportLogMapper.insert(entity);
        } catch (Exception e) {
            // 不影响导入结果，但必须可见：统一表缺失/列宽超限会在这里持续失败
            log.error("{} 保存导入日志失败（导入结果不受影响）: {}", type, e.getMessage(), e);
        }
    }
}
