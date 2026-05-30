package com.bcsport.admin.task.nxcrm;

import com.bcsport.admin.entity.ihr.NxcrmTagInfo;
import com.bcsport.admin.entity.ihr.NxcrmTagValue;
import com.bcsport.admin.ihrmapper.NxcrmTagInfoMapper;
import com.bcsport.admin.ihrmapper.NxcrmTagValueMapper;
import com.nascent.ecrp.opensdk.domain.customer.tag.IncrementTagInfo;
import com.nascent.ecrp.opensdk.domain.customer.tag.IncrementTagValueInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("nxcrmTagIncrementSyncTask")
public class NxcrmTagIncrementSyncTask {

    private static volatile boolean syncing = false;

    public static boolean isSyncing() { return syncing; }

    @Resource
    private NxCrmApiClient nxCrmApiClient;

    @Resource
    private NxcrmTagInfoMapper tagInfoMapper;

    @Resource
    private NxcrmTagValueMapper tagValueMapper;

    @Transactional(rollbackFor = Exception.class)
    public void syncIncrementTags(Map<String, String> params) {
        log.info("=== 开始执行: 南讯CRM同步增量标签 ===");
        syncing = true;
        try {
            Date startTime = parseDate(params, "startTime");
            Date endTime = parseDate(params, "endTime");
            if (startTime == null) {
                startTime = Date.from(LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant());
            }
            if (endTime == null) {
                endTime = new Date();
            }
            Integer entityCode = 1;
            if (params != null && params.get("entityCode") != null) {
                entityCode = Integer.valueOf(params.get("entityCode"));
            }
            log.info("参数: entityCode={}, startTime={}, endTime={}", entityCode, startTime, endTime);

            List<IncrementTagInfo> tags = nxCrmApiClient.getIncrementTags(entityCode, startTime, endTime);
            if (tags == null || tags.isEmpty()) {
                log.info("无增量标签数据");
                log.info("=== 完成执行: 南讯CRM同步增量标签 ===");
                return;
            }

            tagValueMapper.delete(null);
            tagInfoMapper.delete(null);

            LocalDateTime now = LocalDateTime.now();
            List<NxcrmTagInfo> tagInfoList = new ArrayList<>();
            List<NxcrmTagValue> tagValueList = new ArrayList<>();

            for (IncrementTagInfo t : tags) {
                NxcrmTagInfo info = new NxcrmTagInfo();
                info.setTagCode(t.getTagCode());
                info.setTagName(t.getTagName());
                info.setTagFolderId(t.getTagFolderId());
                info.setParentFolderId(t.getParentFolderId());
                info.setTagFolderName(t.getTagFolderName());
                info.setDescription(t.getDescription());
                info.setHasValue(t.getHasValue() != null && t.getHasValue() ? 1 : 0);
                info.setCreateType(t.getCreateType());
                info.setValueDataType(t.getValueDataType());
                info.setValueUnit(t.getValueUnit());
                info.setDisplayOrder(t.getDisplayOrder());
                info.setIsSystem(t.getIsSystem());
                info.setTagObjectType(t.getTagObjectType());
                info.setTagMasterType(t.getTagMasterType());
                info.setGroupId(t.getGroupId());
                info.setEntityCode(t.getEntityCode());
                info.setTagCreateTime(toLocalDateTime(t.getCreateTime()));
                info.setTagUpdateTime(toLocalDateTime(t.getUpdateTime()));
                info.setSyncTime(now);
                tagInfoList.add(info);

                if (t.getTagValueList() != null) {
                    for (IncrementTagValueInfo v : t.getTagValueList()) {
                        NxcrmTagValue val = new NxcrmTagValue();
                        val.setTagCode(t.getTagCode());
                        val.setTagValueCode(v.getTagValueCode());
                        val.setTagValueName(v.getTagValueName());
                        val.setDescription(v.getDescription());
                        val.setDisplayOrder(v.getDisplayOrder());
                        val.setSyncTime(now);
                        tagValueList.add(val);
                    }
                }
            }

            if (!tagInfoList.isEmpty()) {
                tagInfoMapper.insertBatch(tagInfoList);
            }
            if (!tagValueList.isEmpty()) {
                tagValueMapper.insertBatch(tagValueList);
            }
            log.info("增量标签同步完成, 标签{}条, 标签值{}条", tagInfoList.size(), tagValueList.size());
            log.info("=== 完成执行: 南讯CRM同步增量标签 ===");
        } catch (Exception e) {
            log.error("=== 失败执行: 南讯CRM同步增量标签 ===", e);
            throw e;
        } finally {
            syncing = false;
        }
    }

    private Date parseDate(Map<String, String> params, String key) {
        if (params == null) return null;
        String val = params.get(key);
        if (val == null || val.trim().isEmpty()) return null;
        try {
            LocalDateTime ldt = LocalDateTime.parse(val, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            try {
                LocalDate ld = LocalDate.parse(val, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
            } catch (Exception e2) {
                log.warn("无法解析日期参数 {}: {}", key, val);
                return null;
            }
        }
    }

    private LocalDateTime toLocalDateTime(java.util.Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
