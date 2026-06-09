<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">系统设置</span>
          <el-button v-if="hasPermission('system:config:edit')" type="primary" :icon="Check" @click="handleSave" :loading="saving">保存设置</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 基础配置 -->
        <el-tab-pane label="基础配置" name="basic">
          <el-form label-width="140px" class="settings-form" v-loading="loading">
            <el-form-item label="系统名称">
              <el-input v-model="form['sys.name']" placeholder="BC体育数据管理系统" style="max-width: 400px" />
              <div class="form-tip">显示在页面标题和登录页</div>
            </el-form-item>

            <el-form-item label="默认分页大小">
              <el-select v-model="form['sys.pageSize']" style="max-width: 200px">
                <el-option label="10 条/页" value="10" />
                <el-option label="20 条/页" value="20" />
                <el-option label="50 条/页" value="50" />
                <el-option label="100 条/页" value="100" />
              </el-select>
              <div class="form-tip">列表页面默认每页显示条数</div>
            </el-form-item>

            <el-form-item label="日期格式">
              <el-select v-model="form['sys.dateFormat']" style="max-width: 300px">
                <el-option label="yyyy-MM-dd HH:mm:ss" value="yyyy-MM-dd HH:mm:ss" />
                <el-option label="yyyy/MM/dd HH:mm:ss" value="yyyy/MM/dd HH:mm:ss" />
                <el-option label="yyyy-MM-dd" value="yyyy-MM-dd" />
                <el-option label="dd/MM/yyyy HH:mm:ss" value="dd/MM/yyyy HH:mm:ss" />
              </el-select>
            </el-form-item>

            <el-form-item label="时区">
              <el-select v-model="form['sys.timezone']" style="max-width: 300px">
                <el-option label="GMT+8 (中国标准时间)" value="GMT+8" />
                <el-option label="GMT+9 (日本标准时间)" value="GMT+9" />
                <el-option label="GMT+0 (格林威治时间)" value="GMT+0" />
              </el-select>
            </el-form-item>

            <el-divider content-position="left">文件上传</el-divider>

            <el-form-item label="上传大小限制">
              <el-input-number v-model.number="form['sys.uploadMaxSize']" :min="1" :max="500" />
              <span style="margin-left: 8px; color: #606266">MB</span>
              <div class="form-tip">单个文件最大上传大小</div>
            </el-form-item>

            <el-form-item label="允许上传类型">
              <el-input v-model="form['sys.uploadAllowedTypes']" placeholder=".jpg,.png,.pdf,.xlsx,.docx" style="max-width: 400px" />
              <div class="form-tip">多个扩展名用英文逗号分隔，留空表示不限制</div>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 安全策略 -->
        <el-tab-pane label="安全策略" name="security">
          <el-form label-width="180px" class="settings-form" v-loading="loading">
            <el-divider content-position="left">密码规则</el-divider>

            <el-form-item label="密码最小长度">
              <el-input-number v-model.number="form['security.passwordMinLength']" :min="4" :max="32" />
              <div class="form-tip">最少字符数</div>
            </el-form-item>

            <el-form-item label="必须包含大写字母">
              <el-switch v-model="form['security.passwordRequireUpper']" active-value="true" inactive-value="false" />
            </el-form-item>

            <el-form-item label="必须包含数字">
              <el-switch v-model="form['security.passwordRequireNumber']" active-value="true" inactive-value="false" />
            </el-form-item>

            <el-divider content-position="left">登录安全</el-divider>

            <el-form-item label="登录失败锁定次数">
              <el-input-number v-model.number="form['security.loginMaxRetry']" :min="1" :max="20" />
              <div class="form-tip">连续失败 N 次后锁定账号</div>
            </el-form-item>

            <el-form-item label="锁定时间(分钟)">
              <el-input-number v-model.number="form['security.loginLockMinutes']" :min="5" :max="1440" />
              <div class="form-tip">账号锁定持续时间</div>
            </el-form-item>

            <el-form-item label="会话超时(分钟)">
              <el-input-number v-model.number="form['security.sessionTimeout']" :min="10" :max="1440" />
              <div class="form-tip">超过此时间未操作自动登出，同时影响 CSRF Token 有效期</div>
            </el-form-item>

            <el-form-item label="验证码开关">
              <el-switch v-model="form['security.captchaEnabled']" active-value="true" inactive-value="false" />
              <div class="form-tip">登录时是否需要输入验证码</div>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 界面主题 -->
        <el-tab-pane label="界面主题" name="theme">
          <el-form label-width="140px" class="settings-form" v-loading="loading">
            <el-divider content-position="left">主题模式</el-divider>

            <el-form-item label="主题模式">
              <el-radio-group v-model="form['sys.themeMode']">
                <el-radio-button value="light">浅色</el-radio-button>
                <el-radio-button value="dark">深色</el-radio-button>
                <el-radio-button value="auto">跟随系统</el-radio-button>
              </el-radio-group>
              <div class="form-tip">选择界面显示风格，深色模式适合夜间使用</div>
            </el-form-item>

            <el-divider content-position="left">主题色</el-divider>

            <el-form-item label="主色调">
              <div class="color-picker-row">
                <el-color-picker v-model="form['sys.primaryColor']" :predefine="presetColors" @change="previewPrimaryColor" />
                <span class="color-value">{{ form['sys.primaryColor'] }}</span>
              </div>
              <div class="form-tip">全局主题色，影响按钮、链接、选中状态等</div>
            </el-form-item>

            <div class="color-presets">
              <span class="preset-label">预设色板：</span>
              <div
                v-for="c in presetColors"
                :key="c"
                class="preset-swatch"
                :class="{ active: form['sys.primaryColor'] === c }"
                :style="{ background: c }"
                @click="form['sys.primaryColor'] = c; previewPrimaryColor(c)"
              >
                <el-icon v-if="form['sys.primaryColor'] === c" :size="12"><Check /></el-icon>
              </div>
            </div>

            <el-divider content-position="left">侧边栏</el-divider>

            <el-form-item label="侧边栏风格">
              <el-radio-group v-model="form['sys.sidebarStyle']">
                <el-radio-button value="dark">深色</el-radio-button>
                <el-radio-button value="light">浅色</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-divider content-position="left">Logo</el-divider>

            <el-form-item label="系统 Logo">
              <div class="logo-upload-area">
                <el-upload
                  class="logo-uploader"
                  action="/bcsports/api/config/upload-logo"
                  :headers="uploadHeaders"
                  :show-file-list="false"
                  accept="image/png,image/jpeg,image/svg+xml"
                  :on-success="handleLogoSuccess"
                  :before-upload="beforeLogoUpload"
                >
                  <img v-if="form['sys.logoUrl']" :src="logoFullUrl" class="logo-preview" />
                  <div v-else class="logo-placeholder">
                    <el-icon :size="24"><Plus /></el-icon>
                    <span>上传 Logo</span>
                  </div>
                </el-upload>
                <el-button v-if="form['sys.logoUrl']" text type="danger" size="small" @click="form['sys.logoUrl'] = ''">移除</el-button>
              </div>
              <div class="form-tip">建议尺寸 120×32 像素，支持 PNG / JPG / SVG</div>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 通知配置 -->
        <el-tab-pane label="通知配置" name="notify">
          <el-form label-width="160px" class="settings-form" v-loading="loading">
            <el-divider content-position="left">企业微信群机器人</el-divider>

            <el-form-item label="Webhook URL">
              <el-input
                v-model="form['schedule.notify.webhookUrl']"
                placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx"
                style="max-width: 500px"
              >
                <template #append>
                  <el-button @click="testWebhook" :loading="testLoading">测试</el-button>
                </template>
              </el-input>
              <div class="form-tip">
                企业微信群机器人的 Webhook 地址，用于接收定时任务执行通知。
                <br>获取方式：群聊 → 右上角「...」→「群机器人」→「添加」→ 复制 Webhook 地址
              </div>
            </el-form-item>

            <el-form-item label="默认推送策略">
              <el-select v-model="form['schedule.notify.defaultStrategy']" style="max-width: 200px">
                <el-option label="不推送" value="DISABLED" />
                <el-option label="仅失败推送" value="FAIL_ONLY" />
                <el-option label="总是推送" value="ALWAYS" />
              </el-select>
              <div class="form-tip">新建定时任务时的默认推送策略（可在任务中单独修改）</div>
            </el-form-item>

            <el-divider content-position="left">消息格式预览</el-divider>

            <div class="notify-preview">
              <div class="preview-card success">
                <div class="preview-title">✅ 任务成功通知</div>
                <div class="preview-content">
                  <div>任务名称：iHR员工同步</div>
                  <div>执行状态：成功</div>
                  <div>触发类型：CRON定时</div>
                  <div>开始时间：2026-06-05 10:00:00</div>
                  <div>结束时间：2026-06-05 10:00:02</div>
                  <div>执行耗时：2.3s</div>
                </div>
              </div>
              <div class="preview-card fail">
                <div class="preview-title">❌ 任务失败通知</div>
                <div class="preview-content">
                  <div>任务名称：南讯CRM会员标签推送</div>
                  <div>执行状态：失败</div>
                  <div>触发类型：手动触发</div>
                  <div>开始时间：2026-06-05 10:05:00</div>
                  <div>结束时间：2026-06-05 10:05:45</div>
                  <div>执行耗时：45.2s</div>
                  <div>错误信息：API调用失败...</div>
                </div>
              </div>
            </div>
          </el-form>
        </el-tab-pane>

        <!-- 系统维护 -->
        <el-tab-pane label="系统维护" name="maintenance">
          <div class="maintenance-section">
            <!-- 健康检查 -->
            <div class="maint-card">
              <div class="maint-card-header">
                <div>
                  <h4>系统健康检查</h4>
                  <p class="maint-desc">检查各组件运行状态</p>
                </div>
                <el-button type="primary" plain :loading="healthLoading" @click="checkHealth">刷新状态</el-button>
              </div>
              <div v-if="healthInfo" class="health-grid">
                <div class="health-item">
                  <span class="health-label">Redis</span>
                  <el-tag :type="healthInfo.redis === '正常' ? 'success' : 'danger'" size="small">{{ healthInfo.redis }}</el-tag>
                </div>
                <div class="health-item">
                  <span class="health-label">数据库</span>
                  <el-tag :type="healthInfo.database === '正常' ? 'success' : 'danger'" size="small">{{ healthInfo.database }}</el-tag>
                </div>
                <div class="health-item">
                  <span class="health-label">磁盘使用</span>
                  <el-tag :type="getDiskTagType(healthInfo.diskUsedPercent)" size="small">
                    {{ healthInfo.diskUsedPercent }} ({{ healthInfo.diskUsed }} / {{ healthInfo.diskTotal }})
                  </el-tag>
                </div>
                <div class="health-item">
                  <span class="health-label">JVM 内存</span>
                  <el-tag type="info" size="small">{{ healthInfo.jvmUsed }} / {{ healthInfo.jvmTotal }}</el-tag>
                </div>
                <div class="health-item">
                  <span class="health-label">配置缓存</span>
                  <el-tag :type="healthInfo.configCount === '正常加载' ? 'success' : 'warning'" size="small">{{ healthInfo.configCount }}</el-tag>
                </div>
              </div>
              <el-empty v-else description="点击上方按钮检查系统状态" :image-size="60" />
            </div>

            <!-- 缓存清理 -->
            <div class="maint-card">
              <div class="maint-card-header">
                <div>
                  <h4>缓存管理</h4>
                  <p class="maint-desc">清除 Redis 缓存并重新加载系统配置（不影响登录会话）</p>
                </div>
                <el-button type="warning" plain :loading="cacheClearing" @click="clearCache">清除缓存</el-button>
              </div>
            </div>

            <!-- 日志清理 -->
            <div class="maint-card">
              <div class="maint-card-header">
                <div>
                  <h4>日志清理</h4>
                  <p class="maint-desc">清理指定天数前的操作日志记录</p>
                </div>
                <div style="display: flex; align-items: center; gap: 12px">
                  <el-select v-model="logCleanDays" style="width: 140px">
                    <el-option label="7 天前" :value="7" />
                    <el-option label="30 天前" :value="30" />
                    <el-option label="90 天前" :value="90" />
                    <el-option label="180 天前" :value="180" />
                    <el-option label="全部" :value="0" />
                  </el-select>
                  <el-button type="danger" plain :loading="logCleaning" @click="cleanLogs">清理日志</el-button>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
defineOptions({ name: 'Settings' })
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Plus } from '@element-plus/icons-vue'
import { getConfigs, updateConfigs } from '@/api/config'
import { usePermission } from '@/composables/usePermission'
import { useThemeStore, applyPrimaryColor } from '@/stores/theme'
import request from '@/api/request'

const apiBase = import.meta.env.VITE_API_BASE || ''

const { hasPermission } = usePermission()
const themeStore = useThemeStore()
const activeTab = ref('basic')
const loading = ref(false)
const saving = ref(false)

const presetColors = [
  '#1d4ed8', '#2563eb', '#3b82f6',
  '#7c3aed', '#8b5cf6', '#a855f7',
  '#059669', '#10b981', '#34d399',
  '#dc2626', '#ef4444', '#f97316',
  '#0891b2', '#0d9488', '#4f46e5'
]

const form = reactive({
  'sys.name': '',
  'sys.pageSize': '20',
  'sys.dateFormat': 'yyyy-MM-dd HH:mm:ss',
  'sys.timezone': 'GMT+8',
  'sys.uploadMaxSize': 10,
  'sys.uploadAllowedTypes': '',
  'security.passwordMinLength': 6,
  'security.passwordRequireUpper': 'false',
  'security.passwordRequireNumber': 'true',
  'security.loginMaxRetry': 5,
  'security.loginLockMinutes': 30,
  'security.sessionTimeout': 120,
  'security.captchaEnabled': 'true',
  'sys.themeMode': 'light',
  'sys.primaryColor': '#1d4ed8',
  'sys.sidebarStyle': 'dark',
  'sys.logoUrl': '',
  'schedule.notify.webhookUrl': '',
  'schedule.notify.defaultStrategy': 'FAIL_ONLY'
})

// 维护相关
const healthLoading = ref(false)
const healthInfo = ref(null)
const cacheClearing = ref(false)
const logCleaning = ref(false)
const logCleanDays = ref(30)

// 通知测试
const testLoading = ref(false)

const uploadHeaders = computed(() => {
  const token = document.cookie.match(/JSESSIONID=[^;]+/)?.[0]
  return token ? { 'X-Requested-With': 'XMLHttpRequest', Cookie: token } : {}
})

const logoFullUrl = computed(() => {
  const url = form['sys.logoUrl']
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('data:')) return url
  return apiBase + url
})

async function loadConfigs() {
  loading.value = true
  try {
    const res = await getConfigs()
    if (res.code === 200) {
      const list = res.data || []
      list.forEach(item => {
        if (item.configKey in form) {
          form[item.configKey] = item.configValue
        }
      })
    }
  } catch (e) {
    console.error('加载配置失败', e)
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    const data = {}
    Object.keys(form).forEach(key => {
      data[key] = String(form[key])
    })
    const res = await updateConfigs(data)
    if (res.code === 200) {
      ElMessage.success('设置已保存')
      if (data['sys.themeMode']) themeStore.themeMode = data['sys.themeMode']
      if (data['sys.primaryColor']) themeStore.primaryColor = data['sys.primaryColor']
      if (data['sys.sidebarStyle']) themeStore.sidebarStyle = data['sys.sidebarStyle']
      if (data['sys.logoUrl'] !== undefined) themeStore.logoUrl = data['sys.logoUrl']
      themeStore.applyTheme()
    }
  } catch (e) {
    // interceptor handles error messages
  } finally {
    saving.value = false
  }
}

function getDiskTagType(percent) {
  if (!percent) return 'info'
  const num = parseInt(percent)
  if (num >= 90) return 'danger'
  if (num >= 75) return 'warning'
  return 'success'
}

async function checkHealth() {
  healthLoading.value = true
  try {
    const res = await request.get('/api/maintenance/health')
    if (res.code === 200) {
      healthInfo.value = res.data
    }
  } catch {
    // handled by interceptor
  } finally {
    healthLoading.value = false
  }
}

async function testWebhook() {
  const url = form['schedule.notify.webhookUrl']
  if (!url) {
    ElMessage.warning('请先输入 Webhook URL')
    return
  }
  if (!url.startsWith('https://qyapi.weixin.qq.com/')) {
    ElMessage.warning('Webhook URL 格式不正确，应以 https://qyapi.weixin.qq.com/ 开头')
    return
  }

  testLoading.value = true
  try {
    const res = await request.post('/api/notify/test-webhook', { webhookUrl: url })
    if (res.code === 200) {
      ElMessage.success('测试消息已发送，请查看企业微信群')
    } else {
      ElMessage.error(res.message || '测试失败')
    }
  } catch {
    // handled by interceptor
  } finally {
    testLoading.value = false
  }
}

async function clearCache() {
  try {
    await ElMessageBox.confirm('确定要清除系统缓存吗？这将重新加载所有配置。', '确认操作', {
      confirmButtonText: '确定清除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch { return }

  cacheClearing.value = true
  try {
    const res = await request.post('/api/maintenance/clear-cache')
    if (res.code === 200) {
      ElMessage.success('缓存已清除')
      await loadConfigs()
    }
  } catch {
    // handled by interceptor
  } finally {
    cacheClearing.value = false
  }
}

async function cleanLogs() {
  const label = logCleanDays.value === 0 ? '全部' : logCleanDays.value + ' 天前'
  try {
    await ElMessageBox.confirm(`确定要清理 ${label} 的操作日志吗？此操作不可撤销。`, '确认操作', {
      confirmButtonText: '确定清理',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch { return }

  logCleaning.value = true
  try {
    const res = await request.post(`/api/maintenance/clean-logs?days=${logCleanDays.value}`)
    if (res.code === 200) {
      ElMessage.success(res.message || '日志已清理')
    }
  } catch {
    // handled by interceptor
  } finally {
    logCleaning.value = false
  }
}

onMounted(() => {
  loadConfigs()
})

function previewPrimaryColor(color) {
  if (color) applyPrimaryColor(color)
}

function beforeLogoUpload(file) {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('仅支持图片文件')
    return false
  }
  if (file.size / 1024 > 512) {
    ElMessage.error('Logo 图片不能超过 512KB')
    return false
  }
  return true
}

function handleLogoSuccess(response) {
  if (response.code === 200 && response.data) {
    form['sys.logoUrl'] = response.data
    ElMessage.success('Logo 上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}
</script>

<style scoped>
.settings-form {
  max-width: 640px;
  padding: 16px 0;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.maintenance-section {
  max-width: 720px;
  padding: 8px 0;
}

.maint-card {
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 20px 24px;
  margin-bottom: 16px;
}

.maint-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.maint-card-header h4 {
  margin: 0 0 4px 0;
  font-size: 15px;
  color: #303133;
}

.maint-desc {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.health-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.health-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.health-label {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
}

/* ===== Theme Tab ===== */
.color-picker-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.color-value {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: var(--bc-text-muted);
}

.color-presets {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin: -8px 0 24px 140px;
}

.preset-label {
  font-size: 13px;
  color: var(--bc-text-muted);
  margin-right: 4px;
}

.preset-swatch {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid transparent;
  transition: all 0.2s;
  color: white;
}

.preset-swatch:hover {
  transform: scale(1.15);
}

.preset-swatch.active {
  border-color: var(--bc-text);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.logo-upload-area {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.logo-uploader {
  display: block;
}

.logo-uploader :deep(.el-upload) {
  border: 2px dashed #d9d9d9;
  border-radius: 10px;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s;
}

.logo-uploader :deep(.el-upload:hover) {
  border-color: var(--bc-primary);
}

.logo-preview {
  width: 160px;
  height: 48px;
  object-fit: contain;
  display: block;
}

.logo-placeholder {
  width: 160px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #8c939d;
  font-size: 13px;
}

/* ===== Notify Preview ===== */
.notify-preview {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  max-width: 640px;
}

.preview-card {
  border-radius: 12px;
  padding: 16px;
  font-size: 13px;
  line-height: 1.8;
}

.preview-card.success {
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  border: 1px solid #86efac;
}

.preview-card.fail {
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
  border: 1px solid #fca5a5;
}

.preview-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 8px;
  color: #1f2937;
}

.preview-content {
  color: #4b5563;
}

.preview-content div {
  padding-left: 12px;
  border-left: 3px solid #d1d5db;
  margin-bottom: 4px;
}
</style>
