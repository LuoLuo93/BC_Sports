package com.bcsport.admin.controller;

import com.bcsport.admin.annotation.OperLog;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.QywxTagQueryDTO;
import com.bcsport.admin.dto.QywxTagRecordQueryDTO;
import com.bcsport.admin.entity.qywx.VxCorpTag;
import com.bcsport.admin.entity.qywx.VxCustomerTag;
import com.bcsport.admin.entity.qywx.VxTagBatch;
import com.bcsport.admin.qywxmapper.VxCorpTagMapper;
import com.bcsport.admin.qywxmapper.VxCustomerTagMapper;
import com.bcsport.admin.qywxmapper.VxTagBatchMapper;
import com.bcsport.admin.task.qywx.QywxCustomerTagTask;
import com.bcsport.admin.task.qywx.QywxApiClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/qywx/tag")
@Api(tags = "企微客户标签管理")
public class QywxTagController {

    /** 单次上传数据行上限，防止超大文件撑爆内存 */
    private static final int MAX_UPLOAD_ROWS = 50000;

    @Autowired
    private QywxCustomerTagTask customerTagTask;

    @Autowired
    private VxCorpTagMapper corpTagMapper;

    @Autowired
    private VxCustomerTagMapper customerTagMapper;

    @Autowired
    private VxTagBatchMapper tagBatchMapper;

    @Autowired
    private QywxApiClient qywxApiClient;

    @Autowired
    @Qualifier("taskThreadPool")
    private ThreadPoolExecutor taskThreadPool;

    @GetMapping("/corp-tags")
    @ApiOperation("获取企业标签库（分页）")
    @RequiresPermissions("qywx:tag:query")
    public Result<Map<String, Object>> getCorpTags(@Valid PageQuery pageQuery, QywxTagQueryDTO queryDTO) {
        String tagName = queryDTO.getTagName();
        // 标签库数据量小(企微上限100组)，放宽页大小上限支持前端一次拉全量(受PageQuery全局@Max(500)约束)
        int pageSize = Math.max(Math.min(pageQuery.getPageSize(), 500), 1);
        int pageNum = Math.max(pageQuery.getPageNum(), 1);
        int offset = (pageNum - 1) * pageSize;

        List<VxCorpTag> groups = corpTagMapper.selectPageGroups(tagName, offset, pageSize);
        long total = corpTagMapper.selectGroupCount(tagName);

        List<VxCorpTag> allTags = new ArrayList<>();
        if (!groups.isEmpty()) {
            List<String> groupIds = groups.stream().map(VxCorpTag::getTagId).collect(Collectors.toList());
            List<VxCorpTag> children = corpTagMapper.selectChildrenByGroupIds(groupIds);

            Map<String, List<VxCorpTag>> childrenMap = children.stream()
                    .collect(Collectors.groupingBy(VxCorpTag::getGroupId));

            for (VxCorpTag group : groups) {
                allTags.add(group);
                List<VxCorpTag> groupChildren = childrenMap.getOrDefault(group.getTagId(), Collections.emptyList());
                allTags.addAll(groupChildren);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", allTags);
        result.put("total", total);
        result.put("current", pageNum);
        result.put("size", pageSize);
        result.put("pages", (total + pageSize - 1) / pageSize);
        return Result.success(result);
    }

    @PostMapping("/sync")
    @ApiOperation("同步企业标签库")
    @OperLog(module = "企微标签", operation = "同步企业标签库")
    @RequiresPermissions("qywx:tag:sync")
    public Result<String> syncCorpTags() {
        if (QywxCustomerTagTask.isBatchTagging()) {
            return Result.error("批量打标进行中，请稍后再试");
        }
        if (QywxCustomerTagTask.isSyncing()) {
            return Result.error("标签库同步正在进行中，请稍后再试");
        }
        taskThreadPool.execute(() -> {
            try {
                customerTagTask.syncTags();
            } catch (Exception e) {
                log.error("标签库同步异常", e);
            }
        });
        return Result.success("标签库同步已触发，请稍后刷新页面查看数据");
    }

    @GetMapping("/sync-status")
    @ApiOperation("标签库同步状态")
    @RequiresPermissions("qywx:tag:query")
    public Result<Map<String, Object>> getSyncStatus() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("syncing", QywxCustomerTagTask.isSyncing());
        data.put("batchTagging", QywxCustomerTagTask.isBatchTagging());
        return Result.success(data);
    }

    @PostMapping("/add-corp-tag")
    @ApiOperation("添加标签组")
    @OperLog(module = "企微标签", operation = "添加标签组")
    @RequiresPermissions("qywx:tag:sync")
    public Result<?> addCorpTag(@RequestBody Map<String, Object> params) {
        if (QywxCustomerTagTask.isBatchTagging()) {
            return Result.error("批量打标进行中，请稍后再操作标签库");
        }
        String groupName = (String) params.get("groupName");
        List<String> tags = (List<String>) params.get("tags");
        if (groupName == null || groupName.trim().isEmpty()) {
            return Result.paramError("标签组名称不能为空");
        }
        if (tags == null || tags.isEmpty()) {
            return Result.paramError("请至少添加一个标签");
        }
        try {
            tags = tags.stream().filter(t -> t != null && !t.trim().isEmpty()).collect(Collectors.toList());
            if (tags.isEmpty()) { return Result.paramError("请至少添加一个有效标签"); }
            qywxApiClient.addCorpTag(null, groupName.trim(), tags);
            customerTagTask.syncTags();
            return Result.success("标签组创建成功");
        } catch (Exception e) {
            log.error("添加标签组失败: {}", e.getMessage(), e);
            return Result.error("添加标签组失败: " + e.getMessage());
        }
    }

    @PostMapping("/edit-corp-tag")
    @ApiOperation("编辑标签组")
    @OperLog(module = "企微标签", operation = "编辑标签组")
    @RequiresPermissions("qywx:tag:sync")
    public Result<?> editCorpTagGroup(@RequestBody Map<String, Object> params) {
        if (QywxCustomerTagTask.isBatchTagging()) {
            return Result.error("批量打标进行中，请稍后再操作标签库");
        }
        String groupId = (String) params.get("groupId");
        String groupName = (String) params.get("groupName");
        List<Map<String, String>> tags = (List<Map<String, String>>) params.get("tags");
        if (groupId == null || groupId.isEmpty()) {
            return Result.paramError("标签组ID不能为空");
        }
        if (groupName == null || groupName.trim().isEmpty()) {
            return Result.paramError("标签组名称不能为空");
        }
        try {
            // 1. 编辑标签组名称
            qywxApiClient.editCorpTag(groupId, groupName.trim(), null);

            if (tags != null) {
                // 2. 编辑已有标签名称 / 收集新标签
                List<String> newTagNames = new ArrayList<>();
                for (Map<String, String> tag : tags) {
                    String tagId = tag.get("tagId");
                    String tagName = tag.get("tagName");
                    if (tagName == null || tagName.trim().isEmpty()) continue;
                    if (tagId != null && !tagId.isEmpty()) {
                        // 已有标签，编辑名称
                        qywxApiClient.editCorpTag(tagId, tagName.trim(), null);
                    } else {
                        // 新标签
                        newTagNames.add(tagName.trim());
                    }
                }
                // 3. 删除已移除的标签
                List<String> deletedIds = (List<String>) params.get("deletedTagIds");
                if (deletedIds != null && !deletedIds.isEmpty()) {
                    qywxApiClient.delCorpTag(deletedIds, null);
                }
                // 4. 添加新标签
                if (!newTagNames.isEmpty()) {
                    qywxApiClient.addCorpTag(groupId, null, newTagNames);
                }
            }
            customerTagTask.syncTags();
            return Result.success("标签组编辑成功");
        } catch (Exception e) {
            log.error("编辑标签组失败: {}", e.getMessage(), e);
            return Result.error("标签组编辑部分失败，部分修改可能已生效，请同步标签库后检查: " + e.getMessage());
        }
    }

    @PostMapping("/delete-corp-tag")
    @ApiOperation("删除标签组")
    @OperLog(module = "企微标签", operation = "删除标签组")
    @RequiresPermissions("qywx:tag:sync")
    public Result<?> deleteCorpTagGroup(@RequestBody Map<String, Object> params) {
        if (QywxCustomerTagTask.isBatchTagging()) {
            return Result.error("批量打标进行中，请稍后再操作标签库");
        }
        String groupId = (String) params.get("groupId");
        if (groupId == null || groupId.isEmpty()) {
            return Result.paramError("标签组ID不能为空");
        }
        try {
            // 官方语义：传 group_id 即删除整个标签组及其下所有标签，无需同时传 tag_id
            qywxApiClient.delCorpTag(null, Collections.singletonList(groupId));
            customerTagTask.syncTags();
            return Result.success("标签组删除成功");
        } catch (Exception e) {
            log.error("删除标签组失败: {}", e.getMessage(), e);
            return Result.error("删除标签组失败: " + e.getMessage());
        }
    }

    @GetMapping("/template")
    @ApiOperation("下载批量打标Excel模板")
    @RequiresPermissions("qywx:tag:query")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode("批量打标模板.xlsx", StandardCharsets.UTF_8.name()));

        ExcelWriter writer = ExcelUtil.getWriter(true);
        try {
            writer.addHeaderAlias("externalUserid", "externalUserid (客户ID)");
            writer.addHeaderAlias("tag1", "标签1");
            writer.addHeaderAlias("tag2", "标签2");
            writer.addHeaderAlias("tag3", "标签3");
            writer.addHeaderAlias("tag4", "标签4");
            writer.addHeaderAlias("tag5", "标签5");

            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("externalUserid", "示例: wmABC123...");
            sample.put("tag1", "VIP客户");
            sample.put("tag2", "-已流失");
            sample.put("tag3", "");
            sample.put("tag4", "");
            sample.put("tag5", "");
            writer.write(Collections.singletonList(sample));
            writer.flush(response.getOutputStream());
        } finally {
            writer.close();
        }
    }

    /**
     * 解析批量打标Excel。第1列 externalUserid，后续列标签名；标签名前加 - 表示移除该标签。
     * 行内 (客户ID, 动作, 标签名) 去重；限制单次数据行数。
     */
    private List<Map<String, String>> parseTagExcel(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream(); ExcelReader reader = ExcelUtil.getReader(in)) {
            List<List<Object>> data = reader.read();
            List<Map<String, String>> rows = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            int dataRows = 0;
            for (int i = 1; i < data.size(); i++) {
                List<Object> row = data.get(i);
                if (row == null || row.isEmpty() || row.get(0) == null) {
                    continue;
                }
                String externalUserid = cellToString(row.get(0));
                if (externalUserid.isEmpty()) {
                    continue;
                }
                boolean hasTag = false;
                for (int col = 1; col < row.size(); col++) {
                    String tagName = cellToString(row.get(col));
                    if (tagName.isEmpty()) continue;
                    hasTag = true;
                    String action = QywxCustomerTagTask.ACTION_ADD;
                    if (tagName.startsWith("-")) {
                        tagName = tagName.substring(1).trim();
                        if (tagName.isEmpty()) continue;
                        action = QywxCustomerTagTask.ACTION_REMOVE;
                    }
                    if (seen.add(externalUserid + "\u0001" + action + "\u0001" + tagName)) {
                        Map<String, String> item = new HashMap<>();
                        item.put("externalUserid", externalUserid);
                        item.put("tagName", tagName);
                        item.put("action", action);
                        rows.add(item);
                    }
                }
                if (hasTag) {
                    dataRows++;
                    if (dataRows > MAX_UPLOAD_ROWS) {
                        throw new IllegalArgumentException("数据行数超过单次上限" + MAX_UPLOAD_ROWS + "行，请拆分文件后分批上传");
                    }
                }
            }
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("Excel中没有有效数据");
            }
            return rows;
        }
    }

    /** 数字单元格取字符串：去掉 1.0 / 科学计数法尾巴，其余按字符串 trim */
    private String cellToString(Object v) {
        if (v == null) return "";
        if (v instanceof Number) {
            return new BigDecimal(String.valueOf(v)).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(v).trim();
    }

    @PostMapping("/upload-preview")
    @ApiOperation("上传Excel预检（dry-run，只校验不执行）")
    @RequiresPermissions("qywx:tag:batch")
    public Result<Map<String, Object>> uploadTagExcelPreview(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.paramError("请上传Excel文件");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls"))) {
            return Result.paramError("仅支持.xlsx或.xls格式的Excel文件");
        }
        try {
            List<Map<String, String>> rows = parseTagExcel(file);
            return Result.success(customerTagTask.previewTag(rows));
        } catch (IllegalArgumentException e) {
            return Result.paramError(e.getMessage());
        } catch (Exception e) {
            log.error("Excel预检解析失败: {}", e.getMessage(), e);
            return Result.error("Excel解析失败，请检查文件格式");
        }
    }

    @PostMapping("/upload")
    @ApiOperation("上传Excel批量打标")
    @OperLog(module = "企微标签", operation = "Excel批量打标", saveParams = false)
    @RequiresPermissions("qywx:tag:batch")
    public Result<String> uploadTagExcel(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "autoCreateTags", required = false, defaultValue = "false") boolean autoCreateTags) {
        if (file.isEmpty()) {
            return Result.paramError("请上传Excel文件");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls"))) {
            return Result.paramError("仅支持.xlsx或.xls格式的Excel文件");
        }
        if (QywxCustomerTagTask.isBatchTagging()) {
            return Result.error("打标任务正在进行中，请稍后再试");
        }

        try {
            List<Map<String, String>> rows = parseTagExcel(file);
            String fileName = originalFilename;
            taskThreadPool.execute(() -> customerTagTask.batchTagAsync(rows, fileName, autoCreateTags));
            return Result.success("打标任务已触发，请稍后查看打标签日志");
        } catch (IllegalArgumentException e) {
            return Result.paramError(e.getMessage());
        } catch (Exception e) {
            log.error("Excel解析失败: {}", e.getMessage(), e);
            return Result.error("Excel解析失败，请检查文件格式");
        }
    }

    @GetMapping("/batches")
    @ApiOperation("查询批量打标批次汇总（分页）")
    @RequiresPermissions("qywx:tag:query")
    public Result<Map<String, Object>> getTagBatches(@Valid PageQuery pageQuery) {
        Page<VxTagBatch> pageParam = pageQuery.toPage();
        QueryWrapper<VxTagBatch> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("startTime");

        IPage<VxTagBatch> pageResult = tagBatchMapper.selectPage(pageParam, wrapper);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("current", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        result.put("pages", pageResult.getPages());
        return Result.success(result);
    }

    @GetMapping("/records")
    @ApiOperation("查询打标记录（分页）")
    @RequiresPermissions("qywx:tag:query")
    public Result<Map<String, Object>> getTagRecords(@Valid PageQuery pageQuery, QywxTagRecordQueryDTO queryDTO) {
        Page<VxCustomerTag> pageParam = pageQuery.toPage();
        QueryWrapper<VxCustomerTag> wrapper = new QueryWrapper<>();
        if (queryDTO.getExternalUserid() != null && !queryDTO.getExternalUserid().isEmpty()) {
            wrapper.eq("externalUserid", queryDTO.getExternalUserid());
        }
        if (queryDTO.getTagId() != null && !queryDTO.getTagId().isEmpty()) {
            wrapper.eq("tagId", queryDTO.getTagId());
        }
        if (queryDTO.getTagName() != null && !queryDTO.getTagName().isEmpty()) {
            wrapper.like("tagName", queryDTO.getTagName());
        }
        if (queryDTO.getBatchNo() != null && !queryDTO.getBatchNo().isEmpty()) {
            wrapper.eq("batchNo", queryDTO.getBatchNo());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq("status", queryDTO.getStatus());
        }
        wrapper.orderByDesc("createTime");

        IPage<VxCustomerTag> pageResult = customerTagMapper.selectPage(pageParam, wrapper);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("current", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        result.put("pages", pageResult.getPages());
        return Result.success(result);
    }
}
