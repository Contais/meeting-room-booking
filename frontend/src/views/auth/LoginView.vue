<template>
  <div class="login-container">
    <div class="bg-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
    </div>

    <div class="login-wrapper">
      <div class="brand-panel">
        <div class="brand-content">
          <div class="brand-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
            </svg>
          </div>
          <h1 class="brand-title">会议室预约系统</h1>
          <p class="brand-desc">高效管理会议室资源，轻松预约，智能协作</p>
          <div class="brand-features">
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>实时查看会议室状态</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>一键预约，智能冲突检测</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>日历视图，全局掌控</span>
            </div>
          </div>
        </div>
      </div>

      <div class="form-panel">
        <div class="form-content">
          <div class="form-header">
            <h2 class="form-title">欢迎回来</h2>
            <p class="form-subtitle">请登录您的账号</p>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="0" class="login-form">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" size="large" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password :prefix-icon="Lock" @keyup.enter="handleLogin" />
            </el-form-item>

            <div class="form-options">
              <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              <a href="javascript:;" class="forgot-link" @click="openForgot">忘记密码?</a>
            </div>

            <el-form-item>
              <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">
                <span v-if="!loading">登 录</span>
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <span class="footer-text">Meeting Room Booking System</span>
          </div>
        </div>
      </div>
    </div>
    <div class="icp-wrap">
      <IcpFooter />
    </div>

    <el-dialog v-model="forgotVisible" title="找回密码" width="420px" append-to-body>
      <el-form :model="forgotForm" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="forgotForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱验证码">
          <div class="code-row">
            <el-input v-model="forgotForm.code" placeholder="6 位验证码" maxlength="6" />
            <el-button :disabled="countdown > 0" :loading="sendingCode" @click="handleSendCode">
              {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="forgotForm.newPassword" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="forgotForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forgotVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="handleReset">重置密码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/auth'
import { sendPasswordResetCode, resetPasswordByCode } from '@/api/user'
import { useUserStore } from '@/stores/user'
import IcpFooter from '@/components/IcpFooter.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const rememberMe = ref(false)
const forgotVisible = ref(false)
const sendingCode = ref(false)
const resetting = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const form = reactive({ username: '', password: '' })
const forgotForm = reactive({ username: '', code: '', newPassword: '', confirmPassword: '' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login({ username: form.username, password: form.password })
    userStore.setUserToken(res.data.token)
    // 先用登录返回的基础信息占位，再立即拉取完整资料（含真实姓名、头像）
    userStore.setUserInfo({
      id: res.data.userId,
      username: res.data.username,
      role: res.data.role,
      phone: '',
      realName: '',
      status: 1,
      createTime: '',
    })
    await userStore.fetchUserInfo()
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/home'
    router.push(redirect)
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function openForgot() {
  forgotForm.username = form.username
  forgotVisible.value = true
}

async function handleSendCode() {
  if (!forgotForm.username.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  sendingCode.value = true
  try {
    await sendPasswordResetCode(forgotForm.username.trim())
    ElMessage.success('验证码已发送，请查收邮箱')
    countdown.value = 60
    countdownTimer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0 && countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }, 1000)
  } catch {
    // error handled by interceptor
  } finally {
    sendingCode.value = false
  }
}

async function handleReset() {
  if (!forgotForm.username.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!forgotForm.code.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }
  if (forgotForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (forgotForm.newPassword !== forgotForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  resetting.value = true
  try {
    await resetPasswordByCode({
      username: forgotForm.username.trim(),
      code: forgotForm.code.trim(),
      newPassword: forgotForm.newPassword,
    })
    ElMessage.success('密码已重置，请使用新密码登录')
    forgotVisible.value = false
    form.username = forgotForm.username
    form.password = ''
    forgotForm.code = ''
    forgotForm.newPassword = ''
    forgotForm.confirmPassword = ''
  } catch {
    // error handled by interceptor
  } finally {
    resetting.value = false
  }
}

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
  position: relative;
  overflow: hidden;
}
.bg-shapes { position: absolute; inset: 0; overflow: hidden; pointer-events: none; }
.shape { position: absolute; border-radius: 50%; opacity: 0.08; }
.shape-1 { width: 600px; height: 600px; background: linear-gradient(135deg, #667eea, #764ba2); top: -200px; right: -100px; animation: float 20s ease-in-out infinite; }
.shape-2 { width: 400px; height: 400px; background: linear-gradient(135deg, #f093fb, #f5576c); bottom: -150px; left: -100px; animation: float 15s ease-in-out infinite reverse; }
.shape-3 { width: 300px; height: 300px; background: linear-gradient(135deg, #4facfe, #00f2fe); top: 50%; left: 50%; transform: translate(-50%, -50%); animation: float 25s ease-in-out infinite; }
@keyframes float { 0%, 100% { transform: translateY(0) rotate(0deg); } 33% { transform: translateY(-30px) rotate(5deg); } 66% { transform: translateY(20px) rotate(-5deg); } }
.login-wrapper { display: flex; width: 900px; min-height: 520px; border-radius: 20px; overflow: hidden; box-shadow: 0 25px 60px rgba(0, 0, 0, 0.4); position: relative; z-index: 1; }
.brand-panel { flex: 1; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 60px 50px; display: flex; align-items: center; position: relative; overflow: hidden; }
.brand-panel::before { content: ''; position: absolute; top: -50%; right: -50%; width: 100%; height: 100%; background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%); pointer-events: none; }
.brand-content { position: relative; z-index: 1; }
.brand-icon { width: 64px; height: 64px; background: rgba(255, 255, 255, 0.2); border-radius: 16px; display: flex; align-items: center; justify-content: center; margin-bottom: 28px; backdrop-filter: blur(10px); }
.brand-icon svg { width: 36px; height: 36px; color: #fff; }
.brand-title { font-size: 32px; font-weight: 700; color: #fff; margin: 0 0 12px 0; }
.brand-desc { font-size: 15px; color: rgba(255, 255, 255, 0.75); margin: 0 0 40px 0; line-height: 1.6; }
.brand-features { display: flex; flex-direction: column; gap: 16px; }
.feature-item { display: flex; align-items: center; gap: 12px; color: rgba(255, 255, 255, 0.85); font-size: 14px; }
.feature-dot { width: 8px; height: 8px; border-radius: 50%; background: rgba(255, 255, 255, 0.6); flex-shrink: 0; }
.form-panel { flex: 1; background: #fff; padding: 60px 50px; display: flex; align-items: center; }
.form-content { width: 100%; }
.form-header { margin-bottom: 36px; }
.form-title { font-size: 28px; font-weight: 700; color: #1a1a2e; margin: 0 0 8px 0; }
.form-subtitle { font-size: 15px; color: #8c8c8c; margin: 0; }
.login-form { width: 100%; }
.login-form :deep(.el-input__wrapper) { border-radius: 10px; box-shadow: 0 0 0 1px #e0e0e0 inset; padding: 4px 12px; transition: all 0.3s ease; }
.login-form :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px #c0c0c0 inset; }
.login-form :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 2px #667eea inset; }
.login-form :deep(.el-form-item) { margin-bottom: 22px; }
.form-options { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.forgot-link { font-size: 13px; color: #667eea; text-decoration: none; }
.forgot-link:hover { color: #764ba2; }
.code-row { display: flex; gap: 8px; width: 100%; }
.code-row .el-input { flex: 1; }
.code-row .el-button { white-space: nowrap; }
.login-btn { width: 100%; height: 48px; border-radius: 10px; font-size: 16px; font-weight: 600; letter-spacing: 2px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border: none; transition: all 0.3s ease; }
.login-btn:hover { transform: translateY(-1px); box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4); }
.login-btn:active { transform: translateY(0); }
.form-footer { text-align: center; margin-top: 40px; padding-top: 24px; border-top: 1px solid #f0f0f0; }
.footer-text { font-size: 12px; color: #bbb; letter-spacing: 1px; }
.icp-wrap { position: absolute; bottom: 16px; left: 0; right: 0; color: rgba(255, 255, 255, 0.6); z-index: 1; }
@media (max-width: 768px) { .login-wrapper { flex-direction: column; width: 90%; min-height: auto; margin: 20px; } .brand-panel { padding: 40px 30px; } .form-panel { padding: 40px 30px; } .brand-title { font-size: 24px; } }
</style>
