# XSS 防护说明

## 概述

项目已实现服务端 XSS 防护，通过过滤器对所有请求的参数、请求头和请求体进行清洗，防止存储型 XSS 攻击。

## 实现架构

```
请求 → XSS 过滤器 → Shiro 认证 → Controller → 数据库
         ↓
    清洗恶意代码
    - 参数 (getParameter)
    - 请求头 (getHeader)
    - 请求体 (getInputStream)
```

## 文件结构

```
config/xss/
├── XssCleaner.java                 # XSS 清洗工具类
├── XssFilter.java                  # XSS 过滤器
└── XssHttpServletRequestWrapper.java  # 请求包装器
```

## 清洗规则

### 支持的清洗范围

| 范围 | 方法 | 说明 |
|------|------|------|
| URL 参数 | `getParameter()` | GET/POST 表单参数 |
| 请求头 | `getHeader()` | HTTP 请求头 |
| JSON 请求体 | `getInputStream()` | @RequestBody 数据 |

### 移除的危险标签

| 标签 | 说明 |
|------|------|
| `<script>` | JavaScript 脚本 |
| `<iframe>` | 内嵌框架 |
| `<object>` | 嵌入对象 |
| `<embed>` | 嵌入内容 |
| `<applet>` | Java 小程序 |
| `<svg>` | SVG 图形（支持 JS 事件） |
| `<style>` | CSS 样式（可嵌入表达式） |

### 移除的危险属性

| 属性 | 说明 |
|------|------|
| `onclick` | 点击事件 |
| `onerror` | 错误事件 |
| `onload` | 加载事件 |
| `onmouseover` | 鼠标悬停事件 |
| 其他 `on*` | 所有事件属性 |

### 移除的危险协议

| 协议 | 说明 |
|------|------|
| `javascript:` | JavaScript 协议 |
| `vbscript:` | VBScript 协议 |
| `data:` | Data 协议 |

### 其他防护

- 移除 CSS 表达式 `expression()`
- 大小写不敏感匹配
- 多行内容匹配

## 技术特点

### 1. 预编译正则表达式

所有正则表达式都预编译为 `static final Pattern` 常量，提高性能：

```java
private static final Pattern SCRIPT_PATTERN = Pattern.compile(
    "<script[^>]*>.*?</script>",
    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
);
```

### 2. 请求体缓存

使用 `StreamUtils.copyToByteArray()` 缓存请求体，避免重复读取：

```java
this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
```

### 3. 大小写不敏感

所有正则都使用 `Pattern.CASE_INSENSITIVE` 标志，防止大小写绕过：

```
<SCRIPT>alert(1)</SCRIPT>  → 被拦截
<ScRiPt>alert(1)</ScRiPt>  → 被拦截
```

### 4. 多行匹配

使用 `Pattern.DOTALL` 标志，防止多行绕过：

```
<script>
alert(1)
</script>
→ 被拦截
```

## 测试方法

### 测试脚本

```javascript
// 1. 测试 URL 参数
fetch('/api/test?input=<script>alert(1)</script>')

// 2. 测试 JSON 请求体
fetch('/api/test', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    name: '<script>alert("XSS")</script>测试内容'
  })
})
```

### 验证步骤

1. 启动应用
2. 调用包含 XSS 代码的 API
3. 检查返回值是否已清洗
4. 检查数据库中存储的值是否已清洗

## 注意事项

1. **不进行 HTML 转义**：输入层只做恶意代码删除，不做 HTML 转义，避免数据存储污染
2. **HTML 转义在输出层**：Thymeleaf 模板的 `th:text` 会自动进行 HTML 转义
3. **覆盖所有请求**：过滤器拦截所有路径，包括静态资源（静态资源通常不含用户输入）
4. **性能影响**：使用预编译正则和缓存请求体，性能影响可忽略

## 后续优化

1. **白名单机制**：允许特定 HTML 标签（如 `<b>`、`<i>`）
2. **日志记录**：记录被过滤的 XSS 攻击尝试
3. **告警通知**：检测到攻击时发送告警
