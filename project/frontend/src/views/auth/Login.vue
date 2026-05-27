<template>
  <div class="login-layout">
    <!-- Left: Brand -->
    <div class="brand-side">
      <img
        src="https://images.unsplash.com/photo-1551698618-1dfe5d97d256?auto=format&fit=crop&q=80&w=3540"
        class="brand-image"
        alt="Skiing Summit"
      />
      <div class="brand-overlay"></div>

      <!-- Noise texture -->
      <div class="noise-layer"></div>

      <!-- Aurora effect -->
      <div class="aurora">
        <div class="aurora-beam b1"></div>
        <div class="aurora-beam b2"></div>
        <div class="aurora-beam b3"></div>
        <div class="aurora-beam b4"></div>
      </div>

      <!-- Floating shapes -->
      <div class="floating-shapes">
        <span v-for="i in 12" :key="i" class="shape" :class="'shape-' + (i % 4)" :style="shapeStyle(i)"></span>
      </div>

      <!-- Mountain silhouette with glow -->
      <div class="mountain-glow"></div>
      <svg class="mountain-silhouette" viewBox="0 0 1440 220" preserveAspectRatio="none">
        <path d="M0,220 L0,150 L100,90 L200,130 L340,55 L460,100 L560,25 L680,85 L800,35 L940,100 L1060,30 L1180,75 L1300,50 L1440,90 L1440,220 Z" fill="rgba(0,0,0,0.2)"/>
        <path d="M0,220 L0,165 L160,115 L340,155 L520,95 L700,140 L880,80 L1060,125 L1240,90 L1440,135 L1440,220 Z" fill="rgba(0,0,0,0.1)"/>
      </svg>

      <div class="brand-text">
        <div class="brand-badge">EST. 2024</div>
        <h2>Touch the<br/><span class="title-accent">Highest Peak</span></h2>
        <p>B.C.SPORTS 巅峰数字化中心<br/>为每一场远征保驾护航。</p>

        <!-- Stats -->
        <div class="brand-stats">
          <div class="stat-item">
            <div class="stat-num" data-suffix="+">500</div>
            <div class="stat-label">户外产品</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-num" data-suffix="+">50</div>
            <div class="stat-label">品牌合作</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-num">365</div>
            <div class="stat-label">天在线服务</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Right: Form -->
    <div class="form-side">
      <!-- Animated gradient blobs -->
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>

      <!-- Dot grid pattern -->
      <div class="dot-grid"></div>

      <div class="form-wrapper">
        <!-- Logo -->
        <div class="login-header">
          <div class="logo-badge">
            <div class="logo-icon">
              <el-icon><Location /></el-icon>
            </div>
            <span>B.C.SPORTS</span>
          </div>
          <h2>让人人尽享户外运动的快乐</h2>
          <p class="login-subtitle">
            <span class="subtitle-dot"></span>
            请通过您的专业凭据访问系统中心
          </p>
        </div>

        <!-- Form -->
        <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
          <div class="bc-form-group">
            <label class="bc-label" for="username">
              <el-icon><User /></el-icon>
              IDENTITY / 通行证
            </label>
            <el-input
              id="username"
              v-model="form.username"
              placeholder="请输入员工账号/ID"
              size="large"
            />
          </div>

          <div class="bc-form-group">
            <label class="bc-label" for="password">
              <el-icon><Key /></el-icon>
              SECURITY / 校验码
            </label>
            <el-input
              id="password"
              v-model="form.password"
              type="password"
              placeholder="请输入访问密钥"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </div>

          <div class="options-bar">
            <el-checkbox v-model="form.rememberMe">信任此设备</el-checkbox>
            <a href="#" class="form-link">授权遇到困难?</a>
          </div>

          <button
            type="submit"
            class="btn-adventure"
            :class="{ 'is-loading': loading }"
            :disabled="loading"
            @click.prevent="handleLogin"
          >
            <span class="btn-bg"></span>
            <span class="btn-shimmer"></span>
            <span class="btn-glow"></span>
            <span class="btn-content">
              <template v-if="!loading">
                <span>进入数字化中心</span>
                <svg class="btn-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
              </template>
              <template v-else>
                <svg class="spinner" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-dasharray="31.4 31.4" />
                </svg>
                <span>正在验证专业凭据...</span>
              </template>
            </span>
          </button>
        </el-form>

        <transition name="alert-fade">
          <div v-if="errorMsg" class="error-alert">
            <el-icon><CircleCheck /></el-icon>
            <span>{{ errorMsg }}</span>
          </div>
        </transition>
      </div>

      <div class="brand-footer">
        <div class="badge-item">
          <el-icon :size="20"><Orange /></el-icon>
          <span>Hiking / 徒步</span>
        </div>
        <div class="badge-divider"></div>
        <div class="badge-item">
          <el-icon :size="20"><Coffee /></el-icon>
          <span>Camping / 露营</span>
        </div>
        <div class="badge-divider"></div>
        <div class="badge-item">
          <el-icon :size="20"><IceCreamRound /></el-icon>
          <span>Skiing / 滑雪</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTabStore } from '@/stores/tab'
import { User, Key, ArrowRight, Location, CircleCheck, Orange, Coffee, IceCreamRound } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const tabStore = useTabStore()

const formRef = ref(null)
const loading = ref(false)
const errorMsg = ref('')

const form = ref({
  username: '',
  password: '',
  rememberMe: false
})

const rules = {
  username: [{ required: true, message: '请输入员工账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入访问密钥', trigger: 'blur' }]
}

onMounted(() => {
  if (route.query.kicked === '1') {
    errorMsg.value = '您的账号已在其他设备登录，请重新登录'
  }
})

function shapeStyle(i) {
  const size = 5 + Math.random() * 12
  return {
    width: size + 'px',
    height: size + 'px',
    left: (3 + Math.random() * 94) + '%',
    top: (3 + Math.random() * 70) + '%',
    animationDelay: (i * 0.6) + 's',
    animationDuration: (7 + Math.random() * 8) + 's'
  }
}

async function handleLogin() {
  errorMsg.value = ''
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form.value)
    tabStore.clearAll()
    tabStore.initDashboard()
    router.push('/')
  } catch (e) {
    errorMsg.value = e.message || '凭据检验未通过，请核对后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: #ffffff;
  font-family: 'Inter', 'Outfit', sans-serif;
}

/* ============================================================
   BRAND SIDE
   ============================================================ */
.brand-side {
  flex: 0 0 60%;
  position: relative;
  background-color: #0c1a14;
  overflow: hidden;
}

.brand-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: contrast(1.1) brightness(0.65) saturate(1.2);
  animation: ken-burns 35s ease-in-out infinite alternate;
  transform-origin: 30% 35%;
}

@keyframes ken-burns {
  0% { transform: scale(1) translate(0, 0); }
  100% { transform: scale(1.12) translate(-1.5%, -1%); }
}

.brand-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(0,0,0,0.02) 0%, rgba(6,78,59,0.25) 40%, rgba(0,0,0,0.55) 100%),
    linear-gradient(135deg, rgba(6,78,59,0.45) 0%, rgba(29,78,216,0.18) 50%, rgba(99,102,241,0.12) 100%);
  z-index: 1;
}

/* --- Noise texture --- */
.noise-layer {
  position: absolute;
  inset: 0;
  z-index: 2;
  opacity: 0.03;
  pointer-events: none;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 200px;
}

/* --- Aurora --- */
.aurora {
  position: absolute;
  inset: 0;
  z-index: 3;
  pointer-events: none;
  overflow: hidden;
}

.aurora-beam {
  position: absolute;
  width: 250%;
  height: 55%;
  border-radius: 50%;
  filter: blur(100px);
  mix-blend-mode: screen;
}

.b1 {
  top: -25%;
  left: -60%;
  background: linear-gradient(90deg, transparent, rgba(16,185,129,0.3), rgba(59,130,246,0.2), transparent);
  animation: aurora-drift 14s ease-in-out infinite alternate;
}

.b2 {
  top: -15%;
  left: -40%;
  background: linear-gradient(90deg, transparent, rgba(99,102,241,0.25), rgba(16,185,129,0.15), transparent);
  animation: aurora-drift 18s ease-in-out infinite alternate-reverse;
}

.b3 {
  top: 5%;
  left: -50%;
  background: linear-gradient(90deg, transparent, rgba(14,165,233,0.2), rgba(139,92,246,0.15), transparent);
  animation: aurora-drift 22s ease-in-out infinite alternate;
}

.b4 {
  top: -5%;
  left: -30%;
  background: linear-gradient(90deg, transparent, rgba(56,189,248,0.15), rgba(99,102,241,0.1), transparent);
  animation: aurora-drift 16s ease-in-out infinite alternate-reverse;
  animation-delay: -5s;
}

@keyframes aurora-drift {
  0% { transform: translateX(-8%) rotate(-2deg) scaleY(0.9); }
  50% { transform: translateX(5%) rotate(1deg) scaleY(1.1); }
  100% { transform: translateX(10%) rotate(3deg) scaleY(0.95); }
}

/* --- Floating Shapes --- */
.floating-shapes {
  position: absolute;
  inset: 0;
  z-index: 4;
  pointer-events: none;
  overflow: hidden;
}

.shape {
  position: absolute;
  opacity: 0;
  animation: shape-float linear infinite;
}

.shape-0 {
  border-radius: 50%;
  background: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.12);
  backdrop-filter: blur(1px);
}

.shape-1 {
  border-radius: 2px;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
}

.shape-2 {
  border-radius: 0;
  background: transparent;
  border: 1px solid rgba(255,255,255,0.08);
}

.shape-3 {
  border-radius: 50%;
  background: transparent;
  border: 1px solid rgba(255,255,255,0.06);
}

@keyframes shape-float {
  0%   { opacity: 0; transform: translateY(0) rotate(0deg) scale(0.5); }
  10%  { opacity: 0.7; transform: scale(1); }
  90%  { opacity: 0.3; }
  100% { opacity: 0; transform: translateY(-160px) rotate(200deg) scale(0.5); }
}

/* --- Mountain glow --- */
.mountain-glow {
  position: absolute;
  bottom: 60px;
  left: 0;
  width: 100%;
  height: 200px;
  background: linear-gradient(0deg, rgba(16,185,129,0.08) 0%, transparent 100%);
  z-index: 4;
  pointer-events: none;
  filter: blur(40px);
}

/* --- Mountain Silhouette --- */
.mountain-silhouette {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 220px;
  z-index: 5;
  pointer-events: none;
}

/* --- Brand Text --- */
.brand-text {
  position: absolute;
  bottom: 60px;
  left: 55px;
  z-index: 6;
  color: white;
  max-width: 540px;
  animation: fade-in-up 1.2s cubic-bezier(0.16, 1, 0.3, 1) 0.3s both;
}

.brand-badge {
  display: inline-block;
  padding: 6px 18px;
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 20px;
  font-family: 'Outfit', sans-serif;
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.22em;
  color: rgba(255,255,255,0.8);
  margin-bottom: 26px;
  backdrop-filter: blur(12px);
  background: rgba(255,255,255,0.06);
  box-shadow: 0 0 20px rgba(255,255,255,0.03);
}

.brand-text h2 {
  font-family: 'Outfit', sans-serif;
  font-weight: 900;
  font-size: 3.75rem;
  line-height: 1;
  margin-bottom: 20px;
  text-transform: uppercase;
  letter-spacing: -0.03em;
  text-shadow: 0 4px 40px rgba(0,0,0,0.4);
}

.title-accent {
  background: linear-gradient(135deg, #6ee7b7, #3b82f6, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 0 30px rgba(59,130,246,0.3));
}

.brand-text p {
  font-size: 1.0625rem;
  font-weight: 400;
  opacity: 0.8;
  margin-bottom: 0;
  line-height: 1.8;
  max-width: 400px;
  text-shadow: 0 1px 8px rgba(0,0,0,0.3);
}

/* --- Stats --- */
.brand-stats {
  display: flex;
  align-items: center;
  gap: 0;
  margin-top: 32px;
  padding: 18px 28px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 18px;
  backdrop-filter: blur(16px);
  max-width: 400px;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.06), 0 8px 32px rgba(0,0,0,0.1);
}

.stat-item {
  text-align: center;
  flex: 1;
}

.stat-num {
  font-family: 'Outfit', sans-serif;
  font-weight: 800;
  font-size: 1.75rem;
  letter-spacing: -0.03em;
  color: #fff;
  text-shadow: 0 0 20px rgba(255,255,255,0.1);
}

.stat-num[data-suffix]::after {
  content: attr(data-suffix);
  font-size: 1rem;
  opacity: 0.6;
  margin-left: 1px;
}

.stat-label {
  font-size: 0.75rem;
  font-weight: 500;
  color: rgba(255,255,255,0.55);
  margin-top: 4px;
  letter-spacing: 0.06em;
}

.stat-divider {
  width: 1px;
  height: 36px;
  background: linear-gradient(180deg, transparent, rgba(255,255,255,0.15), transparent);
  flex-shrink: 0;
}

/* ============================================================
   FORM SIDE
   ============================================================ */
.form-side {
  flex: 1;
  background: linear-gradient(170deg, #ffffff 0%, #f8fafc 30%, #f1f5f9 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 48px 48px;
  position: relative;
  overflow: hidden;
}

/* --- Dot Grid --- */
.dot-grid {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle, #e2e8f0 0.8px, transparent 0.8px);
  background-size: 28px 28px;
  opacity: 0.4;
  z-index: 0;
  pointer-events: none;
}

/* --- Gradient Blobs --- */
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  pointer-events: none;
  z-index: 1;
  opacity: 0.5;
}

.blob-1 {
  width: 400px;
  height: 400px;
  background: rgba(29,78,216,0.08);
  top: -120px;
  right: -100px;
  animation: blob-drift 20s ease-in-out infinite alternate;
}

.blob-2 {
  width: 300px;
  height: 300px;
  background: rgba(124,58,237,0.06);
  bottom: 40px;
  left: -80px;
  animation: blob-drift 24s ease-in-out infinite alternate-reverse;
}

.blob-3 {
  width: 200px;
  height: 200px;
  background: rgba(16,185,129,0.05);
  top: 45%;
  right: 8%;
  animation: blob-drift 16s ease-in-out infinite alternate;
}

@keyframes blob-drift {
  0%   { transform: translate(0, 0) scale(1); }
  33%  { transform: translate(25px, -20px) scale(1.08); }
  66%  { transform: translate(-15px, 15px) scale(0.94); }
  100% { transform: translate(10px, -10px) scale(1.02); }
}

/* --- Form Wrapper --- */
.form-wrapper {
  width: 100%;
  max-width: 420px;
  position: relative;
  z-index: 2;
  background: rgba(255,255,255,0.65);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,0.8);
  border-radius: 28px;
  padding: 44px 40px;
  box-shadow:
    0 0 0 1px rgba(0,0,0,0.02),
    0 4px 16px rgba(0,0,0,0.04),
    0 12px 40px rgba(0,0,0,0.03);
  animation: form-enter 1s cubic-bezier(0.16, 1, 0.3, 1) 0.1s both;
}

@keyframes form-enter {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
    filter: blur(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
    filter: blur(0);
  }
}

/* --- Login Header --- */
.login-header {
  margin-bottom: 36px;
  text-align: center;
}

.logo-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 10px 24px 10px 12px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border: 1.5px solid #e5e7eb;
  border-radius: 18px;
  font-family: 'Outfit', sans-serif;
  font-weight: 800;
  font-size: 1rem;
  color: var(--bc-primary);
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(29,78,216,0.06), 0 1px 2px rgba(0,0,0,0.02);
}

.logo-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #1d4ed8, #6366f1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  box-shadow: 0 2px 8px rgba(29,78,216,0.25);
}

.login-header h2 {
  font-family: 'Outfit', sans-serif;
  font-weight: 800;
  font-size: 1.75rem;
  color: var(--bc-text);
  letter-spacing: -0.02em;
  margin-bottom: 12px;
  background: linear-gradient(135deg, #1d4ed8 0%, #4f46e5 50%, #7c3aed 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-subtitle {
  color: var(--bc-text-muted);
  font-size: 0.9375rem;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.subtitle-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--bc-success);
  box-shadow: 0 0 12px rgba(16,185,129,0.6);
  animation: dot-pulse 2.5s ease-in-out infinite;
}

@keyframes dot-pulse {
  0%, 100% { opacity: 1; box-shadow: 0 0 12px rgba(16,185,129,0.6); }
  50% { opacity: 0.4; box-shadow: 0 0 4px rgba(16,185,129,0.2); }
}

/* ============================================================
   FORM FIELDS
   ============================================================ */
.bc-form-group {
  margin-bottom: 24px;
  animation: fade-in-up 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.bc-form-group:nth-child(1) { animation-delay: 0.15s; }
.bc-form-group:nth-child(2) { animation-delay: 0.3s; }

.bc-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.75rem;
  font-weight: 800;
  color: var(--bc-text);
  text-transform: uppercase;
  letter-spacing: 0.14em;
  margin-bottom: 10px;
  padding-left: 2px;
}

.bc-label .el-icon {
  font-size: 14px;
  color: var(--bc-text-lighter);
}

.bc-form-group :deep(.el-input__wrapper) {
  border-radius: 14px;
  padding: 12px 18px;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
  border: 1.5px solid rgba(229,231,235,0.6);
  background: rgba(255,255,255,0.8);
}

.bc-form-group :deep(.el-input__wrapper:hover) {
  border-color: rgba(29,78,216,0.25);
  box-shadow: 0 4px 16px rgba(29,78,216,0.06);
  background: rgba(255,255,255,0.95);
}

.bc-form-group :deep(.el-input__wrapper:focus-within) {
  border-color: var(--bc-primary);
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(29,78,216,0.08), 0 4px 20px rgba(29,78,216,0.08);
}

.bc-form-group :deep(.el-input__inner) {
  font-size: 1rem;
  height: 32px;
}

.bc-form-group :deep(.el-input__inner::placeholder) {
  color: var(--bc-text-lighter);
}

/* ============================================================
   OPTIONS BAR
   ============================================================ */
.options-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  font-size: 0.875rem;
  font-weight: 500;
  animation: fade-in-up 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.45s both;
}

.form-link {
  color: var(--bc-primary);
  text-decoration: none;
  font-weight: 600;
  transition: all 0.2s;
}

.form-link:hover {
  color: var(--bc-primary-light);
}

/* ============================================================
   SUBMIT BUTTON
   ============================================================ */
.btn-adventure {
  width: 100%;
  height: 58px;
  border: none;
  cursor: pointer;
  border-radius: 16px;
  font-family: 'Outfit', sans-serif;
  font-weight: 800;
  font-size: 1.0625rem;
  letter-spacing: 0.04em;
  color: white;
  position: relative;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  animation: fade-in-up 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.55s both;
}

.btn-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 35%, #6366f1 70%, #8b5cf6 100%);
  background-size: 300% 300%;
  animation: gradient-shift 6s ease infinite;
}

@keyframes gradient-shift {
  0%   { background-position: 0% 50%; }
  50%  { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.btn-shimmer {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    105deg,
    transparent 25%,
    rgba(255,255,255,0.1) 42%,
    rgba(255,255,255,0.18) 50%,
    rgba(255,255,255,0.1) 58%,
    transparent 75%
  );
  transform: translateX(-130%);
  transition: transform 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-adventure:hover .btn-shimmer {
  transform: translateX(130%);
}

.btn-glow {
  position: absolute;
  inset: -2px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(29,78,216,0.4), rgba(99,102,241,0.3), rgba(139,92,246,0.4));
  opacity: 0;
  filter: blur(12px);
  transition: opacity 0.4s ease;
  z-index: -1;
}

.btn-adventure:hover .btn-glow {
  opacity: 1;
}

.btn-adventure:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 40px rgba(29,78,216,0.3);
}

.btn-adventure:active {
  transform: translateY(-1px) scale(0.99);
}

.btn-adventure:disabled {
  cursor: not-allowed;
  opacity: 0.8;
  transform: none !important;
}

.btn-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.btn-arrow {
  width: 20px;
  height: 20px;
  transition: transform 0.3s ease;
}

.btn-adventure:hover .btn-arrow {
  transform: translateX(4px);
}

.spinner {
  width: 20px;
  height: 20px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ============================================================
   ERROR ALERT
   ============================================================ */
.error-alert {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
  padding: 14px 18px;
  background: linear-gradient(135deg, #fef2f2, #fff1f2);
  border: 1px solid #fecaca;
  border-radius: 14px;
  color: #dc2626;
  font-size: 0.9rem;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(239,68,68,0.06);
}

.error-alert .el-icon {
  font-size: 18px;
  flex-shrink: 0;
  color: #ef4444;
}

.alert-fade-enter-active { transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
.alert-fade-leave-active { transition: all 0.3s ease; }
.alert-fade-enter-from { opacity: 0; transform: translateY(-10px) scale(0.98); }
.alert-fade-leave-to { opacity: 0; transform: translateY(-4px); }

/* ============================================================
   FOOTER BADGES
   ============================================================ */
.brand-footer {
  margin-top: auto;
  width: 100%;
  max-width: 420px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0;
  padding-top: 28px;
  position: relative;
  z-index: 2;
  animation: fade-in-up 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.7s both;
}

.brand-footer::before {
  content: '';
  position: absolute;
  top: 0;
  left: 10%;
  right: 10%;
  height: 1px;
  background: linear-gradient(90deg, transparent, #d1d5db, transparent);
}

.badge-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: var(--bc-text-lighter);
  opacity: 0.55;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  padding: 0 22px;
  cursor: default;
}

.badge-item:hover {
  opacity: 1;
  color: var(--bc-primary);
  transform: translateY(-2px);
}

.badge-item span {
  font-size: 0.6875rem;
  font-weight: 800;
  text-transform: uppercase;
  margin-top: 6px;
  letter-spacing: 0.06em;
}

.badge-divider {
  width: 1px;
  height: 28px;
  background: linear-gradient(180deg, transparent, #d1d5db, transparent);
  flex-shrink: 0;
}

/* ============================================================
   ENTRANCE ANIMATION
   ============================================================ */
@keyframes fade-in-up {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 1024px) {
  .brand-side { display: none; }
  .form-side { flex: 1; }
  .form-wrapper { background: transparent; backdrop-filter: none; border: none; box-shadow: none; padding: 0; border-radius: 0; }
}

@media (max-width: 768px) {
  .form-side { padding: 32px 24px; }
  .form-wrapper { max-width: 100%; padding: 32px 24px; border-radius: 20px; }
  .login-header h2 { font-size: 1.4rem; }
  .brand-footer { max-width: 100%; padding-top: 24px; }
  .badge-item { padding: 0 14px; }
  .badge-item span { font-size: 0.6rem; }
}

@media (max-width: 480px) {
  .form-side { padding: 24px 16px; }
  .form-wrapper { padding: 28px 20px; }
  .login-header { margin-bottom: 28px; }
}
</style>
