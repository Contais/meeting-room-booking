<template>
  <div class="forgot-container">
    <div class="bg-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
    </div>

    <div class="forgot-card">
      <div class="card-header">
        <div class="brand-mark">
          <svg viewBox="0 0 32 32" fill="none" width="40" height="40">
            <rect x="4.75" y="4.75" width="22.5" height="22.5" rx="4.5" stroke="#667eea" stroke-width="2.5"/>
            <path d="M11 21.25 L11 10.625 L16 16 L21 10.625 L21 21.25" stroke="#667eea" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="23.625" cy="10.125" r="1.5" fill="#1F6FEB"/>
          </svg>
        </div>
        <h1 class="card-title">找回密码</h1>
        <p v-if="!resetSuccess" class="card-subtitle">
          {{ step === 1 ? '验证身份后设置新密码' : '请设置新的登录密码' }}
        </p>
      </div>

      <ol v-if="!resetSuccess" class="steps" aria-label="找回密码步骤">
        <li :class="['step', { 'is-active': step === 1, 'is-done': step === 2 }]">
          <span class="step-dot">
            <el-icon v-if="step === 2"><Check /></el-icon>
            <template v-else>1</template>
          </span>
          <span class="step-text">验证身份</span>
        </li>
        <li class="step-line" :class="{ 'is-active': step === 2 }"></li>
        <li :class="['step', { 'is-active': step === 2 }]">
          <span class="step-dot">2</span>
          <span class="step-text">设置新密码</span>
        </li>
      </ol>

      <transition name="step-fade" mode="out-in">
        <el-form
          v-if="!resetSuccess && step === 1"
          ref="step1FormRef"
          :model="step1Form"
          :rules="step1Rules"
          label-position="top"
          class="forgot-form"
          @submit.prevent
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="step1Form.username"
              ref="usernameRef"
              size="large"
              placeholder="请输入用户名"
              :prefix-icon="User"
              autocomplete="username"
              maxlength="32"
              @keyup.enter="handleNext"
            />
          </el-form-item>

          <el-form-item label="邮箱验证码" prop="code">
            <div class="code-row">
              <el-input
                v-model="step1Form.code"
                size="large"
                placeholder="6 位数字"
                maxlength="6"
                :prefix-icon="Message"
                autocomplete="one-time-code"
                inputmode="numeric"
                @keyup.enter="handleNext"
              />
              <el-button
                class="send-btn"
                size="large"
                :disabled="countdown > 0"
                :loading="sendingCode"
                @click="handleSendCode"
              >
                {{ countdown > 0 ? `${countdown}s 后重发` : '发送验证码' }}
              </el-button>
            </div>
            <p class="field-helper">验证码将发送至账号绑定的邮箱，5 分钟内有效</p>
          </el-form-item>

          <el-button type="primary" size="large" class="primary-btn" @click="handleNext">下一步</el-button>
        </el-form>

        <el-form
          v-else-if="!resetSuccess && step === 2"
          ref="step2FormRef"
          :model="step2Form"
          :rules="step2Rules"
          label-position="top"
          class="forgot-form"
          @submit.prevent
        >
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="step2Form.newPassword"
              ref="newPasswordRef"
              type="password"
              show-password
              size="large"
              placeholder="6-64 位字符"
              :prefix-icon="Lock"
              autocomplete="new-password"
              maxlength="64"
              @keyup.enter="handleReset"
            />
          </el-form-item>

          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="step2Form.confirmPassword"
              type="password"
              show-password
              size="large"
              placeholder="再次输入新密码"
              :prefix-icon="Lock"
              autocomplete="new-password"
              maxlength="64"
              @keyup.enter="handleReset"
            />
          </el-form-item>

          <p class="field-helper">密码需 6-64 位字符，建议使用字母、数字和符号组合</p>

          <div class="btn-row">
            <el-button size="large" class="secondary-btn" @click="handleBack">上一步</el-button>
            <el-button type="primary" size="large" class="primary-btn" :loading="resetting" @click="handleReset">
              确认重置
            </el-button>
          </div>
        </el-form>

        <div v-else class="success-panel">
          <div class="success-icon">
            <el-icon :size="34"><Check /></el-icon>
          </div>
          <h2 class="success-title">密码重置成功</h2>
          <p class="success-desc">请使用新密码登录您的账号</p>
          <el-button type="primary" size="large" class="primary-btn" @click="goLogin">返回登录</el-button>
        </div>
      </transition>

      <div class="card-footer">
        <router-link :to="{ name: 'Login' }" class="back-link">已有账号？返回登录</router-link>
      </div>
    </div>

    <div class="icp-wrap">
      <IcpFooter />
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElInput } from 'element-plus'
import { Check, Lock, Message, User } from '@element-plus/icons-vue'
import { sendPasswordResetCode, resetPasswordByCode } from '@/api/user'
import IcpFooter from '@/components/IcpFooter.vue'

const route = useRoute()
const router = useRouter()

const step = ref(1)
const sendingCode = ref(false)
const resetting = ref(false)
const resetSuccess = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const step1FormRef = ref<FormInstance>()
const step2FormRef = ref<FormInstance>()
const usernameRef = ref<InstanceType<typeof ElInput>>()
const newPasswordRef = ref<InstanceType<typeof ElInput>>()

const step1Form = reactive({ username: '', code: '' })
const step2Form = reactive({ newPassword: '', confirmPassword: '' })

const step1Rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 32, message: '用户名长度为 2-32 个字符', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '请输入 6 位数字验证码', trigger: 'blur' },
  ],
}

const step2Rules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度为 6-64 位字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: string | Error) => void) => {
        if (value !== step2Form.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

async function handleSendCode(): Promise<void> {
  if (!step1Form.username.trim()) {
    ElMessage.warning('请先输入用户名')
    usernameRef.value?.focus()
    return
  }
  sendingCode.value = true
  try {
    await sendPasswordResetCode(step1Form.username.trim())
    ElMessage.success('验证码已发送，请查收邮箱')
    startCountdown()
  } catch {
    // 错误由响应拦截器统一提示
  } finally {
    sendingCode.value = false
  }
}

function startCountdown(): void {
  countdown.value = 60
  countdownTimer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0 && countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

function stopCountdown(): void {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

async function handleNext(): Promise<void> {
  const valid = await step1FormRef.value?.validate().catch(() => false)
  if (!valid) return
  step.value = 2
}

function handleBack(): void {
  step.value = 1
}

async function handleReset(): Promise<void> {
  const valid = await step2FormRef.value?.validate().catch(() => false)
  if (!valid) return
  resetting.value = true
  try {
    await resetPasswordByCode({
      username: step1Form.username.trim(),
      code: step1Form.code.trim(),
      newPassword: step2Form.newPassword,
    })
    stopCountdown()
    resetSuccess.value = true
  } catch {
    // 错误由响应拦截器统一提示
  } finally {
    resetting.value = false
  }
}

function goLogin(): void {
  router.push({ name: 'Login', query: { username: step1Form.username.trim() } })
}

watch(step, (value) => {
  if (value === 2) {
    void nextTick(() => newPasswordRef.value?.focus())
  }
})

onMounted(() => {
  const username = typeof route.query.username === 'string' ? route.query.username : ''
  if (username) {
    step1Form.username = username
  }
  void nextTick(() => usernameRef.value?.focus())
})

onUnmounted(() => {
  stopCountdown()
})
</script>

<style scoped>
.forgot-container {
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

.forgot-card {
  width: 460px;
  max-width: calc(100% - 32px);
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.4);
  padding: 44px 40px 32px;
  position: relative;
  z-index: 1;
}
.card-header { text-align: center; margin-bottom: 28px; }
.brand-mark {
  width: 56px;
  height: 56px;
  margin: 0 auto 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.brand-mark svg { width: 40px; height: 40px; }
.card-title { font-size: 26px; font-weight: 700; color: #1a1a2e; margin: 0 0 8px 0; }
.card-subtitle { font-size: 14px; color: #8c8c8c; margin: 0; line-height: 1.5; }

.steps { display: flex; align-items: center; list-style: none; margin: 0 0 30px; padding: 0; }
.step { display: flex; align-items: center; gap: 8px; color: #9a9a9a; font-size: 13px; }
.step-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #ececec;
  color: #9a9a9a;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
  transition: all 0.25s ease;
}
.step.is-active { color: #1a1a2e; font-weight: 600; }
.step.is-active .step-dot { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; box-shadow: 0 4px 12px rgba(102, 126, 234, 0.35); }
.step.is-done { color: #667eea; }
.step.is-done .step-dot { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
.step-line { flex: 1; height: 2px; background: #ececec; border-radius: 1px; margin: 0 12px; transition: background 0.25s ease; }
.step-line.is-active { background: linear-gradient(90deg, #667eea, #764ba2); }

.forgot-form { width: 100%; }
.forgot-form :deep(.el-form-item) { margin-bottom: 22px; }
.forgot-form :deep(.el-form-item__label) { font-size: 14px; font-weight: 600; color: #333; line-height: 1.4; }
.forgot-form :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e0e0e0 inset;
  padding: 4px 12px;
  transition: all 0.3s ease;
}
.forgot-form :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px #c0c0c0 inset; }
.forgot-form :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 2px #667eea inset; }

.code-row { display: flex; gap: 10px; width: 100%; }
.code-row .el-input { flex: 1; }
.send-btn { height: 44px; border-radius: 10px; white-space: nowrap; }
.field-helper { font-size: 12px; color: #9a9a9a; line-height: 1.5; margin: 8px 0 0; }

.primary-btn {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;
}
.primary-btn:hover { transform: translateY(-1px); box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4); }
.primary-btn:active { transform: translateY(0); }
.btn-row { display: flex; gap: 12px; }
.btn-row .primary-btn { flex: 1; }
.secondary-btn {
  flex: 1;
  height: 48px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
}

.success-panel { text-align: center; padding: 12px 0 4px; }
.success-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto 20px;
  border-radius: 50%;
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12px 28px rgba(16, 185, 129, 0.3);
}
.success-title { font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0 0 8px 0; }
.success-desc { font-size: 14px; color: #8c8c8c; margin: 0 0 28px 0; }

.card-footer {
  text-align: center;
  margin-top: 26px;
  padding-top: 18px;
  border-top: 1px solid #f0f0f0;
}
.back-link { font-size: 14px; color: #667eea; text-decoration: none; transition: color 0.2s ease; }
.back-link:hover { color: #764ba2; }

.icp-wrap {
  position: absolute;
  bottom: 16px;
  left: 0;
  right: 0;
  color: rgba(255, 255, 255, 0.6);
  z-index: 1;
}

.step-fade-enter-active,
.step-fade-leave-active { transition: opacity 0.25s ease, transform 0.25s ease; }
.step-fade-enter-from { opacity: 0; transform: translateY(8px); }
.step-fade-leave-to { opacity: 0; transform: translateY(-8px); }

@media (max-width: 480px) {
  .forgot-card { padding: 34px 22px 28px; }
  .card-title { font-size: 22px; }
  .steps { margin-bottom: 24px; }
  .step-text { font-size: 12px; }
}

@media (prefers-reduced-motion: reduce) {
  .shape { animation: none; }
  .step-fade-enter-active,
  .step-fade-leave-active { transition: none; }
}
</style>
