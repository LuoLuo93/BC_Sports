package com.bcsport.admin.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bcsport.admin.entity.agent.PrintTask;
import com.bcsport.admin.entity.sticker.BrandTemplateMatch;
import com.bcsport.admin.entity.sticker.StickerPrintOrder;
import com.bcsport.admin.entity.sticker.StickerPrintOrderDetail;
import com.bcsport.admin.mapper.agent.PrintTaskMapper;
import com.bcsport.admin.mapper.sticker.StickerPrintOrderDetailMapper;
import com.bcsport.admin.mapper.sticker.StickerPrintOrderMapper;
import com.bcsport.admin.service.sticker.BrandTemplateMatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PrintTaskService {

    @Autowired
    private PrintTaskMapper taskMapper;

    @Autowired
    private StickerPrintOrderMapper orderMapper;

    @Autowired
    private StickerPrintOrderDetailMapper detailMapper;

    @Autowired
    private BrandTemplateMatchService brandTemplateMatchService;

    @Autowired
    private ObjectMapper objectMapper;

    public List<PrintTask> pullTasks(String agentId) {
        List<PrintTask> tasks = taskMapper.selectList(
            new LambdaQueryWrapper<PrintTask>()
                .eq(PrintTask::getAgentId, agentId)
                .eq(PrintTask::getStatus, 0)
                .orderByAsc(PrintTask::getCreateTime)
                .last("FETCH FIRST 10 ROWS ONLY")
        );

        for (PrintTask task : tasks) {
            task.setStatus(1);
            taskMapper.updateById(task);
        }

        return tasks;
    }

    public void reportResult(String taskId, boolean success, String message) {
        PrintTask task = taskMapper.selectOne(
            new LambdaQueryWrapper<PrintTask>()
                .eq(PrintTask::getTaskId, taskId)
        );

        if (task != null) {
            task.setStatus(success ? 2 : 3);
            task.setErrorMsg(message);
            task.setPrintTime(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }

    public String createTasksFromOrder(String orderId, String agentId) {
        StickerPrintOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("申请单不存在");
        }

        List<StickerPrintOrderDetail> details = detailMapper.selectList(
            new LambdaQueryWrapper<StickerPrintOrderDetail>()
                .eq(StickerPrintOrderDetail::getOrderId, orderId)
        );

        if (details.isEmpty()) {
            throw new RuntimeException("申请单无明细");
        }

        List<String> taskIds = new ArrayList<>();

        for (StickerPrintOrderDetail detail : details) {
            BrandTemplateMatch match = brandTemplateMatchService.matchByName(
                detail.getBrandName(), detail.getKindName()
            );

            String templateFile = match != null ? match.getTemplateName() : "default.btw";
            String printerName = match != null ? match.getPrinterName() : "";

            Map<String, String> printData = new HashMap<>();
            printData.put("MaterialNumber", detail.getMaterialNumber());
            printData.put("MaterialName", detail.getMaterialName());
            printData.put("StyleNumber", detail.getStyleNumber());
            printData.put("Color", detail.getColor());
            printData.put("BrandName", detail.getBrandName());
            printData.put("KindName", detail.getKindName());
            printData.put("SizeName", detail.getSizeName());
            printData.put("EAN13", detail.getEan13());
            printData.put("Price", detail.getPrice() != null ? detail.getPrice().toString() : "");

            String taskId = UUID.randomUUID().toString().replace("-", "");

            PrintTask task = new PrintTask();
            task.setTaskId(taskId);
            task.setOrderNo(order.getOrderNo());
            task.setOrderId(orderId);
            task.setMaterialNumber(detail.getMaterialNumber());
            task.setMaterialName(detail.getMaterialName());
            task.setStyleNumber(detail.getStyleNumber());
            task.setColor(detail.getColor());
            task.setBrandName(detail.getBrandName());
            task.setKindName(detail.getKindName());
            task.setSizeName(detail.getSizeName());
            task.setPrintQty(detail.getPrintQty());
            task.setTemplateFile(templateFile);
            task.setPrinterName(printerName);
            try {
                task.setPrintData(objectMapper.writeValueAsString(printData));
            } catch (Exception e) {
                task.setPrintData("{}");
            }
            task.setAgentId(agentId);
            task.setStatus(0);
            task.setCreateTime(LocalDateTime.now());
            task.setRetryCount(0);

            taskMapper.insert(task);
            taskIds.add(taskId);
        }

        return String.join(",", taskIds);
    }

    public List<PrintTask> getTasksByOrderId(String orderId) {
        return taskMapper.selectList(
            new LambdaQueryWrapper<PrintTask>()
                .eq(PrintTask::getOrderId, orderId)
                .orderByAsc(PrintTask::getCreateTime)
        );
    }

    public List<PrintTask> getTasksByAgentId(String agentId) {
        return taskMapper.selectList(
            new LambdaQueryWrapper<PrintTask>()
                .eq(PrintTask::getAgentId, agentId)
                .orderByDesc(PrintTask::getCreateTime)
                .last("FETCH FIRST 100 ROWS ONLY")
        );
    }
}
