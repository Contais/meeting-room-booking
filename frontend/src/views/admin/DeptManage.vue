<template>
  <div class="page-view">
    <SearchBar @search="applyFilter" @reset="resetFilter">
      <template #collapsed>
        <el-input v-model="filterName" placeholder="搜索部门名称" clearable @input="onSearchInput" @keyup.enter="applyFilter" />
      </template>
      <template #expanded>
        <div class="search-item">
          <label>部门名称</label>
          <el-input v-model="filterName" placeholder="请输入" clearable @input="onSearchInput" @keyup.enter="applyFilter" />
        </div>
        <div class="search-item">
          <label>状态</label>
          <el-select v-model="filterStatus" placeholder="全部" clearable @change="applyFilter">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </div>
        <div class="search-item is-wide">
          <label>创建时间</label>
          <el-date-picker v-model="createTimeRange" type="datetimerange" range-separator="至"
            start-placeholder="开始时间" end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss" @change="applyFilter" />
        </div>
      </template>
    </SearchBar>

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

      <el-table :data="filteredTree" v-loading="loading" row-key="id" :key="expandAll" :default-expand-all="expandAll" :tree-props="{ children: 'children' }">
        <el-table-column prop="name" label="部门名称" min-width="220" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small" effect="light">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-links">
              <el-button type="primary" link @click="showCreateDialog(row.id)">
                <el-icon><Plus /></el-icon>添加子部门
              </el-button>
              <el-button type="primary" link @click="showEditDialog(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
              <el-button type="danger" link @click="handleDelete(row.id)">
                <el-icon><Delete /></el-icon>删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <FormDrawer v-model:visible="dialogVisible" :title="isEdit ? '编辑部门' : '新增部门'" :loading="submitting" @submit="handleSubmit">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="部门名称" prop="name"><el-input v-model="form.name" placeholder="请输入部门名称" /></el-form-item>
        <el-form-item label="上级部门"><el-tree-select v-model="form.parentId" :data="treeData" node-key="id" :props="{ label: 'name', children: 'children' }" check-strictly clearable placeholder="留空则为顶级部门" style="width:100%" /></el-form-item>
        <el-form-item label="排序号"><el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width:180px" /></el-form-item>
      </el-form>
    </FormDrawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Refresh } from '@element-plus/icons-vue'
import { getDepartmentTree, createDepartment, updateDepartment, deleteDepartment } from '@/api/department'
import SearchBar from '@/components/SearchBar.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import { formatDateTime } from '@/utils/datetime'
import type { Department } from '@/types/department'

const loading = ref(false)
const submitting = ref(false)
const filterStatus = ref(undefined as number | undefined)
const treeData = ref<Department[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const expandAll = ref(true)
const filterName = ref('')
const createTimeRange = ref<string[]>([])
let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() { if (searchTimer) clearTimeout(searchTimer); searchTimer = setTimeout(applyFilter, 300) }
const form = reactive({ id: undefined as number | undefined, name: '', parentId: undefined as number | undefined, sortOrder: 0 })
const rules: FormRules = { name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }] }

/**
 * 客户端树形过滤：保留命中节点及其祖先链路
 * 部门数据量小，无需后端分页，前端过滤即可
 */
const filteredTree = computed(() => {
  if (!filterName.value && filterStatus.value === undefined && createTimeRange.value.length === 0) {
    return treeData.value
  }
  const start = createTimeRange.value[0] || ''
  const end = createTimeRange.value[1] || ''
  const filterNode = (node: Department): Department | null => {
    const nameMatch = !filterName.value || node.name.includes(filterName.value)
    const statusMatch = filterStatus.value === undefined || node.status === filterStatus.value
    const timeMatch = (!start || !node.createTime || node.createTime >= start)
      && (!end || !node.createTime || node.createTime <= end)
    const selfMatch = nameMatch && statusMatch && timeMatch
    const children = (node.children || []).map(filterNode).filter((c): c is Department => c !== null)
    if (selfMatch || children.length > 0) {
      return { ...node, children }
    }
    return null
  }
  return treeData.value.map(filterNode).filter((n): n is Department => n !== null)
})

function applyFilter() {
  // 计算属性自动响应，此函数仅为查询按钮提供显式触发入口
  // el-table 通过 :data="filteredTree" 自动更新
}
function resetFilter() {
  filterName.value = ''
  filterStatus.value = undefined
  createTimeRange.value = []
  applyFilter()
}

async function loadData() {
  loading.value = true
  try { const res = await getDepartmentTree(); treeData.value = res.data }
  catch { /* */ } finally { loading.value = false }
}
function showCreateDialog(parentId?: number) { isEdit.value = false; Object.assign(form, { id: undefined, name: '', parentId: parentId || undefined, sortOrder: 0 }); dialogVisible.value = true }
function showEditDialog(row: Department) { isEdit.value = true; const pidNum = Number(row.parentId); Object.assign(form, { id: row.id, name: row.name, parentId: !row.parentId || pidNum === 0 ? undefined : row.parentId, sortOrder: row.sortOrder }); dialogVisible.value = true }
async function handleSubmit() { const valid = await formRef.value?.validate().catch(() => false); if (!valid) return; submitting.value = true; try { if (isEdit.value) { await updateDepartment({ id: form.id!, name: form.name, parentId: form.parentId || 0, sortOrder: form.sortOrder }); ElMessage.success('更新成功') } else { await createDepartment({ name: form.name, parentId: form.parentId || 0, sortOrder: form.sortOrder }); ElMessage.success('创建成功') }; dialogVisible.value = false; loadData() } catch { /* */ } finally { submitting.value = false } }
async function handleDelete(id: number) { try { await ElMessageBox.confirm('确定删除该部门?', '提示', { type: 'warning' }); await deleteDepartment(id); ElMessage.success('删除成功'); loadData() } catch { /* */ } }
onMounted(loadData)
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }
</style>
