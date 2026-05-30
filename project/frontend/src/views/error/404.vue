<template>
  <div class="not-found-page">
    <div class="not-found-bg">
      <div class="bg-circle circle-1"></div>
      <div class="bg-circle circle-2"></div>
      <div class="bg-circle circle-3"></div>
    </div>
    <div class="not-found-content">
      <div class="error-code">404</div>
      <div class="error-title">页面不存在</div>
      <div class="error-desc">抱歉，您访问的页面不存在或已被移除</div>
      <el-button type="primary" size="large" @click="router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<style scoped>
.not-found-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: #f7f8fa;
}

.not-found-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
}

.circle-1 {
  width: 400px;
  height: 400px;
  background: rgba(239, 68, 68, 0.08);
  top: -100px;
  right: -80px;
}

.circle-2 {
  width: 300px;
  height: 300px;
  background: rgba(245, 158, 11, 0.06);
  bottom: -60px;
  left: -60px;
}

.circle-3 {
  width: 200px;
  height: 200px;
  background: rgba(124, 58, 237, 0.05);
  top: 50%;
  left: 30%;
}

.not-found-content {
  text-align: center;
  z-index: 1;
}

.error-code {
  font-family: 'Outfit', sans-serif;
  font-weight: 900;
  font-size: 8rem;
  line-height: 1;
  background: linear-gradient(135deg, #ef4444, #f59e0b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
}

.error-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--bc-text);
  margin-bottom: 8px;
}

.error-desc {
  font-size: 0.9375rem;
  color: var(--bc-text-muted);
  margin-bottom: 32px;
}
</style>
