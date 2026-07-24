<template>
  <div class="page-view">
    <div class="page-header"><h2>菜单管理</h2></div>
    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="search-fields">
        <div class="search-item"><el-input class="search-keyword-input" v-model="filterName" placeholder="请输入菜单名称" clearable @input="onSearchInput" /></div>
        <template v-if="expanded">
          <div class="search-item"><label>路由地址</label><el-input v-model="filterPath" placeholder="请输入路由地址" clearable @input="onSearchInput" /></div>
        </template>
      </div>
      <div class="search-actions">
        <el-button @click="resetFilter">重置</el-button>
        <el-button type="primary" @click="applyFilter">查询</el-button>
        <el-button link type="primary" @click="expanded = !expanded">{{ expanded ? '收起' : '展开' }} <el-icon><ArrowDown v-if="!expanded" /><ArrowUp v-else /></el-icon></el-button>
      </div>
    </div>

    <!-- 表格卡片 -->
    <div class="table-card">
      <!-- 工具栏 -->
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-button class="btn-outline" @click="showCreateDialog()"><el-icon><Plus /></el-icon>添加菜单</el-button>
          <el-button @click="toggleExpandAll">{{ expandAll ? '收起' : '展开' }}</el-button>
        </div>
        <div class="toolbar-right">
          <el-tooltip content="刷新"><el-button circle @click="loadData"><el-icon><Refresh /></el-icon></el-button></el-tooltip>
          <el-tooltip content="排序"><el-button circle><el-icon><Sort /></el-icon></el-button></el-tooltip>
          <el-tooltip content="全屏"><el-button circle><el-icon><FullScreen /></el-icon></el-button></el-tooltip>
          <el-tooltip content="列设置"><el-button circle><el-icon><Setting /></el-icon></el-button></el-tooltip>
        </div>
      </div>

      <!-- 树形表格 -->
      <el-table ref="tableRef" :data="filteredData" v-loading="loading"
        row-key="id" :default-expand-all="expandAll"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        :header-cell-style="{ background: '#fafbfc', color: '#606266', fontWeight: 500 }">
        <el-table-column prop="name" label="菜单名称" min-width="200" />
        <el-table-column label="菜单类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="!row.path && row.children && row.children.length > 0" size="small" type="info" effect="light">目录</el-tag>
            <el-tag v-else size="small" type="primary" effect="light">菜单</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由" min-width="150">
          <template #default="{ row }">
            <span v-if="row.path">{{ row.path }}</span>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="权限标识" min-width="120">
          <template #default>
            <span style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="编辑时间" min-width="170" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small" effect="light">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-tooltip content="添加子菜单"><el-button type="primary" link circle size="small" @click="showCreateDialog(row.id)"><el-icon><Plus /></el-icon></el-button></el-tooltip>
              <el-tooltip content="编辑"><el-button type="primary" link circle size="small" @click="showEditDialog(row)"><el-icon><Edit /></el-icon></el-button></el-tooltip>
              <el-tooltip content="删除"><el-button type="danger" link circle size="small" @click="handleDelete(row.id)"><el-icon><Delete /></el-icon></el-button></el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑菜单' : '添加菜单'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="菜单名称" prop="name"><el-input v-model="form.name" placeholder="请输入菜单名称" /></el-form-item>
        <el-form-item label="路由路径"><el-input v-model="form.path" placeholder="如: /admin/users（留空则为目录）" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" placeholder="如: HomeFilled" /></el-form-item>
        <el-form-item label="上级菜单"><el-tree-select v-model="form.parentId" :data="treeData" :props="{ label: 'name', value: 'id', children: 'children' }" check-strictly clearable placeholder="留空则为顶级菜单" style="width:100%" /></el-form-item>
        <el-form-item label="排序号"><el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width:180px" /></el-form-item>
        <el-form-item label="是否显示"><el-radio-group v-model="form.visible"><el-radio :value="1">显示</el-radio><el-radio :value="0">隐藏</el-radio></el-radio-group></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Refresh, Sort, FullScreen, Setting, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { getMenuTree, createMenu, updateMenu, deleteMenu } from '@/api/menu'
import type { MenuItem } from '@/types/menu'

const loading = ref(false); const expanded = ref(false)
const submitting = ref(false)
const treeData = ref<MenuItem[]>([])
const filteredData = ref<MenuItem[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const expandAll = ref(true)
const filterName = ref('')
const filterPath = ref('')
let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() { if (searchTimer) clearTimeout(searchTimer); searchTimer = setTimeout(applyFilter, 300) }

const form = reactive({ id: undefined as number | undefined, name: '', path: '', icon: '', parentId: undefined as number | undefined, sortOrder: 0, visible: 1 })
const rules: FormRules = { name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }] }

import { reactive } from 'vue'

async function loadData() {
  loading.value = true
  try {
    const res = await getMenuTree()
    treeData.value = res.data
    applyFilter()
  } catch { /* */ } finally { loading.value = false }
}

function applyFilter() {
  const result = JSON.parse(JSON.stringify(treeData.value))
  if (filterName.value || filterPath.value) {
    const filterTree = (items: MenuItem[]): MenuItem[] => {
      return items.filter(item => {
        const nameMatch = !filterName.value || item.name.includes(filterName.value)
        const pathMatch = !filterPath.value || (item.path && item.path.includes(filterPath.value))
        const childMatch = item.children && item.children.length > 0
        if (item.children) item.children = filterTree(item.children)
        return (nameMatch && pathMatch) || (childMatch && item.children && item.children.length > 0)
      })
    }
    filteredData.value = filterTree(result)
  } else {
    filteredData.value = result
  }
}

function resetFilter() { filterName.value = ''; filterPath.value = ''; applyFilter() }
function toggleExpandAll() {
  expandAll.value = !expandAll.value
  loadData()
}

function showCreateDialog(parentId?: number) {
  isEdit.value = false
  Object.assign(form, { id: undefined, name: '', path: '', icon: '', parentId: parentId || undefined, sortOrder: 0, visible: 1 })
  dialogVisible.value = true
}

function showEditDialog(row: MenuItem) {
  isEdit.value = true
  Object.assign(form, { id: row.id, name: row.name, path: row.path || '', icon: row.icon || '', parentId: row.parentId === 0 ? undefined : row.parentId, sortOrder: row.sortOrder, visible: row.visible })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    
    if (isEdit.value) { await updateMenu({ id: form.id!, name: form.name, path: form.path, icon: form.icon, parentId: form.parentId || 0, sortOrder: form.sortOrder, visible: form.visible }); ElMessage.success('更新成功') }
    else { await createMenu({ name: form.name, path: form.path, icon: form.icon, parentId: form.parentId || 0, sortOrder: form.sortOrder, visible: form.visible }); ElMessage.success('创建成功') }
    dialogVisible.value = false
    loadData()
  } catch { /* */ } finally { submitting.value = false }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该菜单?', '提示', { type: 'warning' })
    await deleteMenu(id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* */ }
}

onMounted(loadData)
</script>

<style scoped>
.page-header { margin-bottom: 0; }
.page-header h2 { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-view { display: flex; flex-direction: column; gap: 16px; }

/* 搜索栏 */
.search-bar {
  background: #fff; border-radius: 12px; padding: 20px 24px;
  display: flex; align-items: flex-end; justify-content: space-between;
  border: 1px solid #f0f0f0;
}
.search-fields { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; flex: 1; align-items: end; }
    .search-item { display: flex; flex-direction: column; gap: 6px; }
    .search-item label { font-size: 13px; color: #606266; font-weight: 500; }
    .search-item :deep(.el-input),
    .search-item :deep(.el-select) { width: 260px; }
    .search-keyword-input { width: 640px !important; }
.search-actions { display: flex; gap: 8px; }

/* 表格卡片 */
.table-card {
  background: #fff; border-radius: 12px; border: 1px solid #f0f0f0; overflow: hidden;
}

/* 工具栏 */
.table-toolbar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px; border-bottom: 1px solid #f5f5f5;
}
.toolbar-left { display: flex; gap: 8px; }
.toolbar-right { display: flex; gap: 4px; }

/* 操作按钮 */
.action-buttons { display: flex; justify-content: center; gap: 4px; }
</style>
