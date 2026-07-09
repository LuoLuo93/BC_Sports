package com.bcsport.admin.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.entity.agent.PrintTask;
import com.bcsport.admin.entity.sticker.BrandTemplateMatch;
import com.bcsport.admin.entity.sticker.PrintFieldMapping;
import com.bcsport.admin.entity.sticker.StickerPrintOrder;
import com.bcsport.admin.entity.sticker.StickerPrintOrderDetail;
import com.bcsport.admin.mapper.agent.PrintTaskMapper;
import com.bcsport.admin.mapper.sticker.StickerPrintOrderDetailMapper;
import com.bcsport.admin.mapper.sticker.StickerPrintOrderMapper;
import com.bcsport.admin.common.exception.BusinessException;
import com.bcsport.admin.service.sticker.BrandTemplateMatchService;
import com.bcsport.admin.service.sticker.PrintFieldMappingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
    private PrintFieldMappingService fieldMappingService;

    @Autowired
    private ObjectMapper objectMapper;

    /** 任务派发后超过该时长仍未回报，视为 Agent 崩溃/丢失，允许重新派发。
     *  必须大于单任务最大物理打印耗时（含大数量批次），否则会误重派正在打印的任务导致重复打印。
     *  改用 dispatch_time（真实派发时刻）而非 create_time 计时，避免排队等待时间被错误计入阈值。 */
    @Value("${agent.task.stuck-minutes:5}")
    private int stuckMinutes;

    /** 单个任务最多重新派发次数，超过则标记失败，避免无限重发导致重复打印。 */
    @Value("${agent.task.max-redispatch:3}")
    private int maxRedispatch;

    /** status=0 任务超过该分钟数仍未被拉取，巡检任务告警（agentId 配错 / Agent 长时间离线）。 */
    private static final int PENDING_WARN_MINUTES = 15;

    public List<PrintTask> pullTasks(String agentId) {
        LocalDateTime stuckBefore = LocalDateTime.now().minusMinutes(stuckMinutes);

        // status=0(待打印) 直接拉取；
        // status=1(已派发) 但 dispatch_time 超过阈值仍未回报，视为 Agent 异常，重新派发。
        List<PrintTask> tasks = taskMapper.selectList(
            new LambdaQueryWrapper<PrintTask>()
                .eq(PrintTask::getAgentId, agentId)
                .and(w -> w
                    .eq(PrintTask::getStatus, 0)
                    .or(n -> n.eq(PrintTask::getStatus, 1).lt(PrintTask::getDispatchTime, stuckBefore))
                )
                .orderByAsc(PrintTask::getCreateTime)
                .last("FETCH FIRST 10 ROWS ONLY")
        );

        List<PrintTask> dispatched = new ArrayList<>();
        for (PrintTask task : tasks) {
            // 重新派发的旧任务：计入重试次数，超过上限则标记失败不再下发
            if (task.getStatus() != null && task.getStatus() == 1) {
                int retry = (task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1;
                if (retry > maxRedispatch) {
                    task.setStatus(3);
                    task.setErrorMsg("多次派发未收到回报，判定为丢失");
                    task.setPrintTime(LocalDateTime.now());
                    taskMapper.updateById(task);
                    log.warn("任务 {} 已达最大重派次数 {}，标记为失败", task.getTaskId(), maxRedispatch);
                    continue;
                }
                task.setRetryCount(retry);
                log.info("重新派发超时未回报的任务 {}，第 {} 次", task.getTaskId(), retry);
            }
            task.setStatus(1);
            task.setDispatchTime(LocalDateTime.now());  // 记录真实派发时刻，用于精确判断卡住重派
            taskMapper.updateById(task);
            dispatched.add(task);
        }
        return dispatched;
    }

    /**
     * 巡检卡住的打印任务（由定时任务 ticket.task.checkStuck 调用）：
     *  - status=0 超过 PENDING_WARN_MINUTES 未被拉取 → 多半 agentId 配错或 Agent 长时间离线；
     *  - status=1 派发超过 阈值×(maxRedispatch+1) 仍无回报 → 多半 Agent 反复崩溃、重派也救不回。
     * 仅做告警（日志），不改变任务状态。
     */
    public void checkStuckTasks() {
        LocalDateTime now = LocalDateTime.now();

        List<PrintTask> pending = taskMapper.selectList(
            new LambdaQueryWrapper<PrintTask>()
                .eq(PrintTask::getStatus, 0)
                .lt(PrintTask::getCreateTime, now.minusMinutes(PENDING_WARN_MINUTES))
                .last("FETCH FIRST 20 ROWS ONLY")
        );
        if (!pending.isEmpty()) {
            log.warn("巡检: {} 个任务已 {} 分钟未被拉取(status=0)，请检查 agentId 是否正确 / 目标 Agent 是否在线: {}",
                pending.size(), PENDING_WARN_MINUTES,
                pending.stream().map(t -> t.getTaskId() + "@(" + t.getAgentId() + ")").limit(5).collect(Collectors.joining(", ")));
        }

        long severeMinutes = (long) stuckMinutes * (maxRedispatch + 1);
        List<PrintTask> inFlight = taskMapper.selectList(
            new LambdaQueryWrapper<PrintTask>()
                .eq(PrintTask::getStatus, 1)
                .lt(PrintTask::getDispatchTime, now.minusMinutes(severeMinutes))
                .last("FETCH FIRST 20 ROWS ONLY")
        );
        if (!inFlight.isEmpty()) {
            log.warn("巡检: {} 个任务派发超 {} 分钟仍无回报(status=1)，可能 Agent 反复崩溃: {}",
                inFlight.size(), severeMinutes,
                inFlight.stream().map(t -> t.getTaskId() + "@(" + t.getAgentId() + ")").limit(5).collect(Collectors.joining(", ")));
        }
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

    @Transactional(rollbackFor = Exception.class)
    public String createTasksFromOrder(String orderId, String agentId) {
        StickerPrintOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("申请单不存在");
        }
        // 仅已审核(status=2)的申请单可下发打印，与 bartenderPrint 保持一致
        if (order.getStatus() == null || order.getStatus() != 2) {
            throw new BusinessException("只有已审核的申请单才能下发打印");
        }

        // 幂等：若该订单已有未完成(待打印/已派发)的任务，拒绝重复下发，避免重复打印
        Long pending = taskMapper.selectCount(
            new LambdaQueryWrapper<PrintTask>()
                .eq(PrintTask::getOrderId, orderId)
                .in(PrintTask::getStatus, 0, 1)
        );
        if (pending != null && pending > 0) {
            throw new BusinessException("该申请单已有未完成的打印任务，请勿重复下发");
        }

        List<StickerPrintOrderDetail> details = detailMapper.selectList(
            new LambdaQueryWrapper<StickerPrintOrderDetail>()
                .eq(StickerPrintOrderDetail::getOrderId, orderId)
        );

        if (details.isEmpty()) {
            throw new BusinessException("申请单无明细");
        }

        // 先校验并缓存每条明细的模板匹配；任一未配置即整体抛错回滚，不产生半成品任务
        // （避免回退 default.btw 导致 Agent 端必然"模板不存在"打印失败）
        List<BrandTemplateMatch> matches = new ArrayList<>();
        List<String> unmatched = new ArrayList<>();
        for (StickerPrintOrderDetail detail : details) {
            BrandTemplateMatch match = brandTemplateMatchService.matchByName(
                detail.getBrandName(), detail.getKindName()
            );
            matches.add(match);
            if (match == null) {
                unmatched.add(detail.getBrandName() + "/" + detail.getKindName());
            }
        }
        if (!unmatched.isEmpty()) {
            throw new BusinessException("以下品牌/品类未配置打印模板，请先在模板匹配中维护: " + String.join("; ", unmatched));
        }

        List<String> taskIds = new ArrayList<>();

        // 预加载每个模板的字段映射（按模板名称缓存，同一模板名称共享映射配置）
        Map<String, List<PrintFieldMapping>> mappingCache = new HashMap<>();

        for (int i = 0; i < details.size(); i++) {
            StickerPrintOrderDetail detail = details.get(i);
            BrandTemplateMatch match = matches.get(i);

            String templateFile = match.getTemplateName();
            String printerName = match.getPrinterName() != null ? match.getPrinterName() : "";

            // 获取该模板的字段映射（按模板名称查询）
            List<PrintFieldMapping> mappings = mappingCache.computeIfAbsent(templateFile, name -> {
                List<PrintFieldMapping> list = fieldMappingService.getByTemplateName(name);
                return list != null ? list : Collections.emptyList();
            });

            // 根据映射构建 printData
            Map<String, String> printData = buildPrintData(detail, mappings);

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

    /**
     * 补打单个任务：克隆原任务的打印数据生成新的待打印任务(status=0)，原任务保持不动（保留审计）。
     * 约束：
     *  - 原任务必须处于终态（2成功 / 3失败），进行中(0/1)不允许补打，避免与正在打印的冲突导致重复；
     *  - 原任务当前已有未完成的补打任务时拒绝，防止重复补打。
     */
    @Transactional(rollbackFor = Exception.class)
    public String reprintTask(String sourceTaskId, String agentId, String reason) {
        PrintTask source = taskMapper.selectOne(
            new LambdaQueryWrapper<PrintTask>().eq(PrintTask::getTaskId, sourceTaskId)
        );
        if (source == null) {
            throw new BusinessException("原任务不存在");
        }
        if (source.getStatus() == null || (source.getStatus() != 2 && source.getStatus() != 3)) {
            throw new BusinessException("只有已成功或失败的任务才能补打");
        }
        Long pending = taskMapper.selectCount(
            new LambdaQueryWrapper<PrintTask>()
                .eq(PrintTask::getSourceTaskId, sourceTaskId)
                .in(PrintTask::getStatus, 0, 1)
        );
        if (pending != null && pending > 0) {
            throw new BusinessException("该任务已有未完成的补打任务，请等待打印完成");
        }

        PrintTask reprint = new PrintTask();
        reprint.setTaskId(UUID.randomUUID().toString().replace("-", ""));
        reprint.setOrderNo(source.getOrderNo());
        reprint.setOrderId(source.getOrderId());
        reprint.setMaterialNumber(source.getMaterialNumber());
        reprint.setMaterialName(source.getMaterialName());
        reprint.setStyleNumber(source.getStyleNumber());
        reprint.setColor(source.getColor());
        reprint.setBrandName(source.getBrandName());
        reprint.setKindName(source.getKindName());
        reprint.setSizeName(source.getSizeName());
        reprint.setPrintQty(source.getPrintQty());
        reprint.setTemplateFile(source.getTemplateFile());
        reprint.setPrinterName(source.getPrinterName());
        reprint.setPrintData(source.getPrintData());
        reprint.setAgentId(agentId);
        reprint.setStatus(0);
        reprint.setCreateTime(LocalDateTime.now());
        reprint.setRetryCount(0);
        reprint.setIsReprint(1);
        reprint.setSourceTaskId(sourceTaskId);
        reprint.setReprintReason(reason);
        taskMapper.insert(reprint);
        log.info("补打任务已创建: 原 {} → 新 {}，agent={}", sourceTaskId, reprint.getTaskId(), agentId);
        return reprint.getTaskId();
    }

    public List<PrintTask> getTasksByAgentId(String agentId) {
        return taskMapper.selectList(
            new LambdaQueryWrapper<PrintTask>()
                .eq(PrintTask::getAgentId, agentId)
                .orderByDesc(PrintTask::getCreateTime)
                .last("FETCH FIRST 100 ROWS ONLY")
        );
    }

    public IPage<PrintTask> getTasksByAgentIdPage(int pageNum, int pageSize, String agentId) {
        return taskMapper.selectPage(
            new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<PrintTask>()
                .eq(PrintTask::getAgentId, agentId)
                .orderByDesc(PrintTask::getPrintTime)
                .orderByDesc(PrintTask::getCreateTime)
        );
    }

    /**
     * 根据字段映射构建打印数据。
     * 如果没有配置映射，使用默认映射（兼容旧逻辑）。
     */
    private Map<String, String> buildPrintData(StickerPrintOrderDetail detail, List<PrintFieldMapping> mappings) {
        Map<String, String> printData = new HashMap<>();

        if (mappings == null || mappings.isEmpty()) {
            // 默认映射（兼容没有配置映射的情况）
            printData.put("MaterialNumber", detail.getMaterialNumber());
            printData.put("MaterialName", detail.getMaterialName());
            printData.put("StyleNumber", detail.getStyleNumber());
            printData.put("Color", detail.getColor());
            printData.put("BrandName", detail.getBrandName());
            printData.put("KindName", detail.getKindName());
            printData.put("SizeName", detail.getSizeName());
            printData.put("EAN13", detail.getEan13());
            printData.put("Price", detail.getPrice() != null ? detail.getPrice().toString() : "");
            return printData;
        }

        // 按映射配置构建
        for (PrintFieldMapping mapping : mappings) {
            String dbField = mapping.getDbField();
            String templateField = mapping.getTemplateField();
            String value = getFieldValue(detail, dbField);

            // 应用格式化规则
            if (value != null && mapping.getFieldFormat() != null && !mapping.getFieldFormat().isBlank()) {
                value = applyFormat(value, mapping.getFieldFormat());
            }

            printData.put(templateField, value != null ? value : "");
        }

        return printData;
    }

    /**
     * 根据字段名获取明细值（支持反射或直接映射）
     */
    private String getFieldValue(StickerPrintOrderDetail detail, String dbField) {
        if (dbField == null || dbField.isBlank()) return null;

        // 直接映射常用字段
        return switch (dbField.toUpperCase()) {
            case "MATERIAL_NUMBER", "MATERIALNUMBER" -> detail.getMaterialNumber();
            case "MATERIAL_NAME", "MATERIALNAME" -> detail.getMaterialName();
            case "STYLE_NUMBER", "STYLENUMBER" -> detail.getStyleNumber();
            case "COLOR" -> detail.getColor();
            case "BRAND_NAME", "BRANDNAME" -> detail.getBrandName();
            case "KIND_NAME", "KINDNAME" -> detail.getKindName();
            case "SIZE_NAME", "SIZENAME" -> detail.getSizeName();
            case "SIZE_GROUP", "SIZEGROUP" -> detail.getSizeGroup();
            case "EAN13" -> detail.getEan13();
            case "PRICE" -> detail.getPrice() != null ? detail.getPrice().toString() : null;
            case "EXECUTION_STANDARD", "EXECUTIONSTANDARD" -> detail.getExecutionStandard();
            case "ORIGIN" -> detail.getOrigin();
            case "MANUFACTURER" -> detail.getManufacturer();
            case "MANUFACTURER_ADDRESS", "MANUFACTURERADDRESS" -> detail.getManufacturerAddress();
            case "CONTACT_PHONE", "CONTACTPHONE" -> detail.getContactPhone();
            case "MATERIAL_COMPOSITION", "MATERIALCOMPOSITION" -> detail.getMaterialComposition();
            default -> null;
        };
    }

    /**
     * 应用格式化规则（简单实现）
     * 格式：fieldName:format  如 price:.2f
     */
    private String applyFormat(String value, String format) {
        if (value == null || format == null) return value;

        try {
            // 处理数字格式化，如 ".2f" 表示保留2位小数
            if (format.startsWith(".")) {
                double num = Double.parseDouble(value);
                int decimals = Integer.parseInt(format.substring(1, format.length() - 1));
                return String.format("%." + decimals + "f", num);
            }
        } catch (Exception e) {
            log.warn("格式化失败: value={}, format={}, error={}", value, format, e.getMessage());
        }

        return value;
    }
}
