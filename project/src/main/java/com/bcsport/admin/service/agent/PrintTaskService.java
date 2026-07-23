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

import java.time.Duration;
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

    /** 卡住判定动态阈值基础分钟数：阈值 = stuckBaseMinutes + printQty × stuckMinutesPerQty。
     *  按任务打印数量自适应，避免大数量任务(如1000张)因物理打印超时而被误判卡住导致重复打印。 */
    @Value("${agent.task.stuck-base-minutes:3}")
    private int stuckBaseMinutes;

    /** 卡住判定每张追加分钟数，配合 stuckBaseMinutes 动态计算超时阈值。 */
    @Value("${agent.task.stuck-minutes-per-qty:0.02}")
    private double stuckMinutesPerQty;

    /** 单个任务最多重新派发次数，超过则标记失败，避免无限重发导致重复打印。 */
    @Value("${agent.task.max-redispatch:3}")
    private int maxRedispatch;

    /** status=0 任务超过该分钟数仍未被拉取，巡检任务告警（agentId 配错 / Agent 长时间离线）。 */
    private static final int PENDING_WARN_MINUTES = 15;

    /** 计算任务卡住超时阈值（分钟）：基础 + 每张追加，按 printQty 自适应。 */
    private double stuckThresholdMinutes(PrintTask task) {
        int qty = task.getPrintQty() != null ? task.getPrintQty() : 1;
        return stuckBaseMinutes + qty * stuckMinutesPerQty;
    }

    /** 判断打印中(status=1)的任务是否已超时卡住（按 printQty 动态阈值）。
     *  用 Duration 保留小数精度，避免 minusMinutes(long) 截断导致 per-qty 对小数量失效。 */
    private boolean isStuck(PrintTask task) {
        if (task.getDispatchTime() == null) return false;
        double thresholdMinutes = stuckThresholdMinutes(task);
        long thresholdMillis = (long) (thresholdMinutes * 60_000);
        return task.getDispatchTime().isBefore(LocalDateTime.now().minus(Duration.ofMillis(thresholdMillis)));
    }

    public List<PrintTask> pullTasks(String agentId) {
        // status=0(待打印) / status=4(已暂停) 直接拉取；
        // status=1(打印中) 先用基础阈值(base，所有任务的最小阈值)在 SQL 预筛——比 base 还新的绝对没卡住，不捞出来挤占名额；
        //                捞出的候选再在 Java 侧按 printQty 动态阈值精确判断是否真卡住。
        LocalDateTime baseStuckBefore = LocalDateTime.now().minusMinutes(stuckBaseMinutes);
        List<PrintTask> tasks = taskMapper.selectList(
            new LambdaQueryWrapper<PrintTask>()
                .eq(PrintTask::getAgentId, agentId)
                .and(w -> w
                    .in(PrintTask::getStatus, 0, 4)
                    .or(n -> n.eq(PrintTask::getStatus, 1).lt(PrintTask::getDispatchTime, baseStuckBefore))
                )
                .orderByAsc(PrintTask::getCreateTime)
                .last("FETCH FIRST 10 ROWS ONLY")
        );

        List<PrintTask> dispatched = new ArrayList<>();
        for (PrintTask task : tasks) {
            Integer st = task.getStatus();
            // 打印中(status=1)：按 printQty 动态阈值判断，未超时说明还在正常打印，跳过避免误重派
            if (st != null && st == 1) {
                if (!isStuck(task)) {
                    continue; // 还在正常打印中，不重派
                }
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
            // 暂停态(status=4)续打：换纸后的正常恢复，不消耗 retryCount 配额
            // （retryCount 本意是防 Agent 崩溃无限重派，断纸换纸是正常操作不该被计入）
            if (st != null && st == 4) {
                log.info("暂停任务 {} 换纸后续打", task.getTaskId());
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

        // status=1/4 告警阈值：取 base×(maxRedispatch+1) 与保底值 60 分钟的较大者，
        // 确保大数量任务(如1000张≈23分)正常打印时不会被误报为派发超时
        long severeMinutes = Math.max((long) stuckBaseMinutes * (maxRedispatch + 1), 60);
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

        // 暂停态(status=4)长时间未续打告警：可能忘换纸 / Agent 长时间未恢复
        List<PrintTask> paused = taskMapper.selectList(
            new LambdaQueryWrapper<PrintTask>()
                .eq(PrintTask::getStatus, 4)
                .lt(PrintTask::getDispatchTime, now.minusMinutes(severeMinutes))
                .last("FETCH FIRST 20 ROWS ONLY")
        );
        if (!paused.isEmpty()) {
            log.warn("巡检: {} 个暂停任务(status=4)超 {} 分钟仍未续打，请检查是否换纸 / Agent 是否恢复: {}",
                paused.size(), severeMinutes,
                paused.stream().map(t -> t.getTaskId() + "@(" + t.getAgentId() + ")").limit(5).collect(Collectors.joining(", ")));
        }
    }

    /**
     * Agent 回报打印结果。
     * @param taskId 任务ID
     * @param resultStatus 语义状态：completed(成功) / failed(失败) / paused(暂停，断纸换纸后续打)
     *                      为 null 时按老协议 success 布尔值判断（向后兼容）
     * @param success 老协议布尔值（resultStatus 为 null 时生效）
     * @param message 回报消息（失败原因 / 暂停原因）
     */
    public void reportResult(String taskId, String resultStatus, Boolean success, String message) {
        PrintTask task = taskMapper.selectOne(
            new LambdaQueryWrapper<PrintTask>()
                .eq(PrintTask::getTaskId, taskId)
        );

        if (task == null) {
            return;
        }

        // 解析语义状态：传了 resultStatus 优先用，没传则按老协议 success 推导
        String status = resultStatus;
        if (status == null || status.isBlank()) {
            status = Boolean.TRUE.equals(success) ? "completed" : "failed";
        }
        status = status.trim().toLowerCase();

        switch (status) {
            case "completed":
                task.setStatus(2);
                task.setErrorMsg(null);
                task.setPrintTime(LocalDateTime.now());
                break;
            case "paused":
                // 暂停：断纸等异常，换纸后由 pullTasks 自动续打。不记 printTime、不累 retryCount。
                task.setStatus(4);
                task.setErrorMsg(message != null && !message.isBlank() ? message : "打印暂停（断纸/缺纸）");
                break;
            case "failed":
            default:
                task.setStatus(3);
                task.setErrorMsg(message);
                task.setPrintTime(LocalDateTime.now());
                break;
        }
        taskMapper.updateById(task);
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

        // 幂等：若该订单已有未完成任务(待打印0/打印中1/已暂停4)，拒绝重复下发，避免重复打印
        Long pending = taskMapper.selectCount(
            new LambdaQueryWrapper<PrintTask>()
                .eq(PrintTask::getOrderId, orderId)
                .in(PrintTask::getStatus, 0, 1, 4)
        );
        if (pending != null && pending > 0) {
            throw new BusinessException("该申请单已有未完成的打印任务（含暂停中），请勿重复下发");
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

        // 本次下发共享同一个批次号，便于在任务记录中区分"同一批次"
        String batchId = UUID.randomUUID().toString().replace("-", "");

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
            task.setLocalSizeName(detail.getLocalSizeName());
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
            task.setBatchId(batchId);

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
                .in(PrintTask::getStatus, 0, 1, 4)
        );
        if (pending != null && pending > 0) {
            throw new BusinessException("该任务已有未完成的补打任务（含暂停中），请等待打印完成");
        }

        PrintTask reprint = new PrintTask();
        reprint.setTaskId(UUID.randomUUID().toString().replace("-", ""));
        reprint.setBatchId(source.getBatchId());
        reprint.setOrderNo(source.getOrderNo());
        reprint.setOrderId(source.getOrderId());
        reprint.setMaterialNumber(source.getMaterialNumber());
        reprint.setMaterialName(source.getMaterialName());
        reprint.setStyleNumber(source.getStyleNumber());
        reprint.setColor(source.getColor());
        reprint.setBrandName(source.getBrandName());
        reprint.setKindName(source.getKindName());
        reprint.setSizeName(source.getSizeName());
        reprint.setLocalSizeName(source.getLocalSizeName());
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

    /**
     * 分页查询 Agent 的打印任务，支持按批次号筛选（前缀匹配，便于按"同一批次"排查）。
     */
    public IPage<PrintTask> getTasksByAgentIdPage(int pageNum, int pageSize, String agentId, String batchId, String orderNo) {
        LambdaQueryWrapper<PrintTask> wrapper = new LambdaQueryWrapper<PrintTask>()
                .eq(PrintTask::getAgentId, agentId);
        if (batchId != null && !batchId.isBlank()) {
            wrapper.likeRight(PrintTask::getBatchId, batchId.trim());
        }
        if (orderNo != null && !orderNo.isBlank()) {
            wrapper.likeRight(PrintTask::getOrderNo, orderNo.trim());
        }
        wrapper.orderByDesc(PrintTask::getPrintTime).orderByDesc(PrintTask::getCreateTime);
        return taskMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
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
            printData.put("SizeName", resolveSizeName(detail));
            printData.put("EAN13", detail.getEan13());
            printData.put("Price", detail.getPrice() != null ? detail.getPrice().toString() : "");
            return printData;
        }

        // 按映射配置构建
        for (PrintFieldMapping mapping : mappings) {
            String dbField = mapping.getDbField();
            String templateField = mapping.getTemplateField();

            // dbField 为空时使用默认值（固定值场景，如标题/检验员等）
            String value;
            if (dbField == null || dbField.isBlank()) {
                value = mapping.getDefaultValue();
            } else {
                value = getFieldValue(detail, dbField);
                // 货品资料取不到值时，回退到默认值
                if (value == null && mapping.getDefaultValue() != null && !mapping.getDefaultValue().isBlank()) {
                    value = mapping.getDefaultValue();
                }
            }

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
            case "SIZE_NAME", "SIZENAME" -> resolveSizeName(detail);
            case "SIZE_GROUP", "SIZEGROUP" -> detail.getSizeGroup();
            case "EAN13" -> detail.getEan13();
            case "PRICE" -> detail.getPrice() != null ? detail.getPrice().toString() : null;
            case "EXECUTION_STANDARD", "EXECUTIONSTANDARD" -> detail.getExecutionStandard();
            case "ORIGIN" -> detail.getOrigin();
            case "MANUFACTURER" -> detail.getManufacturer();
            case "MANUFACTURER_ADDRESS", "MANUFACTURERADDRESS" -> detail.getManufacturerAddress();
            case "CONTACT_PHONE", "CONTACTPHONE" -> detail.getContactPhone();
            case "FAB_CODE", "FABCODE" -> detail.getFabCode();
            case "FAB_ELEMENT", "FABELEMENT" -> detail.getFabElement();
            case "AC_CODE", "ACCODE" -> detail.getAcCode();
            case "ACC_ELEMENT", "ACCELEMENT" -> detail.getAccElement();
            default -> null;
        };
    }

    /**
     * 解析尺码名称：以矫正尺码(localSizeName)优先，为空时回退到原始尺码(sizeName)。
     */
    private String resolveSizeName(StickerPrintOrderDetail detail) {
        String local = detail.getLocalSizeName();
        if (local != null && !local.isBlank()) {
            return local;
        }
        return detail.getSizeName();
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
