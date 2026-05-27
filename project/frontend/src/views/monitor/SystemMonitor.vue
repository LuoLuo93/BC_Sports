<template>
  <div class="system-monitor">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 健康状态 -->
      <el-tab-pane label="健康状态" name="health">
        <div class="tab-header">
          <el-button :icon="Refresh" circle @click="loadHealth" :loading="healthLoading" />
          <span class="refresh-hint">每 60 秒自动刷新</span>
        </div>

        <el-row :gutter="16" v-loading="healthLoading">
          <!-- 应用状态 -->
          <el-col :span="8">
            <el-card shadow="hover" class="status-card">
              <template #header><span class="card-title">应用状态</span></template>
              <div class="status-center">
                <el-tag :type="healthStatus === 'UP' ? 'success' : 'danger'" size="large" effect="dark" round>
                  {{ healthStatus || '加载中...' }}
                </el-tag>
                <div class="status-detail" v-if="healthDetails">
                  <p v-for="(val, key) in healthDetails" :key="key" class="detail-row">
                    <span class="detail-label">{{ componentLabel(key) }}</span>
                    <el-tag :type="val.status === 'UP' ? 'success' : 'danger'" size="small">
                      {{ val.status }}
                    </el-tag>
                  </p>
                </div>
              </div>
            </el-card>
          </el-col>

          <!-- 磁盘空间 -->
          <el-col :span="8">
            <el-card shadow="hover" class="status-card">
              <template #header><span class="card-title">磁盘空间</span></template>
              <div class="disk-info" v-if="diskSpace">
                <el-progress type="dashboard" :percentage="diskPercent" :color="diskColor" :width="140">
                  <template #default>
                    <div class="disk-percent-text">{{ diskPercent }}%</div>
                    <div class="disk-percent-label">已使用</div>
                  </template>
                </el-progress>
                <div class="disk-detail">
                  <p>总量：{{ formatBytes(diskSpace.total) }}</p>
                  <p>已用：{{ formatBytes(diskSpace.total - diskSpace.free) }}</p>
                  <p>可用：{{ formatBytes(diskSpace.free) }}</p>
                  <p>阈值：{{ formatBytes(diskSpace.threshold) }}</p>
                </div>
              </div>
            </el-card>
          </el-col>

          <!-- 数据源 -->
          <el-col :span="8">
            <el-card shadow="hover" class="status-card">
              <template #header><span class="card-title">数据源</span></template>
              <div class="ds-info" v-if="dataSource">
                <p class="detail-row">
                  <span class="detail-label">状态</span>
                  <el-tag :type="dataSource.status === 'UP' ? 'success' : 'danger'" size="small">
                    {{ dataSource.status }}
                  </el-tag>
                </p>
                <template v-if="dataSource.details">
                  <p v-for="(val, key) in dataSource.details" :key="key" class="detail-row">
                    <span class="detail-label">{{ dsLabel(key) }}</span>
                    <span class="detail-value">{{ dsValue(key, val) }}</span>
                  </p>
                </template>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- JVM 监控 -->
      <el-tab-pane label="JVM 监控" name="jvm">
        <div class="tab-header">
          <el-button :icon="Refresh" circle @click="loadJvmMetrics" :loading="jvmLoading" />
          <span class="refresh-hint">每 30 秒自动刷新</span>
        </div>

        <el-row :gutter="16" v-loading="jvmLoading">
          <el-col :span="6" v-for="m in metricCards" :key="m.key">
            <el-card shadow="hover" class="metric-card">
              <div class="metric-icon" :style="{ background: m.color }">
                <el-icon :size="24"><component :is="m.icon" /></el-icon>
              </div>
              <div class="metric-body">
                <div class="metric-value">{{ m.value }}</div>
                <div class="metric-label">{{ m.label }}</div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="16" style="margin-top: 16px">
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header><span class="card-title">内存使用</span></template>
              <div class="mem-bars" v-if="jvmMetrics.memory">
                <div class="mem-item">
                  <div class="mem-head">
                    <span>堆内存</span>
                    <span>{{ formatBytes(jvmMetrics.memory.heapUsed) }} / {{ formatBytes(jvmMetrics.memory.heapMax) }}</span>
                  </div>
                  <el-progress :percentage="jvmMetrics.memory.heapPercent" :color="memColor(jvmMetrics.memory.heapPercent)" :stroke-width="18" />
                </div>
                <div class="mem-item">
                  <div class="mem-head">
                    <span>非堆内存</span>
                    <span>{{ formatBytes(jvmMetrics.memory.nonHeapUsed) }} / {{ formatBytes(jvmMetrics.memory.nonHeapMax || 0) }}</span>
                  </div>
                  <el-progress :percentage="jvmMetrics.memory.nonHeapPercent" :color="memColor(jvmMetrics.memory.nonHeapPercent)" :stroke-width="18" />
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header><span class="card-title">线程概况</span></template>
              <div class="thread-info" v-if="jvmMetrics.threads">
                <div class="thread-row" v-for="(val, key) in jvmMetrics.threads" :key="key">
                  <span class="detail-label">{{ threadLabel(key) }}</span>
                  <span class="detail-value">{{ val }}</span>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 日志管理 -->
      <el-tab-pane label="日志管理" name="loggers">
        <div class="tab-header">
          <el-input v-model="loggerSearch" placeholder="搜索 Logger 名称" clearable style="width: 300px" :prefix-icon="Search" />
        </div>

        <el-table :data="filteredLoggers" stripe border max-height="600" v-loading="loggersLoading">
          <el-table-column prop="name" label="Logger 名称" min-width="300" show-overflow-tooltip />
          <el-table-column label="有效级别" width="130" align="center">
            <template #default="{ row }">
              <el-tag :type="levelType(row.effectiveLevel)" size="small">{{ row.effectiveLevel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="配置级别" width="160" align="center">
            <template #default="{ row }">
              <el-select v-model="row.configuredLevel" placeholder="继承" size="small" clearable style="width: 110px" @change="val => changeLogLevel(row.name, val)">
                <el-option v-for="lv in logLevels" :key="lv" :label="lv" :value="lv" />
              </el-select>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { Refresh, Search, Monitor, Cpu, Timer, DataLine } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getHealth, getMetricDetail, getLoggers, setLoggerLevel } from '@/api/monitor'

const activeTab = ref('health')
const healthLoading = ref(false)
const jvmLoading = ref(false)
const loggersLoading = ref(false)

let healthTimer = null
let jvmTimer = null

function startHealthTimer() {
  stopHealthTimer()
  healthTimer = setInterval(loadHealth, 60000)
}
function stopHealthTimer() {
  if (healthTimer) { clearInterval(healthTimer); healthTimer = null }
}
function startJvmTimer() {
  stopJvmTimer()
  jvmTimer = setInterval(loadJvmMetrics, 30000)
}
function stopJvmTimer() {
  if (jvmTimer) { clearInterval(jvmTimer); jvmTimer = null }
}

watch(activeTab, (tab) => {
  stopHealthTimer()
  stopJvmTimer()
  if (tab === 'health') startHealthTimer()
  else if (tab === 'jvm') startJvmTimer()
})

// --- Health ---
const healthStatus = ref('')
const healthDetails = ref(null)
const diskSpace = ref(null)
const dataSource = ref(null)

const diskPercent = computed(() => {
  if (!diskSpace.value) return 0
  const used = diskSpace.value.total - diskSpace.value.free
  return Math.round((used / diskSpace.value.total) * 100)
})
const diskColor = computed(() => {
  const p = diskPercent.value
  if (p < 70) return '#67c23a'
  if (p < 90) return '#e6a23c'
  return '#f56c6c'
})

async function loadHealth() {
  healthLoading.value = true
  try {
    const data = await getHealth()
    healthStatus.value = data.status || 'UNKNOWN'
    const comps = data.components || {}
    healthDetails.value = {}
    for (const [k, v] of Object.entries(comps)) {
      if (k !== 'diskSpace' && k !== 'dataSource' && k !== 'db') {
        healthDetails.value[k] = v
      }
    }
    if (comps.diskSpace) diskSpace.value = comps.diskSpace.details || comps.diskSpace
    if (comps.dataSource) dataSource.value = comps.dataSource
    else if (comps.db) dataSource.value = comps.db
  } catch (e) {
    console.error('Health check failed', e)
  } finally {
    healthLoading.value = false
  }
}

// --- JVM ---
const jvmMetrics = ref({ memory: null, threads: null })

const metricCards = computed(() => {
  const m = jvmMetrics.value
  const mem = m.memory || {}
  return [
    { key: 'heap', label: '堆内存使用率', value: mem.heapPercent != null ? mem.heapPercent + '%' : '-', icon: DataLine, color: '#409eff' },
    { key: 'cpu', label: 'CPU 使用率', value: m.cpu != null ? m.cpu.toFixed(1) + '%' : '-', icon: Cpu, color: '#e6a23c' },
    { key: 'threads', label: '活跃线程', value: m.threads?.live ?? '-', icon: Monitor, color: '#67c23a' },
    { key: 'uptime', label: '运行时间', value: m.uptime ?? '-', icon: Timer, color: '#909399' }
  ]
})

async function loadJvmMetrics() {
  jvmLoading.value = true
  try {
    const [heapUsed, heapMax, nonHeapUsed, nonHeapCommitted, cpuData, threadData, uptimeData] = await Promise.all([
      getMetricDetail('jvm.memory.used?tag=area:heap').catch(() => null),
      getMetricDetail('jvm.memory.max?tag=area:heap').catch(() => null),
      getMetricDetail('jvm.memory.used?tag=area:nonheap').catch(() => null),
      getMetricDetail('jvm.memory.committed?tag=area:nonheap').catch(() => null),
      getMetricDetail('process.cpu.usage').catch(() => null),
      getMetricDetail('jvm.threads.live').catch(() => null),
      getMetricDetail('process.uptime').catch(() => null)
    ])

    const heapUsedVal = heapUsed?.measurements?.[0]?.value || 0
    const heapMaxVal = heapMax?.measurements?.[0]?.value || 0
    const nonHeapUsedVal = nonHeapUsed?.measurements?.[0]?.value || 0
    const nonHeapMaxVal = nonHeapCommitted?.measurements?.[0]?.value || 0
    const cpuVal = cpuData?.measurements?.[0]?.value
    const liveThreads = threadData?.measurements?.[0]?.value
    const uptimeSeconds = uptimeData?.measurements?.[0]?.value

    jvmMetrics.value = {
      memory: {
        heapUsed: heapUsedVal,
        heapMax: heapMaxVal,
        heapPercent: heapMaxVal > 0 ? Math.round((heapUsedVal / heapMaxVal) * 100) : 0,
        nonHeapUsed: nonHeapUsedVal,
        nonHeapMax: nonHeapMaxVal,
        nonHeapPercent: nonHeapMaxVal > 0 ? Math.round((nonHeapUsedVal / nonHeapMaxVal) * 100) : 0
      },
      cpu: cpuVal != null ? cpuVal * 100 : null,
      threads: { live: liveThreads ? Math.round(liveThreads) : null },
      uptime: uptimeSeconds ? formatDuration(uptimeSeconds / 1000) : null
    }
  } catch (e) {
    console.error('JVM metrics failed', e)
  } finally {
    jvmLoading.value = false
  }
}

// --- Loggers ---
const loggerSearch = ref('')
const loggersList = ref([])
const logLevels = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'FATAL', 'OFF']

const filteredLoggers = computed(() => {
  if (!loggerSearch.value) return loggersList.value
  const kw = loggerSearch.value.toLowerCase()
  return loggersList.value.filter(l => l.name.toLowerCase().includes(kw))
})

async function loadLoggers() {
  loggersLoading.value = true
  try {
    const data = await getLoggers()
    const loggers = data.loggers || {}
    loggersList.value = Object.entries(loggers)
      .map(([name, info]) => ({
        name,
        effectiveLevel: info.effectiveLevel || 'UNSET',
        configuredLevel: info.configuredLevel || ''
      }))
      .filter(l => l.name !== 'ROOT' && l.effectiveLevel)
      .sort((a, b) => a.name.localeCompare(b.name))
    if (loggers.ROOT) {
      loggersList.value.unshift({
        name: 'ROOT',
        effectiveLevel: loggers.ROOT.effectiveLevel || 'INFO',
        configuredLevel: loggers.ROOT.configuredLevel || ''
      })
    }
  } catch (e) {
    console.error('Loggers failed', e)
  } finally {
    loggersLoading.value = false
  }
}

async function changeLogLevel(name, level) {
  try {
    await setLoggerLevel(name, level || null)
    ElMessage.success(`${name} 日志级别已设为 ${level || '继承'}`)
    setTimeout(loadLoggers, 500)
  } catch {
    ElMessage.error('修改日志级别失败')
  }
}

// --- Helpers ---
function formatBytes(bytes) {
  if (!bytes || bytes < 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let val = bytes
  while (val >= 1024 && i < units.length - 1) { val /= 1024; i++ }
  return val.toFixed(i === 0 ? 0 : 1) + ' ' + units[i]
}

function formatDuration(seconds) {
  if (!seconds) return '-'
  const d = Math.floor(seconds / 86400)
  const h = Math.floor((seconds % 86400) / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  if (d > 0) return `${d}天 ${h}小时`
  if (h > 0) return `${h}小时 ${m}分钟`
  return `${m}分钟`
}

function componentLabel(key) {
  const map = { ping: 'Ping', refreshScope: 'Refresh', serviceDiscovery: '服务发现' }
  return map[key] || key
}

function dsLabel(key) {
  const map = { database: '数据库', validationQuery: '验证语句', result: '验证结果', product: '产品', driver: '驱动' }
  return map[key] || key
}

function dsValue(key, val) {
  if (typeof val === 'object') return JSON.stringify(val)
  return val
}

function threadLabel(key) {
  const map = { live: '活跃线程', peak: '峰值线程', daemon: '守护线程' }
  return map[key] || key
}

function memColor(pct) {
  if (pct < 70) return '#67c23a'
  if (pct < 90) return '#e6a23c'
  return '#f56c6c'
}

function levelType(level) {
  const map = { TRACE: 'info', DEBUG: '', INFO: 'success', WARN: 'warning', ERROR: 'danger', FATAL: 'danger', OFF: 'info' }
  return map[level] || 'info'
}

// --- Lifecycle ---
onMounted(() => {
  loadHealth()
  loadJvmMetrics()
  loadLoggers()
  startHealthTimer()
})

onBeforeUnmount(() => {
  stopHealthTimer()
  stopJvmTimer()
})
</script>

<style scoped>
.system-monitor {
  padding: 16px;
}

.tab-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.refresh-hint {
  font-size: 12px;
  color: #909399;
}

.card-title {
  font-weight: 600;
  font-size: 14px;
}

.status-card {
  min-height: 260px;
}

.status-center {
  text-align: center;
}

.status-detail {
  margin-top: 20px;
  text-align: left;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-label {
  color: #606266;
  font-size: 13px;
  min-width: 80px;
}

.detail-value {
  color: #303133;
  font-size: 13px;
  font-weight: 500;
}

.disk-info {
  text-align: center;
}

.disk-percent-text {
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
}

.disk-percent-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.disk-detail {
  margin-top: 16px;
  text-align: left;
}

.disk-detail p {
  margin: 4px 0;
  font-size: 13px;
  color: #606266;
}

.ds-info p {
  margin: 8px 0;
}

.metric-card {
  display: flex;
  align-items: center;
  padding: 0;
}

.metric-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 20px;
}

.metric-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.metric-body {
  flex: 1;
  min-width: 0;
}

.metric-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.metric-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.mem-bars {
  padding: 8px 0;
}

.mem-item {
  margin-bottom: 20px;
}

.mem-item:last-child {
  margin-bottom: 0;
}

.mem-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
  color: #606266;
}

.thread-info {
  padding: 8px 0;
}

.thread-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}
</style>
