<template>
  <div class="page-view">
    <div class="page-header"><h2>菜单管理</h2></div>
    <div class="table-card page-card">
      <div class="table-toolbar"><div class="table-toolbar-left"><el-button class="btn-outline" @click="showCreateDialog()"><el-icon><Plus /></el-icon>新增菜单</el-button></div></div>
      <el-table :data="treeData" v-loading="loading" row-key="id" :tree-props="{ children: 'children' }" default-expand-all>
        <el-table-column prop="name" label="菜单名称" min-width="160" />
        <el-table-column prop="path" label="路由路径" min-width="160" />
        <el-table-column prop="icon" label="图标" width="100" />
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
        <el-table-column prop="visible" label="显示" width="70" align="center"><template #default="{ row }"><el-tag :type="row.visible === 1 ? 'success' : 'info'" size="small">{{ row.visible === 1 ? '是' : '否' }}</el-tag></template></el-table-column>
        <el-table-column prop="status" label="状态" width="70" align="center"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }"><div class="action-buttons"><el-button type="primary" link @click="showEditDialog(row)">编辑</el-button><el-button type="danger" link @click="handleDelete(row.id)">删除</el-button></div></template>
        </el-table-column>
      </el-table>
    </div>
    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑菜单' : '新增菜单'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="菜单名称" prop="name"><el-input v-model="form.name" placeholder="请输入菜单名称" /></el-form-item>
        <el-form-item label="路由路径"><el-input v-model="form.path" placeholder="如: /admin/users" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" placeholder="如: HomeFilled" /></el-form-item>
        <el-form-item label="上级菜单"><el-tree-select v-model="form.parentId" :data="treeData" :props="{ label: 'name', value: 'id', children: 'children' }" check-strictly clearable placeholder="留空则为顶级菜单" style="width:100%" /></el-form-item>
        <el-form-item label="排序号"><el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width:180px" /></el-form-item>
        <el-form-item label="是否显示"><el-radio-group v-model="form.visible"><el-radio :value="1">显示</el-radio><el-radio :value="0">隐藏</el-radio></el-radio-group></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" class="btn-gradient" :loading="submitting" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
    <!-- 角色权限对话框 -->
    <el-dialog v-model="roleDialogVisible" title="配置角色权限" width="480px" destroy-on-close>
      <div v-if="currentMenu" style="margin-bottom:12px;color:var(--text-secondary)">菜单: {{ currentMenu.name }}</div>
      <el-checkbox-group v-model="selectedRoles">
        <el-checkbox value="admin" label="管理员" />
        <el-checkbox value="user" label="普通用户" />
      </el-checkbox-group>
      <template #footer><el-button @click="roleDialogVisible = false">取消</el-button><el-button type="primary" class="btn-gradient" :loading="roleSaving" @click="handleSaveRoleMenus">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getMenuTree, createMenu, updateMenu, deleteMenu } from '@/api/menu'
import type { MenuItem } from '@/types/menu'

const loading = ref(false); const submitting = ref(false)
const treeData = ref<MenuItem[]>([])
const dialogVisible = ref(false); const isEdit = ref(false); const formRef = ref<FormInstance>()
const form = reactive({ id: undefined as number | undefined, name: '', path: '', icon: '', parentId: undefined as number | undefined, sortOrder: 0, visible: 1 })
const rules: FormRules = { name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }] }

// 角色权限
const roleDialogVisible = ref(false); const roleSaving = ref(false)
const currentMenu = ref<MenuItem | null>(null); const selectedRoles = ref<string[]>([])

async function loadData() { loading.value = true; try { const res = await getMenuTree(); treeData.value = res.data } catch { /* */ } finally { loading.value = false } }
function showCreateDialog(parentId?: number) { isEdit.value = false; Object.assign(form, { id: undefined, name: '', path: '', icon: '', parentId: parentId || undefined, sortOrder: 0, visible: 1 }); dialogVisible.value = true }
function showEditDialog(row: MenuItem) { isEdit.value = true; Object.assign(form, { id: row.id, name: row.name, path: row.path || '', icon: row.icon || '', parentId: row.parentId === 0 ? undefined : row.parentId, sortOrder: row.sortOrder, visible: row.visible }); dialogVisible.value = true }
async function handleSubmit() { const valid = await formRef.value?.validate().catch(() => false); if (!valid) return; submitting.value = true; try { if (isEdit.value) { await updateMenu({ id: form.id!, name: form.name, path: form.path, icon: form.icon, parentId: form.parentId || 0, sortOrder: form.sortOrder, visible: form.visible }); ElMessage.success('更新成功') } else { await createMenu({ name: form.name, path: form.path, icon: form.icon, parentId: form.parentId || 0, sortOrder: form.sortOrder, visible: form.visible }); ElMessage.success('创建成功') }; dialogVisible.value = false; loadData() } catch { /* */ } finally { submitting.value = false } }
async function handleDelete(id: number) { try { await ElMessageBox.confirm('确定删除该菜单?', '提示', { type: 'warning' }); await deleteMenu(id); ElMessage.success('删除成功'); loadData() } catch { /* */ } }
async function handleSaveRoleMenus() { /* TODO: 实现角色菜单权限保存 */ roleDialogVisible.value = false }
onMounted(loadData)
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }
.action-buttons { display: flex; justify-content: center; gap: 8px; }
</style>
