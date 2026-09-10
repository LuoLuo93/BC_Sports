package com.bcsport.admin.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.entity.bcp.BcpSportPoints;
import com.bcsport.admin.entity.SysImportLog;
import com.bcsport.admin.service.BcpSportPointsService;
import com.bcsport.admin.vo.SportPointsRankVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * BC好玩家运动积分导入
 */
@Slf4j
@RestController
@RequestMapping("/api/bcp/sport-points")
@Api(tags = "BC好玩家运动积分")
public class BcpSportPointsController {

    /** 移动端榜单展示上限（业务口径：领奖台 3 人 + 列表最多 97 人） */
    static final int RANK_TOP_LIMIT = 100;

    @Autowired
    private BcpSportPointsService bcpSportPointsService;

    @GetMapping("/rank")
    @ApiOperation("移动端榜单前100名（免登录，Shiro 已放行 anon）")
    public Result<List<SportPointsRankVO>> rank() {
        return Result.success(bcpSportPointsService.rankTop(RANK_TOP_LIMIT));
    }

    @GetMapping("/page")
    @ApiOperation("分页查询")
    @RequiresPermissions("bcp:sport-points:query")
    public Result<PageResult<BcpSportPoints>> page(PageQuery pageQuery,
                                                   @RequestParam(required = false) String sporter) {
        return Result.success(bcpSportPointsService.page(pageQuery, sporter));
    }

    @GetMapping("/import-log/page")
    @ApiOperation("导入日志分页查询")
    @RequiresPermissions("bcp:sport-points:query")
    public Result<PageResult<SysImportLog>> importLogPage(PageQuery pageQuery) {
        return Result.success(bcpSportPointsService.logPage(pageQuery));
    }

    @PostMapping("/import")
    @ApiOperation("上传Excel批量导入")
    @RequiresPermissions("bcp:sport-points:import")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.paramError("请上传Excel文件");
        }
        if (file.getSize() > 100 * 1024 * 1024) {
            return Result.paramError("文件大小不能超过100MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls"))) {
            return Result.paramError("仅支持.xlsx或.xls格式的Excel文件");
        }
        try {
            Map<String, Object> result = bcpSportPointsService.importFromExcel(file);
            return Result.success(result);
        } catch (cn.hutool.poi.exceptions.POIException | org.apache.poi.ooxml.POIXMLException
                 | org.apache.poi.util.RecordFormatException e) {
            log.error("BcpSportPoints Excel解析失败: {}", e.getMessage());
            return Result.error("Excel解析失败，请确认文件是标准的 .xlsx/.xls 格式");
        } catch (Exception e) {
            log.error("BcpSportPoints 导入失败: {}", e.getMessage(), e);
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @ApiOperation("编辑一条运动积分")
    @RequiresPermissions("bcp:sport-points:edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody BcpSportPoints body) {
        bcpSportPointsService.updateSportPoints(id, body.getSporter(), body.getPoints());
        return Result.success(null);
    }

    @GetMapping("/template")
    @ApiOperation("下载导入模板")
    @RequiresPermissions("bcp:sport-points:import")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode("运动积分导入模板.xlsx", StandardCharsets.UTF_8.name()));

        ExcelWriter writer = ExcelUtil.getWriter(true);
        try {
            writer.addHeaderAlias("运动员", "运动员");
            writer.addHeaderAlias("积分", "积分");

            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("运动员", "张三");
            sample.put("积分", "100");
            writer.write(Collections.singletonList(sample), true);
            writer.flush(response.getOutputStream());
        } finally {
            writer.close();
        }
    }
}
