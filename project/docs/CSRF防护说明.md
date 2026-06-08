# CSRF 防护说明

## 概述

项目已实现 CSRF（跨站请求伪造）防护，通过双重机制保护状态修改类接口：
1. Session Cookie 的 SameSite 属性
2. CSRF Token 验证机制

## 防护架构

```
前端请求 → 携带 X-CSRF-Token → CSRF 过滤器 → 验证 Token → Controller
                                    ↓
                              验证失败返回 403
```

## 实现机制

### 1. SameSite Cookie

Session Cookie 设置了 `SameSite=Strict`，浏览器在跨站请求时不会携带 Cookie。

```java
// ShiroConfig.java
cookie.setAttribute("SameSite", "Strict");
```

### 2. CSRF Token 验证

- 服务端生成随机 Token 存储在 Session 中
- 前端每次请求在 Header 中携带 Token
- 服务端验证 Token 是否匹配

## 文件结构

```
config/
├── csrf/
│   └── CsrfFilter.java          # CSRF 过滤器
├── CsrfFilterConfig.java        # 过滤器配置
└── ShiroConfig.java             # Session Cookie 配置

controller/
└── AuthController.java          # CSRF Token 端点

frontend/src/api/
├── auth.js                      # 登录后获取 Token
└── request.js                   # 请求拦截器注入 Token
```

## 工作流程

### 1. 获取 Token

```
前端启动 / 登录成功
    ↓
GET /api/csrf
    ↓
生成 UUID Token
    ↓
存储到 Session + 返回给前端
    ↓
前端缓存到 localStorage
```

### 2. 请求验证

```
POST/PUT/DELETE 请求
    ↓
请求拦截器注入 X-CSRF-Token
    ↓
CSRF 过滤器拦截
    ↓
验证 Token 是否匹配
    ↓
匹配 → 继续处理
不匹配 → 返回 403
```

### 3. Token 刷新

```
403 CSRF 错误
    ↓
前端自动刷新 Token
    ↓
提示用户重试操作
```

## 豁免路径

以下路径不进行 CSRF 验证：

| 路径 | 原因 |
|------|------|
| GET/HEAD/OPTIONS 请求 | 安全方法，不修改状态 |
| `/doLogin` | 登录接口，用户尚未认证 |
| `/api/captcha` | 验证码接口，公开访问 |
| `/api/config/public` | 公开配置接口 |
| `/api/csrf` | Token 获取接口 |

## 测试方法

### 1. 获取 Token

```javascript
const res = await axios.get('/api/csrf')
const token = res.data
console.log('CSRF Token:', token)
```

### 2. 不带 Token 请求（预期失败）

```javascript
await axios.post('/api/user', { name: 'test' })
// 预期: 403 CSRF Token 无效
```

### 3. 带 Token 请求（预期成功）

```javascript
await axios.post('/api/user', { name: 'test' }, {
  headers: { 'X-CSRF-Token': token }
})
// 预期: 200 成功
```

## 注意事项

1. **登录后自动获取**：登录成功后会自动获取并缓存 CSRF Token
2. **自动刷新**：收到 403 CSRF 错误时会自动刷新 Token
3. **localStorage 缓存**：Token 缓存在 localStorage，刷新页面后恢复
4. **SameSite 兼容性**：现代浏览器都支持 SameSite 属性

## 兼容性

| 浏览器 | SameSite 支持 |
|--------|---------------|
| Chrome 80+ | ✅ |
| Firefox 69+ | ✅ |
| Safari 12+ | ✅ |
| Edge 80+ | ✅ |

## 后续优化

1. **Token 轮换**：定期更换 Token
2. **双重提交 Cookie**：备选方案
3. **SPA 集成**：与前端路由守卫集成
