<template>
  <div class="role-manage-page">
    <div class="page-header">
      <h2 class="page-title">角色管理</h2>
      <div class="header-actions">
        <el-input
          v-model="keyword"
          placeholder="搜索角色名称/编码"
          clearable
          style="width: 260px"
          @keyup.enter="loadRoles"
          @clear="loadRoles"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          <span>新建角色</span>
        </el-button>
      </div>
    </div>

    <div class="role-table-wrapper">
      <el-table :data="roleList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleCode" label="角色编码" width="160" />
        <el-table-column prop="roleName" label="角色名称" width="180">
          <template #default="{ row }">
            <span class="role-name-cell">
              <span class="role-dot" :style="{ background: getRoleColor(row.id) }"></span>
              {{ row.roleName }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light" round>
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.isSystem === 1" type="danger" effect="light" round>系统角色</el-tag>
            <el-tag v-else type="info" effect="light" round>自定义</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handlePermission(row)">
              <el-icon><Key /></el-icon>
              权限配置
            </el-button>
            <el-button type="primary" link :disabled="row.isSystem === 1" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button :type="row.status === 1 ? 'warning' : 'success'" link :disabled="row.isSystem === 1" @click="handleToggle(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" link :disabled="row.isSystem === 1" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadRoles"
          @current-change="loadRoles"
        />
      </div>
    </div>

    <!-- 新建/编辑角色对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新建角色'" width="500px" :close-on-click-modal="false">
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
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
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 权限配置对话框 -->
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Plus, Edit, Delete, Key } from '@element-plus/icons-vue'
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

const loading = ref(false)
const roleList = ref<RoleInfo[]>([])
const total = ref(0)
const keyword = ref('')

const queryParams = reactive<RolePageQuery>({
  pageNum: 1,
  pageSize: 10,
})

const roleColors = [
  '#667eea', '#f5576c', '#4facfe', '#43e97b',
  '#fa709a', '#a8edea', '#ff9a9e', '#ffecd2',
  '#a1c4fd', '#d299c2', '#89f7fe', '#fddb92',
]

function getRoleColor(id: number): string {
  return roleColors[id % roleColors.length]
}

function formatDate(date: string): string {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN')
}

async function loadRoles() {
  loading.value = true
  try {
    const res = await listRoles({
      ...queryParams,
      keyword: keyword.value || undefined,
    })
    roleList.value = res.data.records
    total.value = res.data.total
  } catch { /* */ } finally {
    loading.value = false
  }
}

// 新建/编辑相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive({
  id: 0,
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
  formData.id = 0
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
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await updateRole({
          id: formData.id,
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
    } catch { /* */ }
  })
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

async function loadRoleMenus(roleId: number) {
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
.role-manage-page {
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.role-table-wrapper {
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border-light);
  padding: 20px;
}

.role-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.role-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
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
