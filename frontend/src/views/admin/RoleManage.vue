<template>
  <div class="page-view">
    <SearchBar @search="onFilterChange" @reset="resetQuery">
      <template #collapsed>
        <el-input v-model="query.keyword" placeholder="搜索角色名称 / 编码" clearable @input="onSearchInput" @keyup.enter="onSearchInput" />
      </template>
      <template #expanded>
        <div class="search-item"><label>角色名称</label><el-input v-model="query.roleName" placeholder="请输入" clearable @input="onSearchInput" @keyup.enter="onSearchInput" /></div>
        <div class="search-item"><label>状态</label><el-select v-model="query.status" placeholder="全部" clearable @change="onFilterChange"><el-option label="启用" :value="1" /><el-option label="禁用" :value="0" /></el-select></div>
        <div class="search-item"><label>类型</label><el-select v-model="query.isSystem" placeholder="全部" clearable @change="onFilterChange"><el-option label="系统角色" :value="1" /><el-option label="自定义" :value="0" /></el-select></div>
      </template>
    </SearchBar>

    <div ref="tableCardRef" class="table-card-fullscreen">
      <TableCard
        :total="total"
        v-model:page="query.pageNum"
        v-model:size="query.pageSize"
        @size-change="onSizeChange"
        @current-change="loadRoles"
      >
      <template #toolbar-left>
        <el-button class="btn-outline" @click="handleCreate"><el-icon><Plus /></el-icon>新建角色</el-button>
      </template>
      <template #toolbar-right>
        <TableToolbarActions :fullscreen-target="tableCardRef" v-model:sort-order="sortOrder" :columns="columnOptions" v-model:visible-columns="visibleColumns" @refresh="loadRoles" />
      </template>

      <el-table :data="displayData" v-loading="loading" empty-text="暂无角色数据，点击左上角「新建角色」创建">
        <el-table-column type="index" :index="(index: number) => (query.pageNum - 1) * query.pageSize + index + 1" label="序号" width="70" align="center" />
        <el-table-column v-if="isColumnVisible('roleCode')" prop="roleCode" label="角色编码" width="120" />
        <el-table-column v-if="isColumnVisible('roleName')" label="角色名称" width="140">
          <template #default="{ row }">
            <span class="role-name-cell">
              <span class="role-dot" :style="{ background: getRoleColor(row.id) }"></span>
              {{ row.roleName }}
            </span>
          </template>
        </el-table-column>
        <el-table-column v-if="isColumnVisible('description')" prop="description" label="描述" min-width="150" show-overflow-tooltip />
        <el-table-column v-if="isColumnVisible('sort')" prop="sort" label="排序" width="70" align="center" />
        <el-table-column v-if="isColumnVisible('status')" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small" effect="light" round>
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isColumnVisible('type')" label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isSystem === 1" type="danger" size="small" effect="light" round>系统角色</el-tag>
            <el-tag v-else type="info" size="small" effect="light" round>自定义</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isColumnVisible('createTime')" label="创建时间" width="170"><template #default="{ row }">{{ formatDateTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-links">
              <el-button type="primary" link @click="handlePermission(row)">
                <el-icon><Key /></el-icon>
                权限配置
              </el-button>
              <el-button type="primary" link :disabled="row.isSystem === 1" @click="handleEdit(row)">
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
              <el-dropdown trigger="click" popper-class="action-menu-popper" @command="(cmd: string) => handleRowCommand(cmd, row)">
                <el-button type="primary" link>
                  更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="toggle" :disabled="row.isSystem === 1">
                      <el-icon><Switch /></el-icon>{{ row.status === 1 ? '禁用' : '启用' }}
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided class="danger-item" :disabled="row.isSystem === 1">
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

    <!-- 新建/编辑角色抽屉 -->
    <FormDrawer v-model:visible="dialogVisible" :title="isEdit ? '编辑角色' : '新建角色'" :loading="submitting" @submit="handleSubmit">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-position="top" class="form-standard">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="formData.roleCode" placeholder="请输入角色编码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="formData.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入角色描述" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" :max="999" />
        </el-form-item>
      </el-form>
    </FormDrawer>

    <!-- 权限配置对话框（角色管理独有，保留 el-dialog） -->
    <el-dialog v-model="permDialogVisible" title="权限配置" width="600px" :close-on-click-modal="false">
      <div class="perm-dialog-header">
        <span>角色：<strong>{{ currentRole?.roleName }}</strong></span>
        <el-tag type="info" effect="light">勾选菜单分配权限</el-tag>
      </div>
      <div class="perm-tree-wrapper">
        <el-tree
          ref="menuTreeRef"
          :data="menuTree"
          :props="{ label: 'name', children: 'children' }"
          node-key="id"
          show-checkbox
          default-expand-all
          :check-strictly="false"
        />
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePermissionSubmit">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Edit, Delete, Key, ArrowDown, Switch } from '@element-plus/icons-vue'
import {
  listRoles,
  createRole,
  updateRole,
  deleteRole,
  toggleRoleStatus,
  getRoleMenuIds,
  assignRoleMenus,
  type RoleInfo,
  type RolePageQuery,
} from '@/api/role'
import { getMenuTree } from '@/api/menu'
import type { MenuItem } from '@/types/menu'
import SearchBar from '@/components/SearchBar.vue'
import TableCard from '@/components/TableCard.vue'
import TableToolbarActions from '@/components/TableToolbarActions.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import { sortByProperty } from '@/utils/table'
import { useTableToolbar } from '@/composables/useTableToolbar'
import { formatDateTime } from '@/utils/datetime'
import type { TableColumnOption } from '@/types/table'

const loading = ref(false)
const submitting = ref(false)
const roleList = ref<RoleInfo[]>([])
const tableCardRef = ref<HTMLDivElement>()
const columnOptions: TableColumnOption[] = [
  { key: 'roleCode', label: '角色编码' },
  { key: 'roleName', label: '角色名称' },
  { key: 'description', label: '描述' },
  { key: 'sort', label: '排序' },
  { key: 'status', label: '状态' },
  { key: 'type', label: '类型' },
  { key: 'createTime', label: '创建时间' }
]
const { sortOrder, visibleColumns, isColumnVisible } = useTableToolbar(columnOptions)
const displayData = computed(() => sortByProperty(roleList.value, sortOrder.value, (row) => row.sort ?? 0))
const total = ref(0)

const query = reactive<RolePageQuery & { roleName?: string; status?: number; isSystem?: number }>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
})

const roleColors = [
  '#667eea', '#f5576c', '#4facfe', '#43e97b',
  '#fa709a', '#a8edea', '#ff9a9e', '#ffecd2',
  '#a1c4fd', '#d299c2', '#89f7fe', '#fddb92',
]
function getRoleColor(id: string): string {
  let hash = 0
  for (const ch of id) hash = (hash + ch.charCodeAt(0)) % roleColors.length
  return roleColors[hash]
}

async function loadRoles() {
  loading.value = true
  try {
    const params: RolePageQuery = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.keyword) params.keyword = query.keyword
    const res = await listRoles(params)
    let records = res.data.records
    // 前端二次过滤（角色名称/状态/类型，后端未支持这些筛选）
    if (query.roleName) {
      records = records.filter(r => r.roleName.includes(query.roleName!))
    }
    if (query.status != null) {
      records = records.filter(r => r.status === query.status)
    }
    if (query.isSystem != null) {
      records = records.filter(r => r.isSystem === query.isSystem)
    }
    roleList.value = records
    total.value = Number(res.data.total) || 0
  } catch { /* */ } finally {
    loading.value = false
  }
}

function onSizeChange() { query.pageNum = 1; loadRoles() }
function onFilterChange() { query.pageNum = 1; loadRoles() }
let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { query.pageNum = 1; loadRoles() }, 300)
}
function resetQuery() {
  query.keyword = ''; query.roleName = undefined; query.status = undefined; query.isSystem = undefined
  query.pageNum = 1; loadRoles()
}

// 新建/编辑相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive({
  id: undefined as string | undefined,
  roleCode: '',
  roleName: '',
  description: '',
  sort: 0,
})

const formRules: FormRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
}

function handleCreate() {
  isEdit.value = false
  formData.id = undefined
  formData.roleCode = ''
  formData.roleName = ''
  formData.description = ''
  formData.sort = 0
  dialogVisible.value = true
}

function handleEdit(row: RoleInfo) {
  isEdit.value = true
  formData.id = row.id
  formData.roleCode = row.roleCode
  formData.roleName = row.roleName
  formData.description = row.description || ''
  formData.sort = row.sort
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateRole({
        id: formData.id!,
        roleName: formData.roleName,
        description: formData.description,
        sort: formData.sort,
      })
      ElMessage.success('更新成功')
    } else {
      await createRole({
        roleCode: formData.roleCode,
        roleName: formData.roleName,
        description: formData.description,
        sort: formData.sort,
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadRoles()
  } catch { /* */ } finally {
    submitting.value = false
  }
}

async function handleToggle(row: RoleInfo) {
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}该角色吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await toggleRoleStatus(row.id)
    ElMessage.success(`${action}成功`)
    loadRoles()
  } catch { /* */ }
}

async function handleDelete(row: RoleInfo) {
  try {
    await ElMessageBox.confirm('确定要删除该角色吗？此操作不可恢复。', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error',
    })
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    loadRoles()
  } catch { /* */ }
}
function handleRowCommand(command: string, row: RoleInfo) {
  if (command === 'toggle') handleToggle(row)
  else if (command === 'delete') handleDelete(row)
}

// 权限配置相关
const permDialogVisible = ref(false)
const currentRole = ref<RoleInfo | null>(null)
const menuTree = ref<MenuItem[]>([])
const menuTreeRef = ref<any>()

async function handlePermission(row: RoleInfo) {
  currentRole.value = row
  permDialogVisible.value = true
  await loadMenuTree()
  await loadRoleMenus(row.id)
}

async function loadMenuTree() {
  try {
    const res = await getMenuTree()
    menuTree.value = res.data || []
  } catch { /* */ }
}

async function loadRoleMenus(roleId: string) {
  try {
    const res = await getRoleMenuIds(roleId)
    const menuIds = res.data || []
    if (menuTreeRef.value) {
      menuTreeRef.value.setCheckedKeys(menuIds)
    }
  } catch { /* */ }
}

async function handlePermissionSubmit() {
  if (!currentRole.value) return
  const checkedKeys = menuTreeRef.value?.getCheckedKeys() || []
  const halfCheckedKeys = menuTreeRef.value?.getHalfCheckedKeys() || []
  const allKeys = [...checkedKeys, ...halfCheckedKeys]
  try {
    await assignRoleMenus({
      roleId: currentRole.value.id,
      menuIds: allKeys,
    })
    ElMessage.success('权限配置成功')
    permDialogVisible.value = false
  } catch { /* */ }
}

onMounted(() => {
  loadRoles()
})
</script>

<style scoped>
.role-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.role-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.perm-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}
.perm-dialog-header strong {
  color: var(--primary);
}

.perm-tree-wrapper {
  max-height: 450px;
  overflow-y: auto;
  padding: 8px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
}
</style>
