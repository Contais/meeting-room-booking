<template>
  <div class="page-view">
    <div class="page-header"><h2>部门管理</h2></div>
    <div class="table-card page-card">
      <div class="table-toolbar"><div class="table-toolbar-left"><el-button class="btn-outline" @click="showCreateDialog()"><el-icon><Plus /></el-icon>新增部门</el-button></div></div>
      <el-table :data="treeData" v-loading="loading" row-key="id" :tree-props="{ children: 'children' }" default-expand-all>
        <el-table-column prop="name" label="部门名称" min-width="200" />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="80" align="center"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag></template></el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }"><div class="action-buttons"><el-button type="primary" link @click="showCreateDialog(row.id)">添加子部门</el-button><el-button type="primary" link @click="showEditDialog(row)">编辑</el-button><el-button type="danger" link @click="handleDelete(row.id)">删除</el-button></div></template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑部门' : '新增部门'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="部门名称" prop="name"><el-input v-model="form.name" placeholder="请输入部门名称" /></el-form-item>
        <el-form-item label="上级部门"><el-tree-select v-model="form.parentId" :data="treeData" :props="{ label: 'name', value: 'id', children: 'children' }" check-strictly clearable placeholder="留空则为顶级部门" style="width:100%" /></el-form-item>
        <el-form-item label="排序号"><el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width:180px" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" class="btn-gradient" :loading="submitting" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getDepartmentTree, createDepartment, updateDepartment, deleteDepartment } from '@/api/department'
import type { Department } from '@/types/department'

const loading = ref(false); const submitting = ref(false)
const treeData = ref<Department[]>([])
const dialogVisible = ref(false); const isEdit = ref(false); const formRef = ref<FormInstance>()
const form = reactive({ id: undefined as number | undefined, name: '', parentId: undefined as number | undefined, sortOrder: 0 })
const rules: FormRules = { name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }] }

async function loadData() { loading.value = true; try { const res = await getDepartmentTree(); treeData.value = res.data } catch { /* */ } finally { loading.value = false } }
function showCreateDialog(parentId?: number) { isEdit.value = false; Object.assign(form, { id: undefined, name: '', parentId: parentId || undefined, sortOrder: 0 }); dialogVisible.value = true }
function showEditDialog(row: Department) { isEdit.value = true; Object.assign(form, { id: row.id, name: row.name, parentId: row.parentId === 0 ? undefined : row.parentId, sortOrder: row.sortOrder }); dialogVisible.value = true }
async function handleSubmit() { const valid = await formRef.value?.validate().catch(() => false); if (!valid) return; submitting.value = true; try { const data = { ...form, parentId: form.parentId || 0 }; if (isEdit.value) { await updateDepartment(data); ElMessage.success('更新成功') } else { await createDepartment(data); ElMessage.success('创建成功') }; dialogVisible.value = false; loadData() } catch { /* */ } finally { submitting.value = false } }
async function handleDelete(id: number) { try { await ElMessageBox.confirm('确定删除该部门?', '提示', { type: 'warning' }); await deleteDepartment(id); ElMessage.success('删除成功'); loadData() } catch { /* */ } }
onMounted(loadData)
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }
.action-buttons { display: flex; justify-content: center; gap: 8px; }
</style>
