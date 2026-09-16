package com.bcsport.admin.task.sys;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bcsport.admin.entity.JenkinsJobState;
import com.bcsport.admin.mapper.JenkinsJobStateMapper;
import com.bcsport.admin.service.ConfigService;
import com.bcsport.admin.service.notify.NotifyManager;
import com.bcsport.admin.service.notify.NotifyMessage;
import com.bcsport.admin.service.notify.NotifyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Jenkins构建监控任务
 *
 * 轮询 Jenkins 全部任务(一次 /api/json 拉齐,匿名只读,无需认证),全量同步到
 * BC_SPORTS_JENKINS_JOB_STATE 供"Jenkins监控"明细页展示:
 *   - 启用+停用任务都入库(buildable 列区分,页面默认只显示启用);
 *   - Jenkins 上已删除的任务行同步清理;
 *   - 告警只针对启用任务:新完成构建 FAILURE/UNSTABLE → 企微群机器人告警(NotifyType.ALERT),
 *     由失败恢复 SUCCESS → 恢复通知(NotifyType.INFO);
 *   - 首次见到的任务只记基线不告警(历史失败不刷屏);
 *   - ABORTED(人工取消)/NOT_BUILT 只记状态不告警。
 * 任务本身抛异常(如 Jenkins 不可达)时由调度框架按 FAIL_ONLY 策略推送任务失败通知。
 */
@Slf4j
@Component("jenkinsMonitorTask")
public class JenkinsMonitorTask {

    private static final String CONFIG_BASE_URL = "jenkins.monitor.baseUrl";
    private static final String DEFAULT_BASE_URL = "http://192.168.5.151:8085";

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId ZONE = ZoneId.of("GMT+8");

    private static final String RESULT_SUCCESS = "SUCCESS";
    private static final String RESULT_FAILURE = "FAILURE";
    private static final String RESULT_UNSTABLE = "UNSTABLE";

    @Autowired
    private ConfigService configService;

    @Autowired
    private NotifyManager notifyManager;

    @Autowired
    private JenkinsJobStateMapper jenkinsJobStateMapper;

    public void monitor() {
        String baseUrl = configService.getString(CONFIG_BASE_URL, DEFAULT_BASE_URL);
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        log.info("=== Jenkins构建监控开始: {} ===", baseUrl);

        JSONObject root = fetchJobs(baseUrl);
        JSONArray jobs = root.getJSONArray("jobs");
        if (jobs == null || jobs.isEmpty()) {
            log.warn("=== Jenkins构建监控: API返回jobs为空,跳过 ===");
            return;
        }

        List<String> alertBlocks = new ArrayList<>();
        List<String> recoverBlocks = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();
        int newBuilds = 0;

        for (int i = 0; i < jobs.size(); i++) {
            JSONObject job = jobs.getJSONObject(i);
            String name = job.getStr("name");
            if (name == null) {
                continue;
            }
            seenNames.add(name);
            boolean buildable = job.getBool("buildable", false);
            JSONObject build = job.getJSONObject("lastCompletedBuild");
            JSONObject lastSuccess = job.getJSONObject("lastSuccessfulBuild");

            JenkinsJobState state = jenkinsJobStateMapper.selectById(name);
            boolean firstSeen = state == null;
            if (firstSeen) {
                state = new JenkinsJobState();
                state.setJobName(name);
            }

            long number = build != null ? build.getLong("number", 0L) : 0L;
            String result = build != null ? build.getStr("result") : null;
            long lastNumber = state.getLastBuildNumber() == null ? 0L : state.getLastBuildNumber();

            // 告警/恢复判定:仅启用任务、非首次、且有新的完成构建(停用任务不会产生新构建,天然静默)
            if (!firstSeen && buildable && number > lastNumber) {
                newBuilds++;
                boolean isError = RESULT_FAILURE.equals(result) || RESULT_UNSTABLE.equals(result);
                boolean wasError = RESULT_FAILURE.equals(state.getLastResult()) || RESULT_UNSTABLE.equals(state.getLastResult());
                if (isError) {
                    alertBlocks.add(buildBlock(name, number, result,
                            build.getLong("timestamp", 0L), build.getStr("url")));
                } else if (RESULT_SUCCESS.equals(result) && wasError) {
                    recoverBlocks.add(buildBlock(name, number, result,
                            build.getLong("timestamp", 0L), build.getStr("url")));
                }
            }

            // 全量刷新展示字段(含停用任务)
            state.setLastBuildNumber(number > 0 ? number : null);
            state.setLastResult(result);
            state.setLastBuildTime(build != null ? toDate(build.getLong("timestamp", 0L)) : null);
            state.setLastDurationMs(build != null ? build.getLong("duration", 0L) : null);
            state.setLastSuccessBuild(lastSuccess != null ? lastSuccess.getLong("number", 0L) : null);
            state.setLastSuccessTime(lastSuccess != null ? toDate(lastSuccess.getLong("timestamp", 0L)) : null);
            state.setNextBuildNumber(job.getLong("nextBuildNumber", 0L) > 0 ? job.getLong("nextBuildNumber", 0L) : null);
            state.setHealthScore(firstHealthScore(job));
            state.setColor(job.getStr("color"));
            state.setBuildable(buildable ? 1 : 0);
            state.setInQueue(job.getBool("inQueue", false) ? 1 : 0);
            state.setJobUrl(job.getStr("url"));
            state.setUpdateTime(new Date());
            if (firstSeen) {
                jenkinsJobStateMapper.insert(state);
            } else {
                jenkinsJobStateMapper.updateById(state);
            }
        }

        // 清理 Jenkins 上已删除的任务
        int removed = 0;
        List<JenkinsJobState> all = jenkinsJobStateMapper.selectList(null);
        for (JenkinsJobState row : all) {
            if (!seenNames.contains(row.getJobName())) {
                jenkinsJobStateMapper.deleteById(row.getJobName());
                removed++;
            }
        }

        if (!alertBlocks.isEmpty()) {
            notifyManager.send(NotifyMessage.builder()
                    .title("Jenkins构建失败告警(" + alertBlocks.size() + "个)")
                    .type(NotifyType.ALERT)
                    .content(String.join("\n――――――――\n", alertBlocks))
                    .build());
        }
        if (!recoverBlocks.isEmpty()) {
            notifyManager.send(NotifyMessage.builder()
                    .title("Jenkins构建恢复通知(" + recoverBlocks.size() + "个)")
                    .type(NotifyType.INFO)
                    .content(String.join("\n――――――――\n", recoverBlocks))
                    .build());
        }

        log.info("=== Jenkins构建监控完成: 任务数{}, 新完成构建{}, 失败告警{}, 恢复通知{}, 清理已删任务{} ===",
                jobs.size(), newBuilds, alertBlocks.size(), recoverBlocks.size(), removed);
    }

    private Integer firstHealthScore(JSONObject job) {
        JSONArray reports = job.getJSONArray("healthReport");
        if (reports == null || reports.isEmpty()) {
            return null;
        }
        return reports.getJSONObject(0).getInt("score");
    }

    private Date toDate(long epochMilli) {
        return epochMilli > 0 ? Date.from(Instant.ofEpochMilli(epochMilli)) : null;
    }

    /**
     * 消息块：行首关键词(任务名称/执行状态/错误信息)与 WechatChannel 的着色规则对齐
     */
    private String buildBlock(String name, long number, String result, long timestamp, String url) {
        StringBuilder sb = new StringBuilder();
        sb.append("任务名称：").append(name).append(" #").append(number).append("\n");
        sb.append("执行状态：").append(RESULT_SUCCESS.equals(result) ? "成功" : "失败").append("(").append(result).append(")\n");
        sb.append("提交时间：").append(timestamp > 0
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZONE).format(TS_FMT) : "-").append("\n");
        if (url != null && !url.isBlank()) {
            sb.append("构建链接：").append(url);
        }
        return sb.toString();
    }

    private JSONObject fetchJobs(String baseUrl) {
        String apiUrl = baseUrl + "/api/json?tree=jobs[name,url,color,buildable,inQueue,healthReport[score],"
                + "lastCompletedBuild[number,result,timestamp,duration,url],"
                + "lastSuccessfulBuild[number,timestamp],nextBuildNumber]";
        // try-with-resources 显式归还连接(同 WechatChannel,防高频轮询句柄堆积)
        try (HttpResponse response = HttpRequest.get(apiUrl)
                .timeout(10000)
                .execute()) {
            if (response.getStatus() != 200) {
                throw new IllegalStateException("Jenkins API返回HTTP " + response.getStatus());
            }
            return JSONUtil.parseObj(response.body());
        }
    }
}
