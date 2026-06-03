<template>
  <div class="login-layout">
    <!-- Left: Brand -->
    <div class="brand-side">
      <div class="brand-image-fallback"></div>
      <img class="brand-image" src="/index.avif" alt="Outdoor" />
      <div class="brand-overlay"></div>

      <!-- Noise texture -->
      <div class="noise-layer"></div>

      <!-- Mesh gradient orbs -->
      <div class="mesh-orb orb-1"></div>
      <div class="mesh-orb orb-2"></div>
      <div class="mesh-orb orb-3"></div>

      <!-- Light beams -->
      <div class="light-beams">
        <div class="beam beam-1"></div>
        <div class="beam beam-2"></div>
      </div>

      <!-- Floating particles -->
      <div class="particles">
        <span v-for="i in 20" :key="i" class="particle" :class="'p-' + (i % 3)" :style="particleStyle(i)"></span>
      </div>

      <!-- Mountain silhouette with glow -->
      <div class="mountain-glow"></div>
      <svg class="mountain-silhouette" viewBox="0 0 1440 220" preserveAspectRatio="none">
        <path d="M0,220 L0,150 L100,90 L200,130 L340,55 L460,100 L560,25 L680,85 L800,35 L940,100 L1060,30 L1180,75 L1300,50 L1440,90 L1440,220 Z" fill="rgba(0,0,0,0.25)"/>
        <path d="M0,220 L0,165 L160,115 L340,155 L520,95 L700,140 L880,80 L1060,125 L1240,90 L1440,135 L1440,220 Z" fill="rgba(0,0,0,0.12)"/>
      </svg>

      <div class="brand-text">
        <div class="brand-badge">
          <span class="badge-dot"></span>
          EST. 2024
        </div>
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
            <span>{{ systemName }}</span>
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
              autofocus
              autocomplete="username"
              @keyup.enter="$refs.passwordInput?.focus()"
            />
          </div>

          <div class="bc-form-group">
            <label class="bc-label" for="password">
              <el-icon><Key /></el-icon>
              SECURITY / 校验码
            </label>
            <el-input
              id="password"
              ref="passwordInput"
              v-model="form.password"
              type="password"
              placeholder="请输入访问密钥"
              size="large"
              show-password
              autocomplete="current-password"
              @keyup.enter="handleLogin"
              @keyup="checkCapsLock"
            />
            <transition name="alert-fade">
              <div v-if="capsLockOn" class="caps-lock-tip">
                <el-icon><WarningFilled /></el-icon>
                <span>Caps Lock 已开启</span>
              </div>
            </transition>
          </div>

          <div v-if="captchaEnabled" class="bc-form-group">
            <label class="bc-label" for="captcha">
              <el-icon><Key /></el-icon>
              CAPTCHA / 验证码
            </label>
            <div class="captcha-row">
              <el-input
                id="captcha"
                v-model="form.captchaCode"
                placeholder="请输入验证码"
                size="large"
                @keyup.enter="handleLogin"
              />
              <div class="captcha-img-wrapper" @click="loadCaptcha" title="点击刷新验证码">
                <img v-if="captchaImage" :src="captchaImage" alt="验证码" class="captcha-img" />
                <el-icon v-else :size="20" class="captcha-refresh"><Refresh /></el-icon>
              </div>
            </div>
          </div>

          <div class="options-bar">
            <!-- TODO: 信任此设备功能暂时关闭（Redis 连接问题） -->
            <!-- <el-checkbox v-model="form.rememberMe">信任此设备</el-checkbox> -->
            <a href="javascript:void(0)" class="form-link" @click="helpDialogVisible = true">授权遇到困难?</a>
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
            <el-icon><WarningFilled /></el-icon>
            <span>{{ errorMsg }}</span>
          </div>
        </transition>
      </div>

      <!-- 帮助对话框 -->
      <el-dialog v-model="helpDialogVisible" title="授权帮助" width="400px" :append-to-body="true">
        <div class="help-content">
          <p><strong>无法登录？请尝试以下方案：</strong></p>
          <ul>
            <li>确认账号密码是否正确（区分大小写）</li>
            <li>联系管理员确认账号是否已启用</li>
            <li>清除浏览器缓存后重试</li>
          </ul>
          <p class="help-contact">技术支持：请联系系统管理员</p>
        </div>
      </el-dialog>

      <!-- 版本号 -->
      <div class="version-info">v1.0.0</div>

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
import { getPublicConfig } from '@/api/config'
import { getCaptcha } from '@/api/auth'
import { applyPublicConfig } from '@/utils/appConfig'
import { User, Key, Location, WarningFilled, Orange, Coffee, IceCreamRound, Refresh } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const tabStore = useTabStore()

const formRef = ref(null)
const passwordInput = ref(null)
const loading = ref(false)
const errorMsg = ref('')
const capsLockOn = ref(false)
const helpDialogVisible = ref(false)
const systemName = ref('BC体育数据管理系统')
const captchaEnabled = ref(false)
const captchaKey = ref('')
const captchaImage = ref('')

const form = ref({
  username: '',
  password: '',
  rememberMe: false,
  captchaCode: ''
})

const rules = {
  username: [{ required: true, message: '请输入员工账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入访问密钥', trigger: 'blur' }]
}

onMounted(async () => {
  if (route.query.kicked === '1') {
    errorMsg.value = '您的账号已在其他设备登录，请重新登录'
  }
  try {
    const res = await getPublicConfig()
    if (res.code === 200 && res.data) {
      if (res.data['sys.name']) {
        systemName.value = res.data['sys.name']
        document.title = systemName.value
      }
      applyPublicConfig(res.data)
      if (res.data['security.captchaEnabled'] === 'true') {
        captchaEnabled.value = true
        loadCaptcha()
      }
    }
  } catch (e) {
    // use default name
  }
})

function checkCapsLock(e) {
  if (e.getModifierState) {
    capsLockOn.value = e.getModifierState('CapsLock')
  }
}

function particleStyle(i) {
  const size = 2 + Math.random() * 4
  return {
    width: size + 'px',
    height: size + 'px',
    left: (3 + Math.random() * 94) + '%',
    top: (3 + Math.random() * 70) + '%',
    animationDelay: (i * 0.8) + 's',
    animationDuration: (8 + Math.random() * 12) + 's'
  }
}

async function loadCaptcha() {
  try {
    const data = await getCaptcha()
    if (data.data) {
      captchaKey.value = data.data.captchaKey
      captchaImage.value = data.data.captchaImage
    }
  } catch (e) {
    // ignore
  }
}

async function handleLogin() {
  errorMsg.value = ''
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const loginData = {
    ...form.value,
    captchaKey: captchaEnabled.value ? captchaKey.value : undefined,
    captchaCode: captchaEnabled.value ? form.value.captchaCode : undefined
  }

  loading.value = true
  try {
    await authStore.login(loginData)
    tabStore.clearAll()
    tabStore.initDashboard()
    router.push('/')
  } catch (e) {
    errorMsg.value = e.message || '凭据检验未通过，请核对后重试'
    if (captchaEnabled.value) loadCaptcha()
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
  background-color: #070e0a;
  overflow: hidden;
}

.brand-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: contrast(1.12) brightness(0.55) saturate(1.3);
  animation: ken-burns 35s ease-in-out infinite alternate;
  transform-origin: 30% 35%;
}

.brand-image-fallback {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 20% 50%, #0d3320 0%, transparent 60%),
    radial-gradient(ellipse at 70% 20%, #0c2d4a 0%, transparent 50%),
    radial-gradient(ellipse at 50% 80%, #1a0a2e 0%, transparent 50%),
    linear-gradient(135deg, #050a07 0%, #0a1f15 30%, #0d1b2a 60%, #0a0a1a 100%);
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
    linear-gradient(180deg, rgba(0,0,0,0.05) 0%, rgba(5,70,50,0.2) 35%, rgba(0,0,0,0.6) 100%),
    linear-gradient(135deg, rgba(5,80,55,0.4) 0%, rgba(15,60,120,0.15) 50%, rgba(80,40,150,0.08) 100%);
  z-index: 1;
}

/* --- Noise texture --- */
.noise-layer {
  position: absolute;
  inset: 0;
  z-index: 2;
  opacity: 0.035;
  pointer-events: none;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 200px;
}

/* --- Mesh gradient orbs --- */
.mesh-orb {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  z-index: 3;
  mix-blend-mode: screen;
}

.orb-1 {
  width: 500px;
  height: 500px;
  top: -15%;
  left: -10%;
  background: radial-gradient(circle, rgba(16,185,129,0.2) 0%, transparent 70%);
  animation: orb-float 18s ease-in-out infinite alternate;
}

.orb-2 {
  width: 400px;
  height: 400px;
  bottom: 10%;
  right: -5%;
  background: radial-gradient(circle, rgba(59,130,246,0.15) 0%, transparent 70%);
  animation: orb-float 22s ease-in-out infinite alternate-reverse;
}

.orb-3 {
  width: 300px;
  height: 300px;
  top: 40%;
  left: 50%;
  background: radial-gradient(circle, rgba(139,92,246,0.1) 0%, transparent 70%);
  animation: orb-float 15s ease-in-out infinite alternate;
  animation-delay: -5s;
}

@keyframes orb-float {
  0% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(30px, -20px) scale(1.1); }
  100% { transform: translate(-20px, 15px) scale(0.95); }
}

/* --- Light beams --- */
.light-beams {
  position: absolute;
  inset: 0;
  z-index: 4;
  pointer-events: none;
  overflow: hidden;
}

.beam {
  position: absolute;
  width: 2px;
  background: linear-gradient(180deg, transparent, rgba(255,255,255,0.04), transparent);
  transform-origin: top center;
}

.beam-1 {
  height: 120%;
  top: -10%;
  left: 30%;
  transform: rotate(-15deg);
  animation: beam-sway 12s ease-in-out infinite alternate;
}

.beam-2 {
  height: 100%;
  top: 0;
  left: 65%;
  transform: rotate(10deg);
  animation: beam-sway 16s ease-in-out infinite alternate-reverse;
}

@keyframes beam-sway {
  0% { transform: rotate(-15deg) scaleY(1); opacity: 0.3; }
  50% { transform: rotate(-10deg) scaleY(1.05); opacity: 0.6; }
  100% { transform: rotate(-20deg) scaleY(0.95); opacity: 0.3; }
}

/* --- Floating Particles --- */
.particles {
  position: absolute;
  inset: 0;
  z-index: 5;
  pointer-events: none;
  overflow: hidden;
}

.particle {
  position: absolute;
  border-radius: 50%;
  opacity: 0;
  animation: particle-drift linear infinite;
}

.p-0 {
  background: rgba(16,185,129,0.5);
  box-shadow: 0 0 6px rgba(16,185,129,0.3);
}

.p-1 {
  background: rgba(59,130,246,0.4);
  box-shadow: 0 0 6px rgba(59,130,246,0.2);
}

.p-2 {
  background: rgba(255,255,255,0.3);
  box-shadow: 0 0 4px rgba(255,255,255,0.15);
}

@keyframes particle-drift {
  0%   { opacity: 0; transform: translateY(0) scale(0.5); }
  15%  { opacity: 0.8; transform: scale(1); }
  85%  { opacity: 0.3; }
  100% { opacity: 0; transform: translateY(-200px) scale(0.3); }
}

/* --- Mountain glow --- */
.mountain-glow {
  position: absolute;
  bottom: 50px;
  left: 0;
  width: 100%;
  height: 250px;
  background:
    radial-gradient(ellipse at 30% 100%, rgba(16,185,129,0.12) 0%, transparent 60%),
    radial-gradient(ellipse at 70% 100%, rgba(59,130,246,0.08) 0%, transparent 50%);
  z-index: 5;
  pointer-events: none;
  filter: blur(50px);
}

/* --- Mountain Silhouette --- */
.mountain-silhouette {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 220px;
  z-index: 6;
  pointer-events: none;
}

/* --- Brand Text --- */
.brand-text {
  position: absolute;
  bottom: 55px;
  left: 55px;
  z-index: 7;
  color: white;
  max-width: 540px;
  animation: fade-in-up 1.2s cubic-bezier(0.16, 1, 0.3, 1) 0.3s both;
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 7px 20px;
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: 24px;
  font-family: 'Outfit', sans-serif;
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.22em;
  color: rgba(255,255,255,0.75);
  margin-bottom: 28px;
  backdrop-filter: blur(16px);
  background: rgba(255,255,255,0.04);
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 10px rgba(16,185,129,0.6);
  animation: dot-blink 3s ease-in-out infinite;
}

@keyframes dot-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.brand-text h2 {
  font-family: 'Outfit', sans-serif;
  font-weight: 900;
  font-size: 3.75rem;
  line-height: 1;
  margin-bottom: 20px;
  text-transform: uppercase;
  letter-spacing: -0.03em;
  text-shadow: 0 4px 40px rgba(0,0,0,0.5);
}

.title-accent {
  background: linear-gradient(135deg, #6ee7b7, #38bdf8, #818cf8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 0 40px rgba(56,189,248,0.3));
}

.brand-text p {
  font-size: 1.0625rem;
  font-weight: 400;
  opacity: 0.75;
  margin-bottom: 0;
  line-height: 1.8;
  max-width: 400px;
  text-shadow: 0 1px 8px rgba(0,0,0,0.4);
}

/* --- Stats --- */
.brand-stats {
  display: flex;
  align-items: center;
  gap: 0;
  margin-top: 32px;
  padding: 20px 30px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 20px;
  backdrop-filter: blur(20px);
  max-width: 400px;
  box-shadow:
    inset 0 1px 0 rgba(255,255,255,0.05),
    0 8px 40px rgba(0,0,0,0.15),
    0 0 0 1px rgba(255,255,255,0.02);
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
  text-shadow: 0 0 20px rgba(255,255,255,0.08);
}

.stat-num[data-suffix]::after {
  content: attr(data-suffix);
  font-size: 1rem;
  opacity: 0.5;
  margin-left: 1px;
}

.stat-label {
  font-size: 0.75rem;
  font-weight: 500;
  color: rgba(255,255,255,0.45);
  margin-top: 5px;
  letter-spacing: 0.06em;
}

.stat-divider {
  width: 1px;
  height: 36px;
  background: linear-gradient(180deg, transparent, rgba(255,255,255,0.1), transparent);
  flex-shrink: 0;
}

/* ============================================================
   FORM SIDE
   ============================================================ */
.form-side {
  flex: 1;
  background: #fafbfc;
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
  background-image: radial-gradient(circle, #e0e4ea 0.7px, transparent 0.7px);
  background-size: 26px 26px;
  opacity: 0.5;
  z-index: 0;
  pointer-events: none;
}

/* --- Gradient Blobs --- */
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  pointer-events: none;
  z-index: 1;
  opacity: 0.45;
}

.blob-1 {
  width: 450px;
  height: 450px;
  background: rgba(29,78,216,0.07);
  top: -150px;
  right: -120px;
  animation: blob-drift 20s ease-in-out infinite alternate;
}

.blob-2 {
  width: 350px;
  height: 350px;
  background: rgba(124,58,237,0.05);
  bottom: 20px;
  left: -100px;
  animation: blob-drift 24s ease-in-out infinite alternate-reverse;
}

.blob-3 {
  width: 250px;
  height: 250px;
  background: rgba(16,185,129,0.04);
  top: 40%;
  right: 5%;
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
  background: rgba(255,255,255,0.7);
  backdrop-filter: blur(24px) saturate(1.2);
  -webkit-backdrop-filter: blur(24px) saturate(1.2);
  border: 1px solid rgba(255,255,255,0.85);
  border-radius: 28px;
  padding: 44px 40px;
  box-shadow:
    0 0 0 1px rgba(0,0,0,0.01),
    0 2px 8px rgba(0,0,0,0.02),
    0 8px 32px rgba(0,0,0,0.04),
    0 24px 60px rgba(0,0,0,0.03);
  animation: form-enter 1s cubic-bezier(0.16, 1, 0.3, 1) 0.1s both;
}

@keyframes form-enter {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.96);
    filter: blur(8px);
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
  background: #ffffff;
  border: 1.5px solid #e8eaef;
  border-radius: 18px;
  font-family: 'Outfit', sans-serif;
  font-weight: 800;
  font-size: 1rem;
  color: var(--bc-primary);
  margin-bottom: 24px;
  box-shadow:
    0 1px 3px rgba(0,0,0,0.04),
    0 4px 16px rgba(29,78,216,0.05);
  transition: box-shadow 0.3s ease;
}

.logo-badge:hover {
  box-shadow:
    0 1px 3px rgba(0,0,0,0.04),
    0 8px 24px rgba(29,78,216,0.1);
}

.logo-icon {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: linear-gradient(135deg, #1d4ed8, #6366f1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  box-shadow:
    0 2px 8px rgba(29,78,216,0.25),
    inset 0 1px 0 rgba(255,255,255,0.15);
}

.login-header h2 {
  font-family: 'Outfit', sans-serif;
  font-weight: 800;
  font-size: 1.75rem;
  color: var(--bc-text);
  letter-spacing: -0.02em;
  margin-bottom: 12px;
  background: linear-gradient(135deg, #111827 0%, #1e293b 50%, #334155 100%);
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
  background: #10b981;
  box-shadow: 0 0 12px rgba(16,185,129,0.5);
  animation: dot-pulse 2.5s ease-in-out infinite;
}

@keyframes dot-pulse {
  0%, 100% { opacity: 1; box-shadow: 0 0 12px rgba(16,185,129,0.5); }
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
  font-size: 0.7rem;
  font-weight: 800;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  margin-bottom: 10px;
  padding-left: 2px;
}

.bc-label .el-icon {
  font-size: 13px;
  color: #94a3b8;
}

.bc-form-group :deep(.el-input__wrapper) {
  border-radius: 14px;
  padding: 12px 18px;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 2px rgba(0,0,0,0.02);
  border: 1.5px solid #e5e7eb;
  background: rgba(255,255,255,0.9);
}

.bc-form-group :deep(.el-input__wrapper:hover) {
  border-color: rgba(29,78,216,0.2);
  box-shadow: 0 4px 16px rgba(29,78,216,0.05);
  background: #ffffff;
}

.bc-form-group :deep(.el-input__wrapper:focus-within) {
  border-color: #3b82f6;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(59,130,246,0.08), 0 4px 20px rgba(59,130,246,0.06);
}

.bc-form-group :deep(.el-input__inner) {
  font-size: 0.95rem;
  height: 30px;
}

.bc-form-group :deep(.el-input__inner::placeholder) {
  color: #b0b8c4;
}

/* --- Caps Lock Tip --- */
.caps-lock-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 8px 12px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 10px;
  color: #92400e;
  font-size: 0.8rem;
  font-weight: 500;
}

.caps-lock-tip .el-icon {
  font-size: 14px;
  color: #f59e0b;
}

/* --- Captcha --- */
.captcha-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.captcha-row .el-input {
  flex: 1;
}

.captcha-img-wrapper {
  width: 120px;
  height: 42px;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  border: 1.5px solid #e5e7eb;
  transition: all 0.25s ease;
}

.captcha-img-wrapper:hover {
  border-color: rgba(29,78,216,0.25);
  box-shadow: 0 2px 8px rgba(29,78,216,0.06);
}

.captcha-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-refresh {
  color: #94a3b8;
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
  color: #3b82f6;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.2s;
  position: relative;
}

.form-link::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 0;
  height: 1px;
  background: #3b82f6;
  transition: width 0.3s ease;
}

.form-link:hover {
  color: #2563eb;
}

.form-link:hover::after {
  width: 100%;
}

/* ============================================================
   SUBMIT BUTTON
   ============================================================ */
.btn-adventure {
  width: 100%;
  height: 56px;
  border: none;
  cursor: pointer;
  border-radius: 16px;
  font-family: 'Outfit', sans-serif;
  font-weight: 800;
  font-size: 1rem;
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
  background: linear-gradient(135deg, #0f172a 0%, #1e40af 40%, #3b82f6 70%, #6366f1 100%);
  background-size: 300% 300%;
  animation: gradient-shift 8s ease infinite;
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
    rgba(255,255,255,0.08) 42%,
    rgba(255,255,255,0.15) 50%,
    rgba(255,255,255,0.08) 58%,
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
  background: linear-gradient(135deg, rgba(30,64,175,0.4), rgba(59,130,246,0.3), rgba(99,102,241,0.4));
  opacity: 0;
  filter: blur(16px);
  transition: opacity 0.4s ease;
  z-index: -1;
}

.btn-adventure:hover .btn-glow {
  opacity: 1;
}

.btn-adventure:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 48px rgba(30,64,175,0.3);
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
  background: linear-gradient(90deg, transparent, #dde1e8, transparent);
}

.badge-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #94a3b8;
  opacity: 0.5;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  padding: 0 22px;
  cursor: default;
}

.badge-item:hover {
  opacity: 1;
  color: #3b82f6;
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
  background: linear-gradient(180deg, transparent, #dde1e8, transparent);
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

/* ============================================================
   HELP DIALOG
   ============================================================ */
.help-content {
  font-size: 0.9rem;
  line-height: 1.8;
  color: var(--bc-text);
}

.help-content ul {
  margin: 12px 0;
  padding-left: 20px;
}

.help-content li {
  margin-bottom: 8px;
}

.help-contact {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
  color: var(--bc-text-muted);
  font-size: 0.85rem;
}

/* ============================================================
   VERSION INFO
   ============================================================ */
.version-info {
  position: absolute;
  bottom: 16px;
  right: 24px;
  font-size: 0.75rem;
  color: #94a3b8;
  opacity: 0.4;
  z-index: 2;
}
</style>
