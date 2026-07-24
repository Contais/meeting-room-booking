<template>
  <div class="page-view">
    <div class="page-header"><h2>部门管理</h2></div>
    <div class="search-bar">
      <div class="search-fields">
        <div class="search-item"><el-input class="search-keyword-input" v-model="filterName" placeholder="请输入部门名称" clearable /></div>
      </div>
      <div class="search-actions">
        <el-button @click="filterName = ''; loadData()">重置</el-button>
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-button class="btn-outline" @click="showCreateDialog()"><el-icon><Plus /></el-icon>新增部门</el-button>
          <el-button @click="expandAll = !expandAll">{{ expandAll ? '收起' : '展开' }}</el-button>
        </div>
        <div class="toolbar-right">
          <el-tooltip content="刷新"><el-button circle @click="loadData"><el-icon><Refresh /></el-icon></el-button></el-tooltip>
        </div>
      </div>

      <el-table :data="treeData" v-loading="loading" row-key="id" :default-expand-all="expandAll" :tree-props="{ children: 'children' }" :header-cell-style="{ background: '#fafbfc', color: '#606266', fontWeight: 500 }">
        <el-table-column prop="name" label="部门名称" min-width="220" />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small" effect="light">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-tooltip content="添加子部门"><el-button type="primary" link circle size="small" @click="showCreateDialog(row.id)"><el-icon><Plus /></el-icon></el-button></el-tooltip>
              <el-tooltip content="编辑"><el-button type="primary" link circle size="small" @click="showEditDialog(row)"><el-icon><Edit /></el-icon></el-button></el-tooltip>
              <el-tooltip content="删除"><el-button type="danger" link circle size="small" @click="handleDelete(row.id)"><el-icon><Delete /></el-icon></el-button></el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑部门' : '新增部门'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="部门名称" prop="name"><el-input v-model="form.name" placeholder="请输入部门名称" /></el-form-item>
        <el-form-item label="上级部门"><el-tree-select v-model="form.parentId" :data="treeData" :props="{ label: 'name', value: 'id', children: 'children' }" check-strictly clearable placeholder="留空则为顶级部门" style="width:100%" /></el-form-item>
        <el-form-item label="排序号"><el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width:180px" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Refresh } from '@element-plus/icons-vue'
import { getDepartmentTree, createDepartment, updateDepartment, deleteDepartment } from '@/api/department'
import type { Department } from '@/types/department'

const loading = ref(false); const submitting = ref(false)
const treeData = ref<Department[]>([])
const dialogVisible = ref(false); const isEdit = ref(false); const formRef = ref<FormInstance>()
const expandAll = ref(true); const filterName = ref('')
const form = reactive({ id: undefined as number | undefined, name: '', parentId: undefined as number | undefined, sortOrder: 0 })
const rules: FormRules = { name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }] }

async function loadData() { loading.value = true; try { const res = await getDepartmentTree(); treeData.value = res.data } catch { /* */ } finally { loading.value = false } }
function showCreateDialog(parentId?: number) { isEdit.value = false; Object.assign(form, { id: undefined, name: '', parentId: parentId || undefined, sortOrder: 0 }); dialogVisible.value = true }
function showEditDialog(row: Department) { isEdit.value = true; Object.assign(form, { id: row.id, name: row.name, parentId: row.parentId === 0 ? undefined : row.parentId, sortOrder: row.sortOrder }); dialogVisible.value = true }
async function handleSubmit() { const valid = await formRef.value?.validate().catch(() => false); if (!valid) return; submitting.value = true; try { if (isEdit.value) { await updateDepartment({ id: form.id!, name: form.name, parentId: form.parentId || 0, sortOrder: form.sortOrder }); ElMessage.success('更新成功') } else { await createDepartment({ name: form.name, parentId: form.parentId || 0, sortOrder: form.sortOrder }); ElMessage.success('创建成功') }; dialogVisible.value = false; loadData() } catch { /* */ } finally { submitting.value = false } }
async function handleDelete(id: number) { try { await ElMessageBox.confirm('确定删除该部门?', '提示', { type: 'warning' }); await deleteDepartment(id); ElMessage.success('删除成功'); loadData() } catch { /* */ } }
onMounted(loadData)
</script>

<style scoped>
.page-header { margin-bottom: 0; }
.page-header h2 { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-view { display: flex; flex-direction: column; gap: 16px; }
.search-bar { background: #fff; border-radius: 12px; padding: 20px 24px; display: flex; align-items: flex-end; justify-content: space-between; border: 1px solid #f0f0f0; }
.search-fields { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; flex: 1; align-items: end; }
    .search-item { display: flex; flex-direction: column; gap: 6px; }
    .search-item label { font-size: 13px; color: #606266; font-weight: 500; }
    .search-item :deep(.el-input),
    .search-item :deep(.el-select) { width: 220px; }
    .search-keyword-input :deep(.el-input) { width: 640px; }
.search-actions { display: flex; gap: 8px; }
.table-card { background: #fff; border-radius: 12px; border: 1px solid #f0f0f0; overflow: hidden; }
.table-toolbar { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #f5f5f5; }
.toolbar-left { display: flex; gap: 8px; }
.toolbar-right { display: flex; gap: 4px; }
.action-buttons { display: flex; justify-content: center; gap: 4px; }
</style>
