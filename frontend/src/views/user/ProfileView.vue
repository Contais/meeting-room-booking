<template>
  <div class="profile-container">
    <div class="profile-banner page-card">
      <div class="banner-avatar">{{ (userStore.userInfo?.username || 'U').charAt(0).toUpperCase() }}</div>
      <div class="banner-info">
        <h2 class="banner-name">{{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}</h2>
        <div class="banner-meta">
          <span class="banner-username">@{{ userStore.userInfo?.username }}</span>
          <el-tag :type="userStore.isAdmin() ? 'danger' : 'info'" effect="dark" round size="small">
            {{ userStore.isAdmin() ? '管理员' : '普通用户' }}
          </el-tag>
        </div>
      </div>
    </div>
    <div class="profile-grid">
      <div class="profile-card page-card">
        <div class="card-header">
          <el-icon class="card-header-icon"><User /></el-icon>
          <h3>个人信息</h3>
        </div>
        <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="80px">
          <el-form-item label="用户名">
            <el-input :value="userStore.userInfo?.username" disabled />
          </el-form-item>
          <el-form-item label="角色">
            <el-tag :type="userStore.isAdmin() ? 'danger' : 'info'" effect="dark" round>
              {{ userStore.isAdmin() ? '管理员' : '普通用户' }}
            </el-tag>
          </el-form-item>
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="btn-gradient" :loading="profileLoading" @click="handleUpdateProfile">
              保存修改
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="profile-card page-card">
        <div class="card-header">
          <el-icon class="card-header-icon"><Lock /></el-icon>
          <h3>修改密码</h3>
        </div>
        <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="80px">
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入旧密码" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="btn-gradient" :loading="passwordLoading" @click="handleChangePassword">
              修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { updateProfile, changePassword } from '@/api/user'

const userStore = useUserStore()
const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const profileLoading = ref(false)
const passwordLoading = ref(false)

const profileForm = reactive({ realName: '', phone: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const profileRules: FormRules = {}
const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度6-64个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

onMounted(async () => {
  await userStore.fetchUserInfo()
  profileForm.realName = userStore.userInfo?.realName || ''
  profileForm.phone = userStore.userInfo?.phone || ''
})

async function handleUpdateProfile() {
  const valid = await profileFormRef.value?.validate().catch(() => false)
  if (!valid) return
  profileLoading.value = true
  try {
    await updateProfile(profileForm)
    await userStore.fetchUserInfo()
    ElMessage.success('更新成功')
  } catch { /* */ } finally {
    profileLoading.value = false
  }
}

async function handleChangePassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return
  passwordLoading.value = true
  try {
    await changePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    ElMessage.success('密码修改成功')
    passwordFormRef.value?.resetFields()
  } catch { /* */ } finally {
    passwordLoading.value = false
  }
}
</script>

<style scoped>
.profile-container { display: flex; flex-direction: column; gap: 20px; }

.profile-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); gap: 20px; }

/* 横幅 */
.profile-banner {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 28px 32px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08), rgba(118, 75, 162, 0.08)), var(--bg-card);
}

.banner-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 600;
  flex-shrink: 0;
}

.banner-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 6px;
}

.banner-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.banner-username {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 表单卡片 */
.profile-card { padding: 24px; }

.profile-card .card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}

.profile-card .card-header-icon {
  font-size: 18px;
  color: var(--primary);
}

.profile-card .card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}
</style>
