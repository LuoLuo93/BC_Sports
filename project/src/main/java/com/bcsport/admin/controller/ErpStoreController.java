package com.bcsport.admin.controller;

import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.Result;
import com.bcsport.admin.erpmapper.BjerpStoreMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ERP 店仓管理控制器（数据源：bjerp C_STORE）
 */
@RestController
@RequestMapping("/api/erp-store")
@Api(tags = "ERP 店仓管理")
public class ErpStoreController {

    @Autowired
    private BjerpStoreMapper bjerpStoreMapper;

    /**
     * 分页查询店仓列表
     */
    @GetMapping("/page")
    @ApiOperation("分页查询店仓列表")
    @RequiresPermissions("bi:erpStore:query")
    public Result<PageResult<Map<String, Object>>> page(PageQuery pageQuery,
                                                         @RequestParam(required = false) String code,
                                                         @RequestParam(required = false) String name,
                                                         @RequestParam(required = false) String supervisorId) {
        int safePageSize = Math.min(pageQuery.getPageSize() != null ? pageQuery.getPageSize() : 10, 500);
        int safePageNum = Math.max(pageQuery.getPageNum() != null ? pageQuery.getPageNum() : 1, 1);
        long offset = (long) (safePageNum - 1) * safePageSize;

        long total = bjerpStoreMapper.countStores(code, name, supervisorId);
        List<Map<String, Object>> list = bjerpStoreMapper.searchStores(code, name, supervisorId, offset, safePageSize);

        PageResult<Map<String, Object>> pageResult = new PageResult<>();
        pageResult.setPageNum((long) safePageNum);
        pageResult.setPageSize((long) safePageSize);
        pageResult.setTotal(total);
        pageResult.setPages((total + safePageSize - 1) / safePageSize);
        pageResult.setRecords(list);
        pageResult.setHasPrevious(safePageNum > 1);
        pageResult.setHasNext(safePageNum < pageResult.getPages());

        return Result.success(pageResult);
    }

    /**
     * 精简分页查询店仓（渠道配置表单专用）：只返回 CODE/NAME，
     * 字段名与前端 EntityChannelForm 表格绑定一致。
     */
    @GetMapping("/simple-page")
    @ApiOperation("分页查询店仓(精简,渠道配置表单用)")
    public Result<PageResult<Map<String, Object>>> simplePage(PageQuery pageQuery,
                                                               @RequestParam(required = false) String code,
                                                               @RequestParam(required = false) String name) {
        int safePageSize = Math.min(pageQuery.getPageSize() != null ? pageQuery.getPageSize() : 10, 500);
        int safePageNum = Math.max(pageQuery.getPageNum() != null ? pageQuery.getPageNum() : 1, 1);
        long offset = (long) (safePageNum - 1) * safePageSize;

        long total = bjerpStoreMapper.countStores(code, name, null);
        List<Map<String, Object>> list = bjerpStoreMapper.searchStoresSimple(code, name, null, offset, safePageSize);

        PageResult<Map<String, Object>> pageResult = new PageResult<>();
        pageResult.setPageNum((long) safePageNum);
        pageResult.setPageSize((long) safePageSize);
        pageResult.setTotal(total);
        pageResult.setPages((total + safePageSize - 1) / safePageSize);
        pageResult.setRecords(list);
        pageResult.setHasPrevious(safePageNum > 1);
        pageResult.setHasNext(safePageNum < pageResult.getPages());

        return Result.success(pageResult);
    }

    /**
     * 查询所有店仓（下拉选择用）
     */
    @GetMapping("/list-all")
    @ApiOperation("查询所有店仓")
    @RequiresPermissions("bi:erpStore:query")
    public Result<List<Map<String, Object>>> listAll() {
        return Result.success(bjerpStoreMapper.listAllStores());
    }

    /**
     * 店仓主品牌下拉（C_STOREATTRIBVALUE DIM5）
     */
    @GetMapping("/brands")
    @ApiOperation("店仓品牌列表")
    @RequiresPermissions("bi:erpStore:query")
    public Result<List<Map<String, Object>>> brands() {
        return Result.success(bjerpStoreMapper.listBrands());
    }

    /**
     * 店仓零售督导下拉（C_STOREATTRIBVALUE DIM6）
     */
    @GetMapping("/supervisors")
    @ApiOperation("店仓零售督导列表")
    @RequiresPermissions("bi:erpStore:query")
    public Result<List<Map<String, Object>>> supervisors() {
        return Result.success(bjerpStoreMapper.listSupervisors());
    }

    /**
     * 编辑店仓品牌/督导及扩展属性，写回 ERP C_STORE。
     * body: { storeId, brandId, supervisorId, isStop, htArea, propType, groupName, channelFormat, mallName }
     */
    @PutMapping("/attrib")
    @ApiOperation("编辑店仓品牌/督导及扩展属性")
    @RequiresPermissions("bi:erpStore:edit")
    public Result<?> updateAttrib(@RequestBody Map<String, Object> body) {
        String storeId = body.get("storeId") == null ? null : body.get("storeId").toString();
        String brandId = body.get("brandId") == null ? null : body.get("brandId").toString();
        String supervisorId = body.get("supervisorId") == null ? null : body.get("supervisorId").toString();
        String isStop = body.get("isStop") == null ? null : body.get("isStop").toString();
        String htArea = body.get("htArea") == null ? null : body.get("htArea").toString();
        String propType = body.get("propType") == null ? null : body.get("propType").toString();
        String groupName = body.get("groupName") == null ? null : body.get("groupName").toString();
        String channelFormat = body.get("channelFormat") == null ? null : body.get("channelFormat").toString();
        String mallName = body.get("mallName") == null ? null : body.get("mallName").toString();
        String rentBegin = body.get("rentBegin") == null ? null : body.get("rentBegin").toString();
        String rentEnd = body.get("rentEnd") == null ? null : body.get("rentEnd").toString();
        if (storeId == null || storeId.isBlank()) {
            return Result.paramError("店仓ID不能为空");
        }
        int rows = bjerpStoreMapper.updateStoreAttrib(storeId, brandId, supervisorId,
                isStop, htArea, propType, groupName, channelFormat, mallName, rentBegin, rentEnd);
        if (rows == 0) {
            return Result.error("店仓不存在，更新失败");
        }
        return Result.success("保存成功");
    }

    /**
     * 零售主管继承-预览：查询某零售主管名下有多少店铺（复用列表 count）。
     * param: fromSupervisorId 原零售主管 ID
     */
    @GetMapping("/supervisor-inherit/preview")
    @ApiOperation("零售主管继承-预览受影响店铺数")
    @RequiresPermissions("bi:erpStore:query")
    public Result<Map<String, Object>> previewSupervisorInherit(@RequestParam String fromSupervisorId) {
        if (fromSupervisorId == null || fromSupervisorId.isBlank()) {
            return Result.paramError("原零售主管不能为空");
        }
        long count = bjerpStoreMapper.countStores(null, null, fromSupervisorId);
        return Result.success(Map.of("count", count));
    }

    /**
     * 零售主管继承-执行：把原零售主管名下的店铺全部改为目标零售主管。
     * body: { fromSupervisorId, toSupervisorId }
     */
    @PutMapping("/supervisor-inherit")
    @ApiOperation("零售主管继承-批量修改")
    @RequiresPermissions("bi:erpStore:edit")
    public Result<?> executeSupervisorInherit(@RequestBody Map<String, Object> body) {
        String fromSupervisorId = body.get("fromSupervisorId") == null ? null : body.get("fromSupervisorId").toString();
        String toSupervisorId = body.get("toSupervisorId") == null ? null : body.get("toSupervisorId").toString();
        if (fromSupervisorId == null || fromSupervisorId.isBlank()) {
            return Result.paramError("原零售主管不能为空");
        }
        if (toSupervisorId == null || toSupervisorId.isBlank()) {
            return Result.paramError("目标零售主管不能为空");
        }
        if (fromSupervisorId.equals(toSupervisorId)) {
            return Result.paramError("原零售主管与目标零售主管不能相同");
        }
        int rows = bjerpStoreMapper.updateSupervisorBatch(fromSupervisorId, toSupervisorId);
        return Result.success("继承成功，共更新 " + rows + " 家店铺");
    }
}
