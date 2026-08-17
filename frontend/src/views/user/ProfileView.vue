<template>
  <div class="profile-container">
    <div class="profile-banner page-card">
      <div class="banner-avatar-wrapper">
        <div class="banner-avatar" :style="getAvatarStyle()">
          <img v-if="currentAvatarIsUrl" :src="userStore.userInfo?.avatar" class="banner-avatar-img" alt="头像" />
          <template v-else-if="selectedAvatarIcon">
            <el-icon :size="36"><component :is="selectedAvatarIcon" /></el-icon>
          </template>
          <template v-else>
            {{ (userStore.userInfo?.username || 'U').charAt(0).toUpperCase() }}
          </template>
        </div>
        <div class="avatar-edit-btn" @click="showAvatarPicker = true">
          <el-icon><Camera /></el-icon>
        </div>
      </div>
      <div class="banner-info">
        <h2 class="banner-name">{{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}</h2>
        <div class="banner-meta">
          <span class="banner-username">@{{ userStore.userInfo?.username }}</span>
          <span v-if="userStore.isAdmin()" class="admin-badge">超级管理员</span>
          <el-tag v-else type="info" size="small">普通用户</el-tag>
        </div>
      </div>
    </div>

    <div class="profile-grid">
      <div class="profile-card page-card">
        <div class="card-header">
          <el-icon class="card-header-icon"><User /></el-icon>
          <h3>个人信息</h3>
        </div>
        <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="100px">
          <el-form-item label="用户名">
            <el-input :value="userStore.userInfo?.username" disabled />
          </el-form-item>
          <el-form-item label="角色">
            <span v-if="userStore.isAdmin()" class="admin-badge">超级管理员</span>
            <el-tag v-else type="info" size="small">普通用户</el-tag>
          </el-form-item>
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
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
        <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px">
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

    <!-- 头像设置弹窗 -->
    <el-dialog v-model="showAvatarPicker" title="设置头像" width="640px" class="avatar-picker-dialog">
      <div class="avatar-picker-layout">
        <div class="picker-preview">
          <div class="preview-avatar" :style="getAvatarStyle()">
            <el-icon v-if="selectedAvatarIcon" :size="40"><component :is="selectedAvatarIcon" /></el-icon>
            <span v-else class="preview-letter">{{ (userStore.userInfo?.username || 'U').charAt(0).toUpperCase() }}</span>
          </div>
          <div class="preview-name">{{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}</div>
          <div class="preview-hint">实时预览</div>
        </div>

        <div class="picker-options">
          <el-tabs v-model="avatarTab" class="avatar-tabs">
            <el-tab-pane label="图标" name="icon">
              <div class="icon-grid">
                <button
                  v-for="icon in avatarIcons"
                  :key="icon.name"
                  class="icon-item"
                  :class="{ active: selectedIconName === icon.name }"
                  @click="selectIcon(icon.name)"
                >
                  <el-icon :size="22"><component :is="icon.comp" /></el-icon>
                </button>
              </div>
            </el-tab-pane>
            <el-tab-pane label="背景" name="gradient">
              <div class="gradient-grid">
                <button
                  v-for="(grad, idx) in avatarGradients"
                  :key="idx"
                  class="gradient-item"
                  :class="{ active: selectedGradientIdx === idx }"
                  :style="{ background: grad }"
                  @click="selectGradient(idx)"
                ></button>
              </div>
            </el-tab-pane>
            <el-tab-pane label="上传图片" name="upload">
              <div class="avatar-upload-content">
                <FileUpload
                  :model-value="currentAvatarIsUrl ? (userStore.userInfo?.avatar || '') : ''"
                  biz-type="AVATAR"
                  shape="avatar"
                  hint="支持 jpg / png / webp，不超过 5MB"
                  @change="onAvatarUploaded"
                />
                <p v-if="currentAvatarIsUrl" class="avatar-url-tip">当前使用图片头像，可在「图标」切换回图标。</p>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
      <template #footer>
        <el-button @click="showAvatarPicker = false">取消</el-button>
        <el-button v-if="avatarTab !== 'upload'" @click="resetAvatar">重置</el-button>
        <el-button v-if="avatarTab !== 'upload'" type="primary" class="btn-gradient" :loading="profileLoading" @click="confirmAvatar">保存头像</el-button>
        <el-button v-else @click="showAvatarPicker = false">完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, markRaw } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import {
  User, Lock, Camera,
  UserFilled, Star,
  Coffee, Food,
  Picture, CameraFilled,
  OfficeBuilding, School, Reading,
  Trophy, Medal, Moon,
  MagicStick, Brush,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { updateProfile, changePassword } from '@/api/user'
import FileUpload from '@/components/FileUpload.vue'
import { isAvatarUrl } from '@/utils/avatar'
import type { FileUploadVO } from '@/types/file'

const userStore = useUserStore()
const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const profileLoading = ref(false)
const passwordLoading = ref(false)

const profileForm = reactive({ realName: '', phone: '', email: '', avatar: '' })
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

// 头像选择器
const showAvatarPicker = ref(false)
const avatarTab = ref<'icon' | 'gradient' | 'upload'>('icon')
const selectedIconName = ref<string>('')
const selectedGradientIdx = ref<number>(0)

// 当前头像是否为图片 URL
const currentAvatarIsUrl = computed(() => isAvatarUrl(userStore.userInfo?.avatar))

const avatarIcons = markRaw([
  { name: 'UserFilled', comp: UserFilled },
  { name: 'User', comp: User },
  { name: 'Star', comp: Star },
  { name: 'Coffee', comp: Coffee },
  { name: 'Food', comp: Food },
  { name: 'Picture', comp: Picture },
  { name: 'CameraFilled', comp: CameraFilled },
  { name: 'OfficeBuilding', comp: OfficeBuilding },
  { name: 'School', comp: School },
  { name: 'Reading', comp: Reading },
  { name: 'Trophy', comp: Trophy },
  { name: 'Medal', comp: Medal },
  { name: 'Moon', comp: Moon },
  { name: 'MagicStick', comp: MagicStick },
  { name: 'Brush', comp: Brush },
  { name: 'Lock', comp: Lock },
  { name: 'Camera', comp: Camera },
])

const avatarGradients = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
  'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)',
  'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)',
  'linear-gradient(135deg, #a1c4fd 0%, #c2e9fb 100%)',
  'linear-gradient(135deg, #d299c2 0%, #fef9d7 100%)',
  'linear-gradient(135deg, #89f7fe 0%, #66a6ff 100%)',
  'linear-gradient(135deg, #fddb92 0%, #d1fdff 100%)',
]

const iconMap: Record<string, any> = markRaw(
  Object.fromEntries(avatarIcons.map(i => [i.name, i.comp]))
)

const selectedAvatarIcon = computed(() => {
  const name = selectedIconName.value
  return name ? iconMap[name] : null
})

function parseAvatar(avatarStr: string | undefined) {
  if (!avatarStr) {
    selectedIconName.value = ''
    selectedGradientIdx.value = 0
    return
  }
  try {
    const data = JSON.parse(avatarStr)
    selectedIconName.value = data.icon || ''
    selectedGradientIdx.value = data.gradient ?? 0
  } catch {
    selectedIconName.value = ''
    selectedGradientIdx.value = 0
  }
}

function buildAvatarData(): string {
  return JSON.stringify({
    icon: selectedIconName.value,
    gradient: selectedGradientIdx.value,
  })
}

function getAvatarStyle(): Record<string, string> {
  const gradient = avatarGradients[selectedGradientIdx.value] || avatarGradients[0]
  return {
    background: gradient,
    color: '#fff',
  }
}

function selectIcon(name: string) {
  selectedIconName.value = selectedIconName.value === name ? '' : name
}

function selectGradient(idx: number) {
  selectedGradientIdx.value = idx
}

function resetAvatar() {
  selectedIconName.value = ''
  selectedGradientIdx.value = 0
}

async function confirmAvatar() {
  const avatarData = buildAvatarData()
  profileLoading.value = true
  try {
    await updateProfile({ avatar: avatarData })
    profileForm.avatar = avatarData
    await userStore.fetchUserInfo()
    ElMessage.success('头像更新成功')
    showAvatarPicker.value = false
  } catch { /* */ } finally {
    profileLoading.value = false
  }
}

/** 图片头像上传成功后立即保存为头像 objectKey（非 presigned URL，避免 URL 过期） */
async function onAvatarUploaded(payload: FileUploadVO | null) {
  if (!payload) {
    // 移除图片头像，回退为首字母
    profileLoading.value = true
    try {
      await updateProfile({ avatar: '' })
      profileForm.avatar = ''
      await userStore.fetchUserInfo()
      ElMessage.success('已移除图片头像')
    } catch { /* */ } finally {
      profileLoading.value = false
    }
    return
  }
  profileLoading.value = true
  try {
    // DB 存 objectKey（永久有效），后端读取时动态签发 presigned URL
    await updateProfile({ avatar: payload.objectKey })
    // 表单用 presigned URL 临时显示，fetchUserInfo 后 store 会有新鲜签名 URL
    profileForm.avatar = payload.url
    await userStore.fetchUserInfo()
    ElMessage.success('头像更新成功')
  } catch { /* */ } finally {
    profileLoading.value = false
  }
}

onMounted(async () => {
  await userStore.fetchUserInfo()
  profileForm.realName = userStore.userInfo?.realName || ''
  profileForm.phone = userStore.userInfo?.phone || ''
  profileForm.email = userStore.userInfo?.email || ''
  profileForm.avatar = userStore.userInfo?.avatar || ''
  parseAvatar(userStore.userInfo?.avatar)
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

.banner-avatar-wrapper {
  position: relative;
  flex-shrink: 0;
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
  transition: transform 0.3s ease;
}

.avatar-edit-btn {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 3px solid var(--bg-card);
  transition: all 0.2s ease;
  font-size: 14px;
}
.avatar-edit-btn:hover {
  transform: scale(1.1);
  background: var(--primary-dark);
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

/* 头像设置弹窗 */
.avatar-picker-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
}

.avatar-picker-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.picker-preview {
  flex-shrink: 0;
  width: 168px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  border-right: 1px solid var(--border-light);
}

.preview-avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 600;
  color: #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transition: background 0.2s ease;
  overflow: hidden;
}

.banner-avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.preview-letter {
  font-size: 32px;
  font-weight: 600;
  color: #fff;
}

.preview-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.preview-hint {
  font-size: 12px;
  color: var(--text-muted);
}

.picker-options {
  flex: 1;
  min-width: 0;
}

.avatar-tabs :deep(.el-tabs__content) {
  overflow: visible;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(44px, 1fr));
  gap: 8px;
  max-height: 260px;
  overflow-y: auto;
}

.icon-item {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: var(--bg-page);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
  color: var(--text-secondary);
  border: 2px solid transparent;
  padding: 0;
}
.icon-item:hover {
  background: var(--border-light);
  color: var(--primary);
}
.icon-item.active {
  background: var(--primary-light);
  color: var(--primary);
  border-color: var(--primary);
}

.gradient-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(48px, 1fr));
  gap: 10px;
}

.gradient-item {
  aspect-ratio: 1;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 3px solid transparent;
  box-sizing: border-box;
  padding: 0;
}
.gradient-item:hover {
  transform: scale(1.06);
}
.gradient-item.active {
  border-color: var(--primary);
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.3);
}

.avatar-upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.avatar-url-tip {
  font-size: 12px;
  color: var(--text-muted);
  margin: 0;
  text-align: center;
}
</style>
