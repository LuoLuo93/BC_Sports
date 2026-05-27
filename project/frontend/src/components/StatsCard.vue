<template>
  <el-card shadow="never" class="stats-card" :class="[`stats-card--${variant}`]">
    <div class="stats-body">
      <slot name="icon">
        <div class="stats-icon-box" :style="{ background: iconBg }">
          <el-icon :size="iconSize" :color="iconColor">
            <component v-if="icon" :is="icon" />
          </el-icon>
        </div>
      </slot>
      <div class="stats-info">
        <div class="stats-label">{{ label }}</div>
        <div class="stats-value">
          <slot>{{ value }}</slot>
        </div>
        <div v-if="trend || subtitle" class="stats-footer">
          <span v-if="trend" class="stats-trend" :class="`stats-trend--${trendType}`">
            <el-icon :size="12">
              <ArrowUp v-if="trendType === 'up'" />
              <ArrowDown v-else-if="trendType === 'down'" />
              <Minus v-else />
            </el-icon>
            <span>{{ trend }}</span>
          </span>
          <span v-if="subtitle" class="stats-subtitle">{{ subtitle }}</span>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { ArrowUp, ArrowDown, Minus } from '@element-plus/icons-vue'

defineProps({
  label: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    default: '--'
  },
  icon: {
    type: [String, Object],
    default: null
  },
  iconSize: {
    type: Number,
    default: 22
  },
  iconBg: {
    type: String,
    default: 'rgba(29, 78, 216, 0.08)'
  },
  iconColor: {
    type: String,
    default: 'var(--bc-primary)'
  },
  trend: {
    type: String,
    default: ''
  },
  trendType: {
    type: String,
    default: 'neutral',
    validator: (v) => ['up', 'down', 'neutral'].includes(v)
  },
  subtitle: {
    type: String,
    default: ''
  },
  variant: {
    type: String,
    default: 'default',
    validator: (v) => ['default', 'success', 'warning', 'danger'].includes(v)
  }
})
</script>

<style scoped>
.stats-card {
  position: relative;
  overflow: hidden;
  border: none;
  transition: transform var(--bc-transition-base), box-shadow var(--bc-transition-base);
}

.stats-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  border-radius: 12px 0 0 12px;
}

.stats-card--default::before {
  background: linear-gradient(180deg, var(--bc-primary), var(--bc-primary-light));
}

.stats-card--success::before {
  background: linear-gradient(180deg, var(--bc-success), #34d399);
}

.stats-card--warning::before {
  background: linear-gradient(180deg, var(--bc-warning), #fbbf24);
}

.stats-card--danger::before {
  background: linear-gradient(180deg, var(--bc-danger), #f87171);
}

.stats-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--bc-shadow-lg);
}

.stats-body {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stats-icon-box {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.stats-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stats-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--bc-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  line-height: 1.4;
}

.stats-value {
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--bc-text);
  line-height: 1.3;
  font-variant-numeric: tabular-nums;
  margin: 2px 0;
}

.stats-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.75rem;
}

.stats-trend {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-weight: 500;
}

.stats-trend--up {
  color: var(--bc-success);
}

.stats-trend--down {
  color: var(--bc-danger);
}

.stats-trend--neutral {
  color: var(--bc-text-lighter);
}

.stats-subtitle {
  color: var(--bc-text-lighter);
}
</style>
