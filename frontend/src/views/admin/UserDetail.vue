<template>
  <div class="page-view">
    <div class="page-header">
      <h2>用户详情</h2>
      <el-button @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
    </div>

    <div v-loading="loading" class="detail-card">
      <div v-if="user" class="user-header">
        <div class="avatar-large">
          {{ (user.username || 'U').charAt(0).toUpperCase() }}
        </div>
        <div class="user-info">
          <h3>{{ user.realName || user.username }}</h3>
          <p>{{ user.username }}</p>
          <el-tag :type="user.status === 1 ? 'success' : 'danger'" size="small">
            {{ user.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </div>
      </div>

      <el-descriptions v-if="user" :column="2" border class="mt-20">
        <el-descriptions-item label="用户ID">
          {{ user.id }}
        </el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag :type="user.role === 'admin' ? 'danger' : 'info'" size="small">
            {{ user.role === 'admin' ? '管理员' : '普通用户' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="姓名">
          {{ user.realName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="手机号">
          {{ user.phone || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="邮箱">
          {{ user.email || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="部门">
          {{ user.departmentName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatDateTime(user.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ formatDateTime(user.updateTime) }}
        </el-descriptions-item>
      </el-descriptions>

      <el-empty v-else description="暂无数据" />
    </div>

    <div v-if="user" class="action-bar">
      <el-button type="primary" @click="handleEdit">
        <el-icon><Edit /></el-icon>
        编辑
      </el-button>
      <el-button type="danger" @click="handleToggleStatus">
        {{ user.status === 1 ? '禁用' : '启用' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Edit } from '@element-plus/icons-vue'
import { getUserDetail, toggleUserStatus } from '@/api/user'
import { formatDateTime } from '@/utils/datetime'
import type { UserInfo } from '@/types/user'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const user = ref<UserInfo | null>(null)

const id = Number(route.params.id)

async function loadDetail() {
  loading.value = true
  try {
    const res = await getUserDetail(id)
    user.value = res.data
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function handleEdit() {
  router.push({ path: '/admin/users', query: { edit: id } })
}

async function handleToggleStatus() {
  const action = user.value?.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${action}该用户？`, '提示', { type: 'warning' })
    await toggleUserStatus(id)
    ElMessage.success(`${action}成功`)
    loadDetail()
  } catch {
    // 用户取消
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.detail-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  padding: 24px;
}

.user-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.avatar-large {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-info h3 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
}

.user-info p {
  font-size: 14px;
  color: #909399;
  margin: 0 0 8px 0;
}

.mt-20 {
  margin-top: 20px;
}

.action-bar {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}
</style>
