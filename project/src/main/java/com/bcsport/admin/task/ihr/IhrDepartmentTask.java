package com.bcsport.admin.task.ihr;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.bcsport.admin.entity.ihr.IhrDepartment;
import com.bcsport.admin.ihrmapper.IhrDepartmentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * IHR任务：部门信息同步 */
@Slf4j
@Component("ihrDepartmentTask")
public class IhrDepartmentTask {

    private static final int BATCH_SIZE = 120;

    @Autowired
    private IhrApiClient apiClient;

    @Autowired
    private IhrDepartmentMapper departmentMapper;

    @Autowired
    @Qualifier("ihrTransactionManager")
    private PlatformTransactionManager transactionManager;

    /**
     * 同步部门组织架构
     */
    public void sync() {
        log.info("=== 开始执行: IHR同步部门信息 ===");
        try {
            // API调用在事务外执行
            JSONArray data = apiClient.getDataArray(
                    "/openapi/thirdparty/api/v1/companies/organizations/basic");

            if (data == null || data.isEmpty()) {
                log.warn("=== 完成: IHR同步部门信息, 无数据 ===");
                return;
            }

            // 数据准备在事务外完成
            List<IhrDepartment> list = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                JSONObject obj = data.getJSONObject(i);
                IhrDepartment dept = new IhrDepartment();
                dept.setId(IdWorker.getId());
                dept.setDepartmentId(obj.getLong("id"));
                dept.setName(obj.getStr("name"));
                dept.setParentId(obj.getLong("parentId"));
                dept.setType(obj.getStr("type"));
                list.add(dept);
            }

            // 短事务仅用于DB写入
            doSync(list);

            log.info("=== 完成: IHR同步部门信息, 共{}条 ===", list.size());
        } catch (Exception e) {
            log.error("=== 失败: IHR同步部门信息: {} ===", e.getMessage());
            throw e;
        }
    }

    void doSync(List<IhrDepartment> list) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(status -> {
            departmentMapper.deleteAll();
            for (int i = 0; i < list.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, list.size());
                departmentMapper.insertBatch(list.subList(i, end));
            }
            return null;
        });
    }
}
