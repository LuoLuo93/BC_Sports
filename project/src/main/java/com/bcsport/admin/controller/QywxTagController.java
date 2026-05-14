package com.bcsport.admin.controller;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.entity.qywx.VxCorpTag;
import com.bcsport.admin.entity.qywx.VxCustomerTag;
import com.bcsport.admin.qywxmapper.VxCorpTagMapper;
import com.bcsport.admin.qywxmapper.VxCustomerTagMapper;
import com.bcsport.admin.task.qywx.QywxCustomerTagTask;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/qywx/tag")
@Api(tags = "企微客户标签管理")
public class QywxTagController {

    @Autowired
    private QywxCustomerTagTask customerTagTask;

    @Autowired
    private VxCorpTagMapper corpTagMapper;

    @Autowired
    private VxCustomerTagMapper customerTagMapper;

    @GetMapping("/corp-tags")
    @ApiOperation("获取企业标签库（分页）")
    @RequiresPermissions("qywx:tag:query")
    public Result<Map<String, Object>> getCorpTags(
            @RequestParam(required = false) String tagName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;
        int offset = (page - 1) * size;

        List<VxCorpTag> groups = corpTagMapper.selectPageGroups(tagName, offset, size);
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
        result.put("current", page);
        result.put("size", size);
        result.put("pages", (total + size - 1) / size);
        return Result.success(result);
    }

    @PostMapping("/sync")
    @ApiOperation("同步企业标签库")
    @RequiresPermissions("qywx:tag:sync")
    public Result<String> syncCorpTags() {
        try {
            customerTagTask.syncTags();
            return Result.success("标签库同步成功");
        } catch (Exception e) {
            log.error("标签库同步失败: {}", e.getMessage(), e);
            return Result.error("标签库同步失败，请查看日志或稍后重试");
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
            sample.put("tag2", "高活跃");
            sample.put("tag3", "");
            sample.put("tag4", "");
            sample.put("tag5", "");
            writer.write(Collections.singletonList(sample));
            writer.flush(response.getOutputStream());
        } finally {
            writer.close();
        }
    }

    @PostMapping("/upload")
    @ApiOperation("上传Excel批量打标")
    @RequiresPermissions("qywx:tag:batch")
    public Result<Map<String, Object>> uploadTagExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.paramError("请上传Excel文件");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls"))) {
            return Result.paramError("仅支持.xlsx或.xls格式的Excel文件");
        }

        try {
            ExcelReader reader = ExcelUtil.getReader(file.getInputStream());
            try {
                List<Map<String, String>> rows = new ArrayList<>();
                List<List<Object>> data = reader.read();
                for (int i = 1; i < data.size(); i++) {
                    List<Object> row = data.get(i);
                    if (row == null || row.isEmpty() || row.get(0) == null) {
                        continue;
                    }
                    String externalUserid = String.valueOf(row.get(0)).trim();
                    if (externalUserid.isEmpty()) {
                        continue;
                    }
                    for (int col = 1; col < row.size(); col++) {
                        Object val = row.get(col);
                        if (val == null) continue;
                        String tagName = String.valueOf(val).trim();
                        if (tagName.isEmpty()) continue;
                        Map<String, String> item = new HashMap<>();
                        item.put("externalUserid", externalUserid);
                        item.put("tagName", tagName);
                        rows.add(item);
                    }
                }

                if (rows.isEmpty()) {
                    return Result.paramError("Excel中没有有效数据");
                }

                Map<String, Object> summary = customerTagTask.batchTag(rows);
                return Result.success(summary);
            } finally {
                reader.close();
            }
        } catch (Exception e) {
            log.error("Excel打标失败: {}", e.getMessage(), e);
            return Result.error("Excel打标失败，请检查文件格式或查看日志");
        }
    }

    @GetMapping("/records")
    @ApiOperation("查询打标记录（分页）")
    @RequiresPermissions("qywx:tag:query")
    public Result<Map<String, Object>> getTagRecords(
            @RequestParam(required = false) String externalUserid,
            @RequestParam(required = false) String tagId,
            @RequestParam(required = false) String tagName,
            @RequestParam(required = false) String batchNo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        if (size > 500) size = 500;
        Page<VxCustomerTag> pageParam = new Page<>(page, size);
        QueryWrapper<VxCustomerTag> wrapper = new QueryWrapper<>();
        if (externalUserid != null && !externalUserid.isEmpty()) {
            wrapper.eq("externalUserid", externalUserid);
        }
        if (tagId != null && !tagId.isEmpty()) {
            wrapper.eq("tagId", tagId);
        }
        if (tagName != null && !tagName.isEmpty()) {
            wrapper.like("tagName", tagName);
        }
        if (batchNo != null && !batchNo.isEmpty()) {
            wrapper.eq("batchNo", batchNo);
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
