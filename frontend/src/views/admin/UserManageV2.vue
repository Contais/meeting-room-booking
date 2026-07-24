<template>
  <div class="page-view">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="search-fields">
        <div class="search-item">
          <label>用户名</label>
          <el-input v-model="query.username" placeholder="请输入用户名" clearable />
        </div>
        <div class="search-item">
          <label>手机号</label>
          <el-input v-model="query.phone" placeholder="请输入手机号" clearable />
        </div>
        <div class="search-item">
          <label>邮箱</label>
          <el-input v-model="query.email" placeholder="请输入邮箱" clearable />
        </div>
      </div>
      <div class="search-actions">
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button link type="primary" @click="expanded = !expanded">
          展开 <el-icon><ArrowDown v-if="!expanded" /><ArrowUp v-else /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 表格卡片 -->
    <div class="table-card">
      <!-- 工具栏 -->
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-button class="btn-outline" @click="showCreateDialog"><el-icon><Plus /></el-icon>新增用户</el-button>
        </div>
        <div class="toolbar-right">
          <el-tooltip content="刷新"><el-button circle @click="loadData"><el-icon><Refresh /></el-icon></el-button></el-tooltip>
          <el-tooltip content="排序"><el-button circle><el-icon><Sort /></el-icon></el-button></el-tooltip>
          <el-tooltip content="全屏"><el-button circle><el-icon><FullScreen /></el-icon></el-button></el-tooltip>
          <el-tooltip content="列设置"><el-button circle><el-icon><Setting /></el-icon></el-button></el-tooltip>
        </div>
      </div>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" stripe row-key="id"
        @selection-change="handleSelectionChange"
        :header-cell-style="{ background: '#fafbfc', color: '#606266', fontWeight: 500 }">
        <el-table-column type="selection" width="40" />
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column label="用户名" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-avatar">{{ (row.username || 'U').charAt(0).toUpperCase() }}</div>
              <div class="user-info">
                <span class="user-name">{{ row.username }}</span>
                <span class="user-email">{{ row.phone || '-' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="性别" width="80" align="center">
          <template #default="{ row }">
            <span>{{ row.role === 'admin' ? '男' : '女' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" size="small" effect="light" round>在线</el-tag>
            <el-tag v-else-if="row.status === 0" type="info" size="small" effect="light" round>异常</el-tag>
            <el-tag v-else type="danger" size="small" effect="light" round>注销</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建日期" min-width="170" sortable />
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link @click="showEditDialog(row)"><el-icon><Edit /></el-icon></el-button>
              <el-button type="danger" link @click="handleDelete(row.id)"><el-icon><Delete /></el-icon></el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <span class="total-text">共 {{ total }} 条</span>
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="prev, pager, next, sizes, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username"><el-input v-model="form.username" :disabled="isEdit" placeholder="请输入用户名" /></el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password"><el-input v-model="form.password" type="password" placeholder="请输入密码" show-password /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" placeholder="请输入真实姓名" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="请输入手机号" /></el-form-item>
        <el-form-item label="角色" prop="role"><el-select v-model="form.role" placeholder="请选择角色" style="width:100%" filterable><el-option label="普通用户" value="user" /><el-option label="管理员" value="admin" /></el-select></el-form-item>
        <el-form-item label="所属部门"><el-tree-select v-model="form.departmentId" :data="deptTree" :props="{ label: 'name', value: 'id', children: 'children' }" check-strictly clearable placeholder="请选择部门" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Refresh, Sort, FullScreen, Setting, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { listUsers, createUser, updateUser, deleteUser } from '@/api/user'
import { getDepartmentTree } from '@/api/department'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const expanded = ref(false)
const deptTree = ref([])
const selectedRows = ref([])

const query = reactive({ page: 1, size: 20, username: '', phone: '', email: '', status: undefined })
const form = reactive({ id: undefined, username: '', password: '', realName: '', phone: '', role: 'user', departmentId: undefined })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await listUsers(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch { /* */ } finally { loading.value = false }
}

function resetQuery() {
  query.username = ''
  query.phone = ''
  query.email = ''
  query.status = undefined
  query.page = 1
  loadData()
}

function showCreateDialog() {
  isEdit.value = false
  Object.assign(form, { id: undefined, username: '', password: '', realName: '', phone: '', role: 'user', departmentId: undefined })
  dialogVisible.value = true
}

function showEditDialog(row) {
  isEdit.value = true
  Object.assign(form, { id: row.id, username: row.username, password: '', realName: row.realName || '', phone: row.phone || '', role: row.role, departmentId: row.departmentId || undefined })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) { await updateUser(form); ElMessage.success('更新成功') }
    else { await createUser(form); ElMessage.success('创建成功') }
    dialogVisible.value = false
    loadData()
  } catch { /* */ } finally { submitting.value = false }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该用户?', '提示', { type: 'warning' })
    await deleteUser(id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* */ }
}

function handleSelectionChange(val) { selectedRows.value = val }

async function loadDeptTree() {
  try { const res = await getDepartmentTree(); deptTree.value = res.data } catch { /* */ }
}

onMounted(() => { loadData(); loadDeptTree() })
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }

/* 搜索栏 */
.search-bar {
  background: #fff; border-radius: 12px; padding: 20px 24px;
  display: flex; align-items: flex-end; justify-content: space-between;
  border: 1px solid #f0f0f0;
}
.search-fields { display: flex; gap: 24px; flex: 1; }
.search-item { display: flex; flex-direction: column; gap: 6px; }
.search-item label { font-size: 13px; color: #606266; font-weight: 500; }
.search-item :deep(.el-input) { width: 200px; }
.search-actions { display: flex; gap: 8px; align-items: center; }

/* 表格卡片 */
.table-card {
  background: #fff; border-radius: 12px; border: 1px solid #f0f0f0; overflow: hidden;
}

/* 工具栏 */
.table-toolbar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px; border-bottom: 1px solid #f5f5f5;
}
.toolbar-right { display: flex; gap: 4px; }

/* 用户单元格 */
.user-cell { display: flex; align-items: center; gap: 10px; }
.user-avatar {
  width: 36px; height: 36px; border-radius: 8px; background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600; flex-shrink: 0;
}
.user-info { display: flex; flex-direction: column; }
.user-name { font-size: 13px; font-weight: 500; color: #303133; }
.user-email { font-size: 11px; color: #909399; }

/* 操作按钮 */
.action-buttons { display: flex; justify-content: center; gap: 8px; }

/* 分页 */
.pagination-wrap {
  display: flex; align-items: center; justify-content: flex-end; gap: 16px;
  padding: 14px 20px; border-top: 1px solid #f5f5f5;
}
.total-text { font-size: 13px; color: #909399; }
</style>
