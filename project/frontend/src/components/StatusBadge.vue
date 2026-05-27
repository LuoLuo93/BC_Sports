<template>
  <span class="status-badge" :class="`status-badge--${type}`">
    <span class="status-badge-dot" />
    <span class="status-badge-text">
      <slot>{{ text }}</slot>
    </span>
  </span>
</template>

<script setup>
defineProps({
  text: {
    type: String,
    default: ''
  },
  type: {
    type: String,
    default: 'default',
    validator: (v) => ['default', 'success', 'warning', 'danger', 'info'].includes(v)
  }
})
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 0.8125rem;
  line-height: 1.6;
  white-space: nowrap;
}

.status-badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-badge-text {
  font-weight: 500;
}

.status-badge--default { background: var(--bc-bg-info-soft); color: var(--bc-text-muted); }
.status-badge--default .status-badge-dot { background: var(--bc-text-lighter); }

.status-badge--success { background: var(--bc-bg-success-soft); color: var(--bc-success); }
.status-badge--success .status-badge-dot { background: var(--bc-success); animation: pulse-dot 2s infinite; }

.status-badge--warning { background: var(--bc-bg-warning-soft); color: var(--bc-warning); }
.status-badge--warning .status-badge-dot { background: var(--bc-warning); }

.status-badge--danger { background: #fef2f2; color: var(--bc-danger); }
.status-badge--danger .status-badge-dot { background: var(--bc-danger); }

.status-badge--info { background: var(--bc-bg-primary-soft); color: var(--bc-primary); }
.status-badge--info .status-badge-dot { background: var(--bc-primary); }

@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.4); }
}
</style>
