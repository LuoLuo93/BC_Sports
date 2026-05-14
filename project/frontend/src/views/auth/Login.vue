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
      <div class="brand-text">
        <p>PRO ATHLETIC GEAR</p>
        <h2>Touch the<br>Highest Peak</h2>
        <p>B.C.SPORTS 巅峰数字化中心，为每一场远征保驾护航。</p>
      </div>
    </div>

    <!-- Right: Form -->
    <div class="form-side">
      <div class="form-wrapper">
        <div class="login-header">
          <div class="logo-badge">
            <el-icon><Location /></el-icon>
            B.C.SPORTS
          </div>
          <h2>让人人尽享户外运动的快乐</h2>
          <p class="login-subtitle"><el-icon><CircleCheck /></el-icon> 请通过您的专业凭据访问系统中心</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
          <div class="bc-form-group">
            <label class="bc-label" for="username">IDENTITY / 通行证</label>
            <el-input
              id="username"
              v-model="form.username"
              placeholder="请输入员工账号/ID"
              size="large"
              :prefix-icon="User"
            />
          </div>

          <div class="bc-form-group">
            <label class="bc-label" for="password">SECURITY / 校验码</label>
            <el-input
              id="password"
              v-model="form.password"
              type="password"
              placeholder="请输入访问密钥"
              size="large"
              :prefix-icon="Key"
              show-password
              @keyup.enter="handleLogin"
            />
          </div>

          <div class="options-bar">
            <el-checkbox v-model="form.rememberMe">信任此设备</el-checkbox>
            <a href="#" class="form-link">授权遇到困难?</a>
          </div>

          <el-button
            type="primary"
            class="btn-adventure"
            :loading="loading"
            @click="handleLogin"
          >
            <span v-if="!loading">进入数字化中心</span>
            <span v-else>正在验证专业凭据...</span>
            <el-icon v-if="!loading"><ArrowRight /></el-icon>
          </el-button>
        </el-form>

        <el-alert
          v-if="errorMsg"
          :title="errorMsg"
          type="error"
          show-icon
          :closable="false"
          class="error-alert"
        />
      </div>

      <div class="brand-footer">
        <div class="badge-item">
          <el-icon :size="24"><Orange /></el-icon>
          <span>Hiking / 徒步</span>
        </div>
        <div class="badge-item">
          <el-icon :size="24"><Coffee /></el-icon>
          <span>Camping / 露营</span>
        </div>
        <div class="badge-item">
          <el-icon :size="24"><IceCreamRound /></el-icon>
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

async function handleLogin() {
  errorMsg.value = ''
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form.value)
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
:root {
  --summit-blue: #1d4ed8;
  --alpine-white: #ffffff;
  --stone-warm: #fafaf9;
  --forest-deep: #064e3b;
  --text-main: #1c1917;
  --text-muted: #78716c;
  --border-light: #e7e5e4;
  --success-green: #10b981;
}

.login-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--alpine-white);
  font-family: 'Inter', sans-serif;
}

.brand-side {
  flex: 0 0 68%;
  position: relative;
  background-color: var(--forest-deep);
  overflow: hidden;
}

.brand-image {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  object-fit: cover;
  filter: contrast(1.1) brightness(0.9);
  transition: transform 15s ease-in-out;
}

.brand-side:hover .brand-image {
  transform: scale(1.1);
}

.brand-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(6, 78, 59, 0.4) 0%, rgba(29, 78, 216, 0.2) 100%);
  z-index: 1;
}

.brand-text {
  position: absolute;
  bottom: 60px; left: 60px;
  z-index: 2;
  color: white;
  max-width: 500px;
}

.brand-text h2 {
  font-family: 'Outfit', sans-serif;
  font-weight: 900;
  font-size: 3.5rem;
  line-height: 1;
  margin-bottom: 24px;
  text-transform: uppercase;
  letter-spacing: -0.02em;
}

.brand-text p {
  font-size: 1.125rem;
  font-weight: 500;
  opacity: 0.9;
  margin-bottom: 0;
}

.form-side {
  flex: 1;
  background-color: var(--stone-warm);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px;
  position: relative;
}

.form-wrapper {
  width: 100%;
  max-width: 400px;
}

.login-header {
  margin-bottom: 48px;
  text-align: center;
}

.logo-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px 16px;
  background: white;
  border: 1.5px solid var(--border-light);
  border-radius: 12px;
  font-family: 'Outfit', sans-serif;
  font-weight: 800;
  font-size: 1rem;
  color: var(--summit-blue);
  margin-bottom: 32px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}

.login-header h2 {
  font-family: 'Outfit', sans-serif;
  font-weight: 800;
  font-size: 1.75rem;
  color: var(--text-main);
  letter-spacing: -0.02em;
  margin-bottom: 8px;
  background: linear-gradient(135deg, var(--summit-blue), #7c3aed);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-subtitle {
  color: var(--text-muted);
  font-size: 0.9375rem;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.bc-form-group {
  margin-bottom: 24px;
}

.bc-label {
  display: block;
  font-size: 0.75rem;
  font-weight: 800;
  color: var(--text-main);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin-bottom: 10px;
  padding-left: 2px;
}

.options-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  font-size: 0.875rem;
  font-weight: 600;
}

.form-link {
  color: var(--summit-blue);
  text-decoration: none;
}

.btn-adventure {
  width: 100%;
  height: 64px;
  border-radius: 16px !important;
  font-family: 'Outfit', sans-serif;
  font-weight: 800;
  font-size: 1.1rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.error-alert {
  margin-top: 24px;
}

.brand-footer {
  margin-top: auto;
  width: 100%;
  display: flex;
  justify-content: space-around;
  padding-top: 40px;
  border-top: 1px solid var(--border-light);
}

.badge-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: var(--text-muted);
  opacity: 0.6;
  transition: opacity 0.3s;
}

.badge-item:hover { opacity: 1; }
.badge-item span { font-size: 0.7rem; font-weight: 800; text-transform: uppercase; margin-top: 4px; }

@media (max-width: 1024px) {
  .brand-side { display: none; }
  .form-side { flex: 1; }
}
</style>
