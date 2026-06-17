<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Message } from '@arco-design/web-vue'
import {
  IconUser,
  IconLock,
  IconImport
} from '@arco-design/web-vue/es/icon'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const loginForm = reactive({
  username: '',
  password: ''
})

const formRef = ref(null)

async function handleSubmit() {
  try {
    const errors = await formRef.value?.validate()
    if (errors) return
  } catch {
    return
  }
  loading.value = true
  try {
    await userStore.login(loginForm)
    await userStore.getUserInfo()
    await userStore.getMenus()
    userStore.generateRoutes()
    Message.success('登录成功')
    const redirect = route.query.redirect ? decodeURIComponent(route.query.redirect) : '/dashboard'
    router.push(redirect)
  } catch (e) {
    console.error('登录失败:', e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <div class="deco-lines">
      <span class="line line-1"></span>
      <span class="line line-2"></span>
      <span class="line line-3"></span>
      <span class="line line-4"></span>
    </div>

    <div class="login-card">
      <div class="card-header">
        <div class="logo-wrap">
          <div class="logo-icon">
            <template v-for="i in 5" :key="i">
              <span class="bar" :style="{ height: 8 + i * 6 + 'px', animationDelay: i * 0.1 + 's' }"></span>
            </template>
          </div>
          <div class="logo-text">
            <h1 class="title">钢e通</h1>
            <p class="subtitle">钢铁行业ERP管理系统</p>
          </div>
        </div>
      </div>

      <a-form
        ref="formRef"
        :model="loginForm"
        layout="vertical"
        class="login-form"
      >
        <a-form-item field="username" label="账号" :rules="[{ required: true, message: '请输入账号' }]">
          <a-input
            v-model="loginForm.username"
            placeholder="请输入账号"
            size="large"
            allow-clear
          >
            <template #prefix>
              <icon-user />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item field="password" label="密码" :rules="[{ required: true, message: '请输入密码' }]">
          <a-input-password
            v-model="loginForm.password"
            placeholder="请输入密码"
            size="large"
          >
            <template #prefix>
              <icon-lock />
            </template>
          </a-input-password>
        </a-form-item>

        <a-button
          type="primary"
          size="large"
          long
          class="login-btn"
          :loading="loading"
          @click="handleSubmit"
        >
          <template #icon>
            <icon-import />
          </template>
          登 录
        </a-button>
      </a-form>

      <div class="card-footer">
        <p>© 2025 钢e通 ERP 系统 · 版权所有</p>
        <p class="tip-tip">打造钢铁行业智慧管理平台</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  position: relative;
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.deco-lines {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.line {
  position: absolute;
  background: linear-gradient(90deg, transparent, rgba(255, 107, 53, 0.15), transparent);
}

.line-1 {
  top: 15%;
  left: -10%;
  width: 120%;
  height: 1px;
  transform: rotate(-5deg);
}

.line-2 {
  top: 35%;
  left: -5%;
  width: 110%;
  height: 1px;
  transform: rotate(3deg);
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.08), transparent);
}

.line-3 {
  top: 65%;
  left: -10%;
  width: 120%;
  height: 1px;
  transform: rotate(-3deg);
}

.line-4 {
  top: 85%;
  left: -5%;
  width: 110%;
  height: 1px;
  transform: rotate(5deg);
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.08), transparent);
}

.login-card {
  position: relative;
  z-index: 10;
  width: 420px;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 8px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.4), 0 0 0 1px rgba(255, 255, 255, 0.05);
  padding: 40px 40px 28px;
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.logo-icon {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #ff6b35 0%, #f7931e 100%);
  border-radius: 10px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 3px;
  padding: 10px 6px;
  box-shadow: 0 6px 16px rgba(255, 107, 53, 0.4);
}

.bar {
  width: 6px;
  background: linear-gradient(180deg, #fff, rgba(255, 255, 255, 0.75));
  border-radius: 2px;
  animation: barPulse 1.8s ease-in-out infinite;
}

@keyframes barPulse {
  0%, 100% { transform: scaleY(1); opacity: 1; }
  50% { transform: scaleY(0.7); opacity: 0.7; }
}

.logo-text .title {
  font-size: 34px;
  font-weight: 800;
  margin: 0;
  background: linear-gradient(135deg, #1a1a2e 0%, #2d3748 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 2px;
}

.logo-text .subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #718096;
  letter-spacing: 1px;
}

.login-form :deep(.arco-form-item-label-col) {
  padding-bottom: 6px;
}

.login-form :deep(.arco-form-item) {
  margin-bottom: 20px;
}

.login-btn {
  height: 46px;
  margin-top: 8px;
  background: linear-gradient(90deg, #ff6b35 0%, #f7931e 100%) !important;
  border: none !important;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  box-shadow: 0 6px 16px rgba(255, 107, 53, 0.35);
  transition: all 0.3s ease;
}

.login-btn:hover {
  background: linear-gradient(90deg, #e85a2b 0%, #e6820f 100%) !important;
  box-shadow: 0 8px 20px rgba(255, 107, 53, 0.5);
  transform: translateY(-1px);
}

.login-btn:active {
  transform: translateY(0);
}

.card-footer {
  margin-top: 32px;
  text-align: center;
  border-top: 1px solid #e2e8f0;
  padding-top: 16px;
}

.card-footer p {
  margin: 4px 0;
  font-size: 12px;
  color: #a0aec0;
}

.card-footer .tip-tip {
  font-size: 11px;
  color: #cbd5e0;
}
</style>
