<template>
  <div class="page-view">
    <div class="page-header">
      <div class="header-left">
        <el-button class="back-btn" @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </el-button>
      </div>
      <div v-if="user" class="header-actions">
        <el-button :type="user.status === 1 ? 'warning' : 'success'" plain @click="handleToggleStatus">
          <el-icon><component :is="user.status === 1 ? 'Warning' : 'CircleCheck'" /></el-icon>
          <span>{{ user.status === 1 ? '禁用' : '启用' }}</span>
        </el-button>
        <el-button type="primary" plain @click="openEditDialog">
          <el-icon><Edit /></el-icon>
          <span>编辑</span>
        </el-button>
      </div>
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

    <FormDrawer v-model:visible="editDialogVisible" title="编辑用户" :loading="submitting" @submit="handleSubmit">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username"><el-input v-model="form.username" disabled /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" placeholder="请输入真实姓名" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="请输入手机号" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" placeholder="请输入邮箱" /></el-form-item>
        <el-form-item label="角色" prop="role"><el-select v-model="form.role" placeholder="请选择角色" style="width:100%" filterable><el-option label="普通用户" value="user" /><el-option label="管理员" value="admin" /></el-select></el-form-item>
        <el-form-item label="所属部门"><el-tree-select v-model="form.departmentId" :data="deptTree" :props="{ label: 'name', value: 'id', children: 'children' }" check-strictly clearable placeholder="请选择部门" style="width:100%" /></el-form-item>
      </el-form>
    </FormDrawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Edit } from '@element-plus/icons-vue'
import { getUserDetail, updateUser, toggleUserStatus } from '@/api/user'
import { getDepartmentTree } from '@/api/department'
import FormDrawer from '@/components/FormDrawer.vue'
import { formatDateTime } from '@/utils/datetime'
import type { UserInfo } from '@/types/user'
import type { Department } from '@/types/department'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const user = ref<UserInfo | null>(null)
const id = Number(route.params.id)

// 编辑弹窗
const editDialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const deptTree = ref<Department[]>([])
const form = reactive({ id: undefined as number | undefined, username: '', realName: '', phone: '', email: '', role: 'user', departmentId: undefined as number | undefined })
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

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

function openEditDialog() {
  if (!user.value) return
  Object.assign(form, {
    id: user.value.id,
    username: user.value.username || '',
    realName: user.value.realName || '',
    phone: user.value.phone || '',
    email: user.value.email || '',
    role: user.value.role || 'user',
    departmentId: user.value.departmentId || undefined
  })
  editDialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await updateUser(form)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    loadDetail()
  } catch { /* */ } finally {
    submitting.value = false
  }
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

async function loadDeptTree() {
  try { const res = await getDepartmentTree(); deptTree.value = res.data } catch { /* */ }
}

onMounted(() => { loadDetail(); loadDeptTree() })
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
}

.back-btn {
  /* 完全使用 Element Plus 默认样式 */
}

.header-actions {
  display: flex;
  gap: 2px;
}

.header-actions .el-button {
  font-weight: 500;
  transition: all 0.2s ease;
}
.header-actions .el-button:hover {
  transform: translateY(-1px);
}

.detail-card {
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border-light);
  padding: 24px;
}

.user-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-light);
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
  color: var(--text-primary);
  margin: 0 0 4px 0;
}

.user-info p {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0 0 8px 0;
}

.mt-20 {
  margin-top: 20px;
}
</style>
