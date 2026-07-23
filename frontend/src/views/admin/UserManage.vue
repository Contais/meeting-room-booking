<template>
  <div class="page-view">
    <div class="page-header">
      <h2>用户管理</h2>
    </div>

    <FilterBar :model="query" @search="loadData" @reset="resetQuery">
      <el-form-item label="用户名">
        <el-input v-model="query.keyword" placeholder="请输入用户名" clearable style="width: 200px" @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="请选择状态" clearable filterable style="width: 140px">
          <el-option label="启用" :value="1" /><el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
    </FilterBar>

    <div class="table-card page-card">
      <div class="table-toolbar">
        <div class="table-toolbar-left">
          <el-button class="btn-outline" @click="showCreateDialog"><el-icon><Plus /></el-icon>新增用户</el-button>
        </div>
        <div class="table-toolbar-right">
          <el-tooltip content="刷新"><div class="action-btn" @click="loadData"><el-icon><Refresh /></el-icon></div></el-tooltip>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" row-key="id">
        <el-table-column type="selection" width="40" />
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="username" label="用户名" min-width="140">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:8px">
              <div style="width:32px;height:32px;border-radius:50%;background:var(--primary-light);color:var(--primary);display:flex;align-items:center;justify-content:center;font-size:13px;font-weight:600;flex-shrink:0">{{ row.username?.charAt(0)?.toUpperCase() }}</div>
              <div><div style="font-weight:500">{{ row.username }}</div><div style="font-size:12px;color:var(--text-muted)">{{ row.realName || '-' }}</div></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="role" label="角色" width="90">
          <template #default="{ row }"><el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">{{ row.role === 'admin' ? '管理员' : '用户' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-tooltip content="编辑"><div class="action-btn" @click="showEditDialog(row)"><el-icon><Edit /></el-icon></div></el-tooltip>
            <el-tooltip content="删除"><div class="action-btn danger" @click="handleDelete(row.id)"><el-icon><Delete /></el-icon></div></el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[10, 20, 50]" :total="total" layout="total, sizes, prev, pager, next, jumper" @size-change="loadData" @current-change="loadData" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username"><el-input v-model="form.username" :disabled="isEdit" placeholder="请输入用户名" /></el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password"><el-input v-model="form.password" type="password" placeholder="请输入密码" show-password /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" placeholder="请输入真实姓名" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="请输入手机号" /></el-form-item>
        <el-form-item label="角色" prop="role"><el-select v-model="form.role" placeholder="请选择角色" style="width: 100%" filterable><el-option label="普通用户" value="user" /><el-option label="管理员" value="admin" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" class="btn-gradient" :loading="submitting" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Edit, Delete } from '@element-plus/icons-vue'
import FilterBar from '@/components/FilterBar.vue'
import { listUsers, createUser, updateUser, deleteUser } from '@/api/user'
import type { UserInfo } from '@/types/user'

const loading = ref(false); const submitting = ref(false)
const tableData = ref<UserInfo[]>([]); const total = ref(0)
const dialogVisible = ref(false); const isEdit = ref(false); const formRef = ref<FormInstance>()
const query = reactive({ page: 1, size: 10, keyword: '', status: undefined as number | undefined })
const form = reactive({ id: undefined as number | undefined, username: '', password: '', realName: '', phone: '', role: 'user' })
const rules: FormRules = { username: [{ required: true, message: '请输入用户名', trigger: 'blur' }], password: [{ required: true, message: '请输入密码', trigger: 'blur' }], role: [{ required: true, message: '请选择角色', trigger: 'change' }] }

async function loadData() { loading.value = true; try { const res = await listUsers(query); tableData.value = res.data.records; total.value = res.data.total } catch { /* */ } finally { loading.value = false } }
function resetQuery() { query.keyword = ''; query.status = undefined; query.page = 1; loadData() }
function showCreateDialog() { isEdit.value = false; Object.assign(form, { id: undefined, username: '', password: '', realName: '', phone: '', role: 'user' }); dialogVisible.value = true }
function showEditDialog(row: UserInfo) { isEdit.value = true; Object.assign(form, { id: row.id, username: row.username, password: '', realName: row.realName || '', phone: row.phone || '', role: row.role }); dialogVisible.value = true }
async function handleSubmit() { const valid = await formRef.value?.validate().catch(() => false); if (!valid) return; submitting.value = true; try { if (isEdit.value) { await updateUser(form); ElMessage.success('更新成功') } else { await createUser(form); ElMessage.success('创建成功') }; dialogVisible.value = false; loadData() } catch { /* */ } finally { submitting.value = false } }
async function handleDelete(id: number) { try { await ElMessageBox.confirm('确定删除该用户?', '提示', { type: 'warning' }); await deleteUser(id); ElMessage.success('删除成功'); loadData() } catch { /* */ } }
onMounted(loadData)
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }
.table-footer { display: flex; justify-content: flex-end; padding: 14px 20px; border-top: 1px solid var(--border-light); }
</style>
