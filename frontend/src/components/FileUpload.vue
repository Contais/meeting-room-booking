<template>
  <div class="file-upload" :class="`shape-${shape}`">
    <!-- 已有图片：预览 -->
    <div v-if="modelValue" class="preview-wrapper">
      <img :src="modelValue" class="preview-img" :class="`shape-${shape}`" alt="预览" />
      <div class="preview-mask">
        <el-tooltip content="重新上传" placement="top">
          <button type="button" class="mask-btn" @click="triggerPick" :disabled="disabled">
            <el-icon><Refresh /></el-icon>
          </button>
        </el-tooltip>
        <el-tooltip content="移除" placement="top">
          <button type="button" class="mask-btn danger" @click="handleRemove" :disabled="disabled">
            <el-icon><Delete /></el-icon>
          </button>
        </el-tooltip>
      </div>
    </div>

    <!-- 无图片：上传触发区 -->
    <el-upload
      v-else
      class="upload-trigger"
      :show-file-list="false"
      :accept="accept"
      :disabled="loading || disabled"
      :before-upload="beforeUpload"
      :http-request="customUpload"
    >
      <div class="trigger-inner" :class="{ loading }">
        <template v-if="loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>上传中...</span>
        </template>
        <template v-else>
          <el-icon><Plus /></el-icon>
          <span>{{ shape === 'avatar' ? '上传头像' : '上传图片' }}</span>
        </template>
      </div>
    </el-upload>

    <div v-if="hint" class="upload-hint">{{ hint }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import { Plus, Delete, Refresh, Loading } from '@element-plus/icons-vue'
import { uploadFile, deleteFile } from '@/api/file'
import type { FileBizType, FileUploadVO } from '@/types/file'

interface Props {
  /** 当前图片 URL（v-model） */
  modelValue?: string
  /** 业务类型 */
  bizType: FileBizType
  /** 展示形态：avatar 圆形 / card 矩形 */
  shape?: 'avatar' | 'card'
  /** 接受的文件类型 */
  accept?: string
  /** 最大体积（MB） */
  maxSize?: number
  /** 是否禁用 */
  disabled?: boolean
  /** 底部提示文案 */
  hint?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  shape: 'card',
  accept: 'image/jpeg,image/png,image/webp',
  maxSize: 5,
  disabled: false,
  hint: '',
})

const emit = defineEmits<{
  (e: 'update:modelValue', url: string): void
  (e: 'change', payload: FileUploadVO | null): void
}>()

const loading = ref(false)
const lastObjectKey = ref<string>('')

function triggerPick() {
  // 通过隐藏 input 触发选择：复用 el-upload 的预览态下的"重新上传"
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = props.accept
  input.onchange = () => {
    const file = input.files?.[0]
    if (file) {
      doUpload(file)
    }
  }
  input.click()
}

function beforeUpload(file: File): boolean {
  if (!props.accept.split(',').some(t => file.type === t.trim())) {
    ElMessage.error('仅支持 jpg/png/webp 格式')
    return false
  }
  if (file.size > props.maxSize * 1024 * 1024) {
    ElMessage.error(`文件不能超过 ${props.maxSize}MB`)
    return false
  }
  return true
}

async function customUpload(options: UploadRequestOptions) {
  await doUpload(options.file as File)
}

async function doUpload(file: File) {
  loading.value = true
  try {
    const res = await uploadFile(file, props.bizType)
    const data = res.data
    lastObjectKey.value = data.objectKey
    emit('update:modelValue', data.url)
    emit('change', data)
    ElMessage.success('上传成功')
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

async function handleRemove() {
  // 仅删除本次会话上传的文件，避免误删外部既有图片
  if (lastObjectKey.value) {
    try {
      await deleteFile(lastObjectKey.value)
    } catch { /* 忽略 */ }
    lastObjectKey.value = ''
  }
  emit('update:modelValue', '')
  emit('change', null)
}
</script>

<style scoped>
.file-upload {
  display: inline-flex;
  flex-direction: column;
  gap: 6px;
}

/* 卡片形态 */
.file-upload.shape-card .preview-wrapper,
.file-upload.shape-card .upload-trigger {
  width: 200px;
}

.file-upload.shape-card :deep(.el-upload) {
  width: 100%;
}

.file-upload.shape-card .preview-img,
.file-upload.shape-card .trigger-inner {
  width: 200px;
  height: 130px;
  border-radius: 10px;
}

/* 头像形态 */
.file-upload.shape-avatar .preview-img,
.file-upload.shape-avatar .trigger-inner {
  width: 96px;
  height: 96px;
  border-radius: 50%;
}

.preview-wrapper {
  position: relative;
  display: inline-block;
  overflow: hidden;
}

.preview-img {
  object-fit: cover;
  display: block;
  background: var(--bg-page);
}

.upload-trigger {
  display: block;
}

.trigger-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px dashed var(--border-light);
  background: var(--bg-page);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
}

.trigger-inner:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: rgba(102, 126, 234, 0.06);
}

.trigger-inner.loading {
  cursor: progress;
  color: var(--primary);
  border-color: var(--primary);
}

.trigger-inner .el-icon {
  font-size: 22px;
}

.preview-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(0, 0, 0, 0.5);
  opacity: 0;
  transition: opacity 0.2s;
}

.preview-wrapper:hover .preview-mask {
  opacity: 1;
}

.mask-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.9);
  color: #333;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.mask-btn:hover {
  background: #fff;
  transform: scale(1.1);
}

.mask-btn.danger:hover {
  background: #fef0f0;
  color: #f56c6c;
}

.mask-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.upload-hint {
  font-size: 12px;
  color: var(--text-muted);
}
</style>
