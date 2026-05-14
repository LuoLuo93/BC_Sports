<template>
  <div class="dashboard">
    <el-row :gutter="16" class="mb-4">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-body">
            <div class="stat-icon bg-primary-soft">
              <el-icon :size="24" color="#1d4ed8"><User /></el-icon>
            </div>
            <div>
              <div class="stat-label">总用户数</div>
              <div class="stat-value">1,234</div>
              <div class="stat-change text-success">+12% 较上月</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-body">
            <div class="stat-icon bg-success-soft">
              <el-icon :size="24" color="#10b981"><Connection /></el-icon>
            </div>
            <div>
              <div class="stat-label">活跃会话</div>
              <div class="stat-value">89</div>
              <div class="stat-change text-success">实时在线</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-body">
            <div class="stat-icon bg-warning-soft">
              <el-icon :size="24" color="#f59e0b"><ShoppingCart /></el-icon>
            </div>
            <div>
              <div class="stat-label">今日订单</div>
              <div class="stat-value">456</div>
              <div class="stat-change text-danger">-3% 较昨日</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-body">
            <div class="stat-icon bg-info-soft">
              <el-icon :size="24" color="#3b82f6"><Money /></el-icon>
            </div>
            <div>
              <div class="stat-label">结算营收</div>
              <div class="stat-value">&yen;12,345</div>
              <div class="stat-change text-muted">当日累计</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="16">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><TrendCharts /></el-icon> 业务趋势分析</span>
            </div>
          </template>
          <div class="chart-placeholder">
            <el-icon :size="48" color="#1d4ed8"><TrendCharts /></el-icon>
            <p>可视化引擎加载中...</p>
            <small>正在同步实时交易数据</small>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="activity-card">
          <template #header>
            <span><el-icon><Lightning /></el-icon> 实时动态</span>
          </template>
          <div class="activity-list">
            <div class="activity-item">
              <div class="activity-dot bg-success"></div>
              <div class="activity-content">
                <div class="activity-title">用户注册 <small class="activity-time">2分钟前</small></div>
                <p>新用户 <strong>张三</strong> 成功加入系统</p>
              </div>
            </div>
            <div class="activity-item">
              <div class="activity-dot bg-primary"></div>
              <div class="activity-content">
                <div class="activity-title">订单完成 <small class="activity-time">15分钟前</small></div>
                <p>订单 <strong>#12345</strong> 已完成银行结算</p>
              </div>
            </div>
            <div class="activity-item">
              <div class="activity-dot bg-warning"></div>
              <div class="activity-content">
                <div class="activity-title">系统通知 <small class="activity-time">1小时前</small></div>
                <p>数据中心预计在 23:00 进行例行维护</p>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { User, Connection, ShoppingCart, Money, TrendCharts, Lightning } from '@element-plus/icons-vue'
</script>

<style scoped>
.dashboard { padding: 0; }

.stat-card { border-radius: 12px; border: none; }
.stat-body { display: flex; align-items: center; gap: 16px; }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.bg-primary-soft { background: #eff6ff; }
.bg-success-soft { background: #f0fdf4; }
.bg-warning-soft { background: #fffbeb; }
.bg-info-soft { background: #eff6ff; }

.stat-label { font-size: 0.75rem; font-weight: 600; color: #78716c; text-transform: uppercase; letter-spacing: 0.05em; }
.stat-value { font-size: 1.75rem; font-weight: 800; color: #1c1917; margin: 2px 0; }
.stat-change { font-size: 0.75rem; }
.text-success { color: #10b981; }
.text-danger { color: #ef4444; }
.text-muted { color: #78716c; }

.chart-card, .activity-card { border-radius: 12px; border: none; }
.card-header { display: flex; align-items: center; gap: 8px; font-weight: 700; }

.chart-placeholder {
  height: 350px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(248, 249, 250, 0.5);
  border-radius: 12px;
  border: 1px dashed #dee2e6;
  color: #78716c;
}

.chart-placeholder p { font-weight: 500; margin-top: 12px; }
.chart-placeholder small { font-size: 0.8rem; }

.activity-list { padding: 0; }
.activity-item { display: flex; gap: 12px; padding: 12px 0; border-bottom: 1px solid #f5f5f4; }
.activity-item:last-child { border-bottom: none; }
.activity-dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 6px; flex-shrink: 0; }
.bg-success { background: #10b981; }
.bg-primary { background: #3b82f6; }
.bg-warning { background: #f59e0b; }

.activity-title { font-weight: 700; font-size: 0.8rem; }
.activity-time { color: #a8a29e; font-weight: 400; margin-left: auto; }
.activity-content p { font-size: 0.8rem; color: #78716c; margin: 4px 0 0; }
</style>
