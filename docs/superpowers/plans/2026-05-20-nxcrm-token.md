# 南讯CRM Token获取 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `task/nxcrm/` 下创建 `NxCrmApiClient.java`，将 `interfaceForYZ` 项目中 `NanXCRMUtils.java` 的 token 获取逻辑重构为与项目其他 ApiClient（QywxApiClient、YdApiClient）一致的 Spring 组件模式。

**Architecture:** 参考 `QywxApiClient` 的 token 缓存模式，使用南讯 SDK 的 `AccessTokenRegisterRequest` / `ApiClient` 获取 token，通过 `@Value` 注入配置，内存缓存 + 过期自动刷新。

**Tech Stack:** Spring Boot `@Component`，南讯 `ecrp-open-sdk` 10.20.0，`@Value` 配置注入，`ReentrantLock` 线程安全。

---

## File Structure

| 操作 | 文件路径 |
|------|----------|
| 创建 | `task/nxcrm/NxCrmApiClient.java` |
| 修改 | `application.yml` — 添加 nxcrm 配置节 |

---

### Task 1: 添加 application.yml 配置

**Files:**
- Modify: `src/main/resources/application.yml` (在 `ydkl:` 配置节之后追加)

- [ ] **Step 1: 在 application.yml 的 ydkl 配置节之后添加 nxcrm 配置**

在 `ydkl:` 配置块结束之后（约第200行之后），添加：

```yaml
# 南讯CRM配置
nxcrm:
  # 生产
  app-key: NS80000319002
  group-id: 80000319
  app-secret: 8F11A34C685C5FD4868FC3748F74CB54
  server-url: https://open-v6.vecrp.com
  # 测试
  #app-key: NS81000091178
  #group-id: 81000091
  #app-secret: C3EE07B4D087B2B4E6C1F3C56EEA8B74
  #server-url: https://sandbox-open.vecrp.com
```

- [ ] **Step 2: 验证 YAML 格式正确**

确认缩进与周围配置一致（2空格），无语法错误。

---

### Task 2: 创建 NxCrmApiClient.java

**Files:**
- Create: `src/main/java/com/bcsport/admin/task/nxcrm/NxCrmApiClient.java`

- [ ] **Step 1: 创建 NxCrmApiClient.java — 完整代码**

参考 `QywxApiClient` 的 token 缓存 + 双检锁模式，使用南讯 SDK 获取 token：

```java
package com.bcsport.admin.task.nxcrm;

import com.nascent.ecrp.opensdk.core.executeClient.ApiClient;
import com.nascent.ecrp.opensdk.core.executeClient.ApiClientImpl;
import com.nascent.ecrp.opensdk.request.basis.AccessTokenRegisterRequest;
import com.nascent.ecrp.opensdk.response.basis.AccessTokenRegisterResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component("nxCrmApiClient")
public class NxCrmApiClient {

    @Value("${nxcrm.app-key}")
    private String appKey;

    @Value("${nxcrm.group-id}")
    private Long groupId;

    @Value("${nxcrm.app-secret}")
    private String appSecret;

    @Value("${nxcrm.server-url}")
    private String serverUrl;

    private volatile String accessToken;
    private volatile LocalDateTime tokenExpireTime;
    private final ReentrantLock tokenRefreshLock = new ReentrantLock();

    @PostConstruct
    public void init() {
        log.info("NxCrmApiClient initialized, serverUrl={}", serverUrl);
    }

    public String getAccessToken() {
        if (accessToken != null && tokenExpireTime != null && LocalDateTime.now().isBefore(tokenExpireTime)) {
            return accessToken;
        }
        tokenRefreshLock.lock();
        try {
            if (accessToken != null && tokenExpireTime != null && LocalDateTime.now().isBefore(tokenExpireTime)) {
                return accessToken;
            }
            return doRefreshToken();
        } finally {
            tokenRefreshLock.unlock();
        }
    }

    public String refreshToken() {
        tokenRefreshLock.lock();
        try {
            return doRefreshToken();
        } finally {
            tokenRefreshLock.unlock();
        }
    }

    private String doRefreshToken() {
        AccessTokenRegisterRequest request = new AccessTokenRegisterRequest();
        request.setAppKey(appKey);
        request.setGroupId(groupId);
        request.setForceRefresh(true);
        request.setAppSecret(appSecret);
        request.setServerUrl(serverUrl);

        try {
            ApiClient apiClient = new ApiClientImpl(request);
            AccessTokenRegisterResponse response = apiClient.execute(request);
            if (!"200".equals(response.getCode())) {
                throw new RuntimeException("南讯CRM Token获取失败: " + response.getBody());
            }
            // 解析响应中的 token 和过期时间
            cn.hutool.json.JSONObject body = cn.hutool.json.JSONUtil.parseObj(response.getBody());
            String token = body.getStr("access_token");
            int expiresIn = body.getInt("expires_in", 7200);

            this.accessToken = token;
            this.tokenExpireTime = LocalDateTime.now().plusSeconds(expiresIn - 300);
            log.info("南讯CRM Token刷新成功, 有效期{}秒", expiresIn);
            return this.accessToken;
        } catch (Exception e) {
            log.error("南讯CRM Token刷新失败: {}", e.getMessage(), e);
            throw new RuntimeException("南讯CRM Token刷新失败", e);
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd E:\work\BC_Sport\BcSportsDataManageSystem\project && mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 3: 提交

- [ ] **Step 1: Git 提交**

```bash
git add src/main/java/com/bcsport/admin/task/nxcrm/NxCrmApiClient.java src/main/resources/application.yml
git commit -m "feat(nxcrm): 添加南讯CRM ApiClient，token获取与自动刷新"
```

---

## 验证

1. `mvn compile` 通过
2. 启动应用后日志出现 `NxCrmApiClient initialized`
3. 调用 `nxCrmApiClient.getAccessToken()` 能成功获取 token 并缓存
