<template>
  <div class="page-header">
    <div class="page-header-info">
      <h2 class="page-header-title">{{ title }}</h2>
      <p v-if="description" class="page-header-desc">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="page-header-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  }
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-header-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.page-header-title {
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--bc-text);
  letter-spacing: -0.01em;
  margin: 0;
  line-height: 1.4;
}

.page-header-desc {
  font-size: 0.8125rem;
  color: var(--bc-text-muted);
  margin: 0;
  line-height: 1.5;
}

.page-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .page-header-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
