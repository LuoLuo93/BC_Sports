package com.bcsport.admin.controller;

import com.bcsport.admin.annotation.OperLog;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.dto.EntityChannelDTO;
import com.bcsport.admin.dto.EntityChannelQueryDTO;
import com.bcsport.admin.service.EntityChannelService;
import com.bcsport.admin.vo.EntityChannelVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;

/**
 * 实体渠道配置控制器
 */
@RestController
@RequestMapping("/api/entity-channel")
@Api(tags = "实体渠道配置")
public class EntityChannelController {

    @Autowired
    private EntityChannelService entityChannelService;

    /**
     * 分页查询实体渠道配置列表
     */
    @GetMapping("/page")
    @ApiOperation("分页查询实体渠道配置列表")
    @RequiresPermissions("bi:entity:query")
    public Result<PageResult<EntityChannelVO>> pageEntityChannels(PageQuery pageQuery, EntityChannelQueryDTO queryDTO) {
        PageResult<EntityChannelVO> pageResult = entityChannelService.pageEntityChannels(pageQuery, queryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询实体渠道配置
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询实体渠道配置")
    @RequiresPermissions("bi:entity:query")
    public Result<EntityChannelVO> getById(@PathVariable String id) {
        EntityChannelVO vo = entityChannelService.getEntityChannelVOById(id);
        if (vo == null) {
            return Result.notFound("实体渠道配置不存在");
        }
        return Result.success(vo);
    }

    /**
     * 新增实体渠道配置
     */
    @PostMapping
    @ApiOperation("新增实体渠道配置")
    @RequiresPermissions("bi:entity:add")
    @OperLog(module = "实体渠道", operation = "新增实体渠道")
    public Result<?> add(@Valid @RequestBody EntityChannelDTO dto) {
        try {
            boolean success = entityChannelService.addEntityChannel(dto);
            return success ? Result.success("新增成功") : Result.error("新增失败");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改实体渠道配置（只允许修改渠道属性）
     */
    @PutMapping("/{id}")
    @ApiOperation("修改实体渠道配置")
    @RequiresPermissions("bi:entity:edit")
    @OperLog(module = "实体渠道", operation = "修改实体渠道")
    public Result<?> update(@PathVariable String id, @Valid @RequestBody EntityChannelDTO dto) {
        dto.setId(id);
        try {
            boolean success = entityChannelService.updateEntityChannel(dto);
            return success ? Result.success("修改成功") : Result.error("修改失败");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除实体渠道配置（逻辑删除完成）
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除实体渠道配置")
    @RequiresPermissions("bi:entity:delete")
    @OperLog(module = "实体渠道", operation = "删除实体渠道")
    public Result<?> delete(@PathVariable String id) {
        boolean success = entityChannelService.deleteEntityChannel(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 按实体查询所有渠道配置
     */
    @GetMapping("/list-by-entity")
    @ApiOperation("按实体查询所有渠道配置")
    @RequiresPermissions("bi:entity:query")
    public Result<?> listByEntity(@RequestParam String externalId, @RequestParam String entityType) {
        return Result.success(entityChannelService.listByEntity(externalId, entityType));
    }

    /**
     * 批量保存实体渠道配置
     */
    @PostMapping("/batch-save")
    @ApiOperation("批量保存实体渠道配置")
    @RequiresPermissions("bi:entity:edit")
    @OperLog(module = "实体渠道", operation = "批量保存实体渠道")
    public Result<?> batchSave(@RequestParam String externalId, @RequestParam String entityType, @RequestBody java.util.List<EntityChannelDTO> list) {
        try {
            boolean success = entityChannelService.batchSave(externalId, entityType, list);
            return success ? Result.success("保存成功") : Result.error("保存失败");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 下载批量导入模板
     */
    @GetMapping("/template")
    @ApiOperation("下载批量导入模板")
    @RequiresPermissions("bi:entity:add")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode("实体渠道配置导入模板.xlsx", StandardCharsets.UTF_8.name()));

        ExcelWriter writer = ExcelUtil.getWriter(true);
        try {
            writer.addHeaderAlias("entityType", "实体类型(store/customer)");
            writer.addHeaderAlias("externalId", "外部ID(ERP编码)");
            writer.addHeaderAlias("entityName", "实体名称");
            writer.addHeaderAlias("brandName", "品牌名称");
            writer.addHeaderAlias("regionLevel1Name", "一级地区");
            writer.addHeaderAlias("regionLevel2Name", "二级地区");
            writer.addHeaderAlias("channelTypeName", "渠道类型");
            writer.addHeaderAlias("channelDefName", "渠道定义");
            writer.addHeaderAlias("channelNatureName", "渠道性质");
            writer.addHeaderAlias("businessTypeName", "销售类型");

            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("entityType", "store");
            sample.put("externalId", "D001");
            sample.put("entityName", "深圳旗舰店");
            sample.put("brandName", "BC");
            sample.put("regionLevel1Name", "广东省");
            sample.put("regionLevel2Name", "深圳市");
            sample.put("channelTypeName", "零售渠道");
            sample.put("channelDefName", "专卖店");
            sample.put("channelNatureName", "直营");
            sample.put("businessTypeName", "零售");
            writer.write(Collections.singletonList(sample));
            writer.flush(response.getOutputStream());
        } finally {
            writer.close();
        }
    }

    /**
     * 上传Excel批量导入实体渠道配置
     */
    @PostMapping("/import")
    @ApiOperation("批量导入实体渠道配置")
    @RequiresPermissions("bi:entity:add")
    @OperLog(module = "实体渠道", operation = "批量导入实体渠道")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.paramError("请上传Excel文件");
        }
        // 文件大小限制：与 application.yml 中 spring.servlet.multipart.max-file-size 保持一致
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.paramError("文件大小不能超过10MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls"))) {
            return Result.paramError("仅支持.xlsx或.xls格式的Excel文件");
        }
        try {
            Map<String, Object> result = entityChannelService.importFromExcel(file);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }
}
