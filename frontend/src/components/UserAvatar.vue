<template>
  <div class="user-avatar" :class="`avatar-${size}`" :style="avatarStyle">
    <img v-if="isUrl" :src="avatar || ''" class="user-avatar-img" alt="头像" />
    <el-icon v-else-if="iconComponent" :size="iconSize"><component :is="iconComponent" /></el-icon>
    <template v-else>{{ initial }}</template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { isAvatarUrl } from '@/utils/avatar'

/**
 * 用户头像组件：三态渲染
 *  1. 图片 URL（http(s):// 或 /api/ 开头，文件上传后的签名地址）→ <img>
 *  2. 图标 JSON（{ icon, gradient } 旧格式）→ el-icon + 渐变背景
 *  3. 兜底 → 用户名首字母 + 渐变背景
 */
const props = withDefaults(defineProps<{
  avatar?: string | null
  username?: string | null
  size?: 'sm' | 'md' | 'lg'
}>(), { size: 'md', username: '', avatar: '' })

const iconComponents: Record<string, any> = Object.fromEntries(
  Object.entries(ElementPlusIconsVue).map(([key, component]) => [key, component])
)

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

const avatarData = computed(() => {
  if (!props.avatar) return { icon: '', gradient: 0 }
  try {
    const data = JSON.parse(props.avatar)
    return { icon: data.icon || '', gradient: data.gradient ?? 0 }
  } catch {
    return { icon: '', gradient: 0 }
  }
})

const isUrl = computed(() => isAvatarUrl(props.avatar))
const iconComponent = computed(() => {
  const name = avatarData.value.icon
  return name ? iconComponents[name] || null : null
})
const initial = computed(() => (props.username || 'U').charAt(0).toUpperCase())
const avatarStyle = computed(() => ({
  background: avatarGradients[avatarData.value.gradient % avatarGradients.length] || avatarGradients[0],
  color: '#fff',
}))
const iconSize = computed(() => (props.size === 'sm' ? 16 : props.size === 'lg' ? 32 : 20))
</script>

<style scoped>
.user-avatar {
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  flex-shrink: 0;
  overflow: hidden;
}
.avatar-sm { width: 32px; height: 32px; font-size: 13px; }
.avatar-md { width: 40px; height: 40px; font-size: 15px; }
.avatar-lg { width: 64px; height: 64px; font-size: 24px; }
.user-avatar-img { width: 100%; height: 100%; object-fit: cover; }
</style>
