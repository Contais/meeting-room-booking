<template>
  <div class="page-view">
    <SearchBar @search="onFilterChange" @reset="resetQuery">
      <template #collapsed>
        <el-input v-model="query.keyword" placeholder="搜索用户名 / 姓名" clearable @input="onSearchInput" @keyup.enter="onSearchInput" />
      </template>
      <template #expanded>
        <div class="search-item"><label>用户名</label><el-input v-model="query.username" placeholder="请输入" clearable @input="onSearchInput" @keyup.enter="onSearchInput" /></div>
        <div class="search-item"><label>手机号</label><el-input v-model="query.phone" placeholder="请输入" clearable @input="onSearchInput" @keyup.enter="onSearchInput" /></div>
        <div class="search-item"><label>状态</label><el-select v-model="query.status" placeholder="全部" clearable @change="onFilterChange"><el-option label="启用" :value="1" /><el-option label="禁用" :value="0" /></el-select></div>
        <div class="search-item is-wide"><label>创建时间</label><el-date-picker v-model="createTimeRange" type="datetimerange" range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" value-format="YYYY-MM-DDTHH:mm:ss" @change="onCreateTimeRangeChange" /></div>
      </template>
    </SearchBar>
    <div ref="tableCardRef" class="table-card-fullscreen">
      <TableCard :total="total" v-model:page="query.page" v-model:size="query.size" @size-change="onSizeChange" @current-change="loadData">
      <template #toolbar-left><el-button class="btn-outline" @click="showCreateDialog"><el-icon><Plus /></el-icon>新增用户</el-button></template>
      <template #toolbar-right>
        <TableToolbarActions :fullscreen-target="tableCardRef" v-model:sort-order="sortOrder" :columns="columnOptions" v-model:visible-columns="visibleColumns" @refresh="loadData" />
      </template>
      <el-table :data="displayData" v-loading="loading" empty-text="暂无用户数据，点击左上角「新增用户」创建">
        <el-table-column type="index" :index="(index: number) => (query.page - 1) * query.size + index + 1" label="序号" width="70" align="center" />
        <el-table-column v-if="isColumnVisible('username')" label="用户名" min-width="200">
          <template #default="{ row }"><div class="user-cell"><UserAvatar :avatar="row.avatar" :username="row.username" size="sm" /><div class="user-info"><span class="user-name">{{ row.username }}</span><span class="user-email">{{ row.email || row.phone || '-' }}</span></div></div></template>
        </el-table-column>
        <el-table-column v-if="isColumnVisible('realName')" prop="realName" label="姓名" min-width="90" />
        <el-table-column v-if="isColumnVisible('phone')" prop="phone" label="手机号" min-width="130" />
        <!-- <el-table-column prop="email" label="邮箱" min-width="180" /> -->
        <el-table-column v-if="isColumnVisible('role')" label="角色" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ROLE_ADMIN' ? 'danger' : 'info'" size="small" effect="light" round>
              {{ getRoleName(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isColumnVisible('status')" label="状态" width="90" align="center"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small" effect="light" round>{{ row.status === 1 ? '启用' : '禁用' }}</el-tag></template></el-table-column>
        <el-table-column v-if="isColumnVisible('createTime')" label="创建时间" width="170"><template #default="{ row }">{{ formatDateTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-links">
              <el-button type="primary" link @click="router.push({ path: `/admin/users/${row.id}`, query: { from: '/admin/users', fromTitle: '用户管理', dt: '用户详情' } })">
                <el-icon><View /></el-icon>详情
              </el-button>
              <el-button type="primary" link @click="showEditDialog(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
              <el-dropdown trigger="click" popper-class="action-menu-popper" @command="(cmd: string) => handleRowCommand(cmd, row)">
                <el-button type="primary" link>
                  更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="reset">
                      <el-icon><Key /></el-icon>重置密码
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided class="danger-item">
                      <el-icon><Delete /></el-icon>删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
      </TableCard>
    </div>
    <FormDrawer v-model:visible="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" :loading="submitting" @submit="handleSubmit">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="form-standard">
        <el-form-item label="用户名" prop="username"><el-input v-model="form.username" :disabled="isEdit" placeholder="请输入用户名" /></el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password"><el-input v-model="form.password" type="password" placeholder="请输入密码" show-password /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" placeholder="请输入真实姓名" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="请输入手机号" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" placeholder="请输入邮箱" /></el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width:100%" filterable>
            <el-option v-for="r in roleList" :key="r.roleCode" :label="r.roleName" :value="r.roleCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属部门"><el-tree-select v-model="form.departmentId" :data="deptTree" node-key="id" :props="{ label: 'name', children: 'children' }" check-strictly clearable placeholder="请选择部门" style="width:100%" /></el-form-item>
      </el-form>
    </FormDrawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Key, View, ArrowDown } from '@element-plus/icons-vue'
import { listUsers, createUser, updateUser, deleteUser, resetPassword } from '@/api/user'
import { getDepartmentTree } from '@/api/department'
import { listAllRoles } from '@/api/role'
import SearchBar from '@/components/SearchBar.vue'
import TableCard from '@/components/TableCard.vue'
import TableToolbarActions from '@/components/TableToolbarActions.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { sortByProperty } from '@/utils/table'
import { useTableToolbar } from '@/composables/useTableToolbar'
import { formatDateTime } from '@/utils/datetime'
import type { TableColumnOption } from '@/types/table'
import type { Department } from '@/types/department'

const router = useRouter()
const loading = ref(false); const submitting = ref(false)
const tableData = ref<any[]>([]); const total = ref(0)
const tableCardRef = ref<HTMLDivElement>()
const columnOptions: TableColumnOption[] = [
  { key: 'username', label: '用户名' },
  { key: 'realName', label: '姓名' },
  { key: 'phone', label: '手机号' },
  { key: 'role', label: '角色' },
  { key: 'status', label: '状态' },
  { key: 'createTime', label: '创建时间' }
]
const { sortOrder, visibleColumns, isColumnVisible } = useTableToolbar(columnOptions)
const displayData = computed(() => sortByProperty(tableData.value, sortOrder.value, (row) => row.createTime || ''))
const dialogVisible = ref(false); const isEdit = ref(false); const formRef = ref<FormInstance>()
const deptTree = ref<Department[]>([])
const roleList = ref<Array<{ roleCode: string; roleName: string }>>([])
const query = reactive({ page: 1, size: 10, keyword: '', username: '', phone: '', status: undefined as number | undefined, createTimeStart: '', createTimeEnd: '' })
const createTimeRange = ref<string[]>([])
const form = reactive({ id: undefined as string | undefined, username: '', password: '', realName: '', phone: '', email: '', role: 'ROLE_USER', departmentId: undefined as string | undefined })
const rules: FormRules = { username: [{ required: true, message: '请输入用户名', trigger: 'blur' }], password: [{ required: true, message: '请输入密码', trigger: 'blur' }], role: [{ required: true, message: '请选择角色', trigger: 'change' }] }

function onSizeChange() { query.page = 1; loadData() }
function onFilterChange() { query.page = 1; loadData() }
function onCreateTimeRangeChange(val: string[] | null) {
  query.createTimeStart = val && val.length === 2 ? val[0] : ''
  query.createTimeEnd = val && val.length === 2 ? val[1] : ''
  onFilterChange()
}
async function loadData() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.username) params.username = query.username
    if (query.phone) params.phone = query.phone
    if (query.status != null) params.status = query.status
    if (query.createTimeStart) params.createTimeStart = query.createTimeStart
    if (query.createTimeEnd) params.createTimeEnd = query.createTimeEnd
    const res = await listUsers(params); tableData.value = res.data.records; total.value = Number(res.data.total) || 0
  } catch { /* */ } finally { loading.value = false }
}
let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() { if (searchTimer) clearTimeout(searchTimer); searchTimer = setTimeout(() => { query.page = 1; loadData() }, 300) }

function resetQuery() { query.keyword = ''; query.username = ''; query.phone = ''; query.status = undefined; query.createTimeStart = ''; query.createTimeEnd = ''; query.page = 1; createTimeRange.value = []; loadData() }
function showCreateDialog() { isEdit.value = false; Object.assign(form, { id: undefined, username: '', password: '', realName: '', phone: '', email: '', role: 'ROLE_USER', departmentId: undefined }); dialogVisible.value = true }
function showEditDialog(row: any) { isEdit.value = true; Object.assign(form, { id: row.id, username: row.username, password: '', realName: row.realName || '', phone: row.phone || '', email: row.email || '', role: row.role, departmentId: row.departmentId || undefined }); dialogVisible.value = true }
async function handleSubmit() { const valid = await formRef.value?.validate().catch(() => false); if (!valid) return; submitting.value = true; try { if (isEdit.value) { const { id, phone, email, realName, role, departmentId } = form; await updateUser({ id, phone, email, realName, role, departmentId }); ElMessage.success('更新成功') } else { await createUser(form); ElMessage.success('创建成功') }; dialogVisible.value = false; loadData() } catch { /* */ } finally { submitting.value = false } }
async function handleDelete(id: string) { try { await ElMessageBox.confirm('确定删除该用户?', '提示', { type: 'warning' }); await deleteUser(id); ElMessage.success('删除成功'); loadData() } catch { /* */ } }
function handleRowCommand(command: string, row: any) {
  if (command === 'reset') handleResetPassword(row)
  else if (command === 'delete') handleDelete(row.id)
}
async function handleResetPassword(row: any) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新密码', '重置密码', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'password',
      inputValidator: (val) => val && val.length >= 6 ? true : '密码长度至少6位',
      inputPlaceholder: '请输入新密码'
    })
    await resetPassword(row.id, value)
    ElMessage.success('密码重置成功')
  } catch { /* */ }
}
async function loadDeptTree() { try { const res = await getDepartmentTree(); deptTree.value = res.data } catch { /* */ } }
async function loadRoleList() { try { const res = await listAllRoles(); roleList.value = res.data } catch { /* */ } }
function getRoleName(roleCode: string): string {
  const role = roleList.value.find(r => r.roleCode === roleCode)
  return role ? role.roleName : roleCode
}
onMounted(() => { loadData(); loadDeptTree(); loadRoleList() })
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }
.user-cell { display: flex; align-items: center; gap: 10px; }
.user-info { display: flex; flex-direction: column; }
.user-name { font-size: 13px; font-weight: 500; color: var(--text-primary); }
.user-email { font-size: 11px; color: var(--text-muted); }
</style>
