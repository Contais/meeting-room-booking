<template>
  <div class="page-view">
    <!-- 搜索栏 -->
    <SearchBar @search="applyFilter" @reset="resetFilter">
      <template #collapsed>
        <el-input v-model="filterName" placeholder="搜索菜单名称" clearable @input="onSearchInput" @keyup.enter="applyFilter" />
      </template>
      <template #expanded>
        <div class="search-item"><label>菜单名称</label><el-input v-model="filterName" placeholder="请输入" clearable @input="onSearchInput" @keyup.enter="applyFilter" /></div>
        <div class="search-item"><label>路由地址</label><el-input v-model="filterPath" placeholder="请输入" clearable @input="onSearchInput" @keyup.enter="applyFilter" /></div>
        <div class="search-item"><label>状态</label><el-select v-model="filterStatus" placeholder="全部" clearable @change="applyFilter"><el-option label="启用" :value="1" /><el-option label="禁用" :value="0" /></el-select></div>
        <div class="search-item is-wide"><label>创建时间</label><el-date-picker v-model="createTimeRange" type="datetimerange" range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" value-format="YYYY-MM-DDTHH:mm:ss" @change="applyFilter" /></div>
      </template>
    </SearchBar>

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
        row-key="id" :key="expandAll" :default-expand-all="expandAll"
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
        <el-table-column label="创建时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.createTime) }}</template></el-table-column>
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

    <!-- 新增/编辑抽屉 -->
    <FormDrawer v-model:visible="dialogVisible" :title="isEdit ? '编辑菜单' : '添加菜单'" :loading="submitting" @submit="handleSubmit">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="菜单名称" prop="name"><el-input v-model="form.name" placeholder="搜索菜单名称" /></el-form-item>
        <el-form-item label="路由路径"><el-input v-model="form.path" placeholder="如: /admin/users（留空则为目录）" /></el-form-item>
        <el-form-item label="图标">
          <el-select v-model="form.icon" placeholder="请选择图标" style="width: 100%" filterable>
            <el-option v-for="icon in iconOptions" :key="icon" :value="icon" :label="icon">
              <div style="display: flex; align-items: center; gap: 8px;">
                <el-icon><component :is="iconMap[icon]" /></el-icon>
                <span>{{ icon }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="上级菜单"><el-tree-select v-model="form.parentId" :data="treeData" :props="{ label: 'name', value: 'id', children: 'children' }" check-strictly clearable placeholder="留空则为顶级菜单" style="width:100%" /></el-form-item>
        <el-form-item label="排序号"><el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width:180px" /></el-form-item>
        <el-form-item label="是否启用"><el-radio-group v-model="form.status"><el-radio :value="1">启用</el-radio><el-radio :value="0">禁用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="是否显示"><el-radio-group v-model="form.visible"><el-radio :value="1">显示</el-radio><el-radio :value="0">隐藏</el-radio></el-radio-group></el-form-item>
      </el-form>
    </FormDrawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, markRaw } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Edit, Delete, Refresh, Sort, FullScreen, Setting,
  HomeFilled, User, UserFilled, OfficeBuilding, Calendar,
  Menu, Grid, List, Picture, Camera, Bell, Message,
  Avatar, Coin, Goods, DataLine, Location, Phone,
  Lock, Key, Tools, Box, Folder, Files, Document,
  Reading, Tickets, School, Medal, Trophy,
  Star, Warning, InfoFilled, SuccessFilled,
  CircleCheck, CircleClose, SwitchButton, Connection,
  Platform, Histogram, DataBoard, PieChart, TrendCharts,
  Share, Promotion, ChatLineRound, ChatDotRound,
  IceCreamRound, IceTea, Coffee, Food, Goblet,
  Basketball, Football, Soccer,
  Monitor, Iphone, PhoneFilled, Microphone, Headset,
  MagicStick, Brush, Guide, PictureFilled, CameraFilled
} from '@element-plus/icons-vue'
import { getMenuTree, createMenu, updateMenu, deleteMenu } from '@/api/menu'
import SearchBar from '@/components/SearchBar.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import { formatDateTime } from '@/utils/datetime'
import type { MenuItem } from '@/types/menu'

const loading = ref(false)
const submitting = ref(false)
const treeData = ref<MenuItem[]>([])
const filteredData = ref<MenuItem[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const expandAll = ref(true)
const filterName = ref('')
const filterPath = ref(''); const filterStatus = ref(undefined as number | undefined)
const createTimeRange = ref<string[]>([])
let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() { if (searchTimer) clearTimeout(searchTimer); searchTimer = setTimeout(applyFilter, 300) }

const form = reactive({ id: undefined as number | undefined, name: '', path: '', icon: '', parentId: undefined as number | undefined, sortOrder: 0, status: 1, visible: 1 })
const rules: FormRules = { name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }] }

const iconMap: Record<string, any> = markRaw({
  HomeFilled, User, UserFilled, OfficeBuilding, Calendar,
  Menu, Grid, List, Picture, Camera, Bell, Message,
  Avatar, Coin, Goods, DataLine, Location, Phone,
  Lock, Key, Tools, Box, Folder, Files, Document,
  Reading, Tickets, School, Medal, Trophy,
  Star, Warning, InfoFilled, SuccessFilled,
  CircleCheck, CircleClose, SwitchButton, Connection,
  Platform, Histogram, DataBoard, PieChart, TrendCharts,
  Share, Promotion, ChatLineRound, ChatDotRound,
  IceCreamRound, IceTea, Coffee, Food, Goblet,
  Basketball, Football, Soccer,
  Monitor, Iphone, PhoneFilled, Microphone, Headset,
  MagicStick, Brush, Guide, PictureFilled, CameraFilled,
  Plus, Edit, Delete, Refresh, Sort, FullScreen, Setting
})

const iconOptions = Object.keys(iconMap)

async function loadData() {
  loading.value = true
  try {
    const res = await getMenuTree()
    treeData.value = res.data
    applyFilter()
  } catch { /* */ } finally { loading.value = false }
}

function applyFilter() {
  const start = createTimeRange.value[0] || ''
  const end = createTimeRange.value[1] || ''
  const hasFilter = filterName.value || filterPath.value || filterStatus.value !== undefined || start || end
  if (!hasFilter) {
    filteredData.value = treeData.value
    return
  }
  // 深拷贝避免污染源数据
  const result = JSON.parse(JSON.stringify(treeData.value))
  const filterTree = (items: MenuItem[]): MenuItem[] => {
    return items.filter(item => {
      const nameMatch = !filterName.value || item.name.includes(filterName.value)
      const pathMatch = !filterPath.value || (item.path && item.path.includes(filterPath.value))
      const statusMatch = filterStatus.value === undefined || item.status === filterStatus.value
      const timeMatch = (!start || !item.createTime || item.createTime >= start)
        && (!end || !item.createTime || item.createTime <= end)
      if (item.children) item.children = filterTree(item.children)
      const hasChildren = item.children && item.children.length > 0
      return (nameMatch && pathMatch && statusMatch && timeMatch) || (hasChildren && item.children && item.children.length > 0)
    })
  }
  filteredData.value = filterTree(result)
}

function resetFilter() { filterName.value = ''; filterPath.value = ''; filterStatus.value = undefined; createTimeRange.value = []; applyFilter() }
function toggleExpandAll() {
  expandAll.value = !expandAll.value
  loadData()
}

function showCreateDialog(parentId?: number) {
  isEdit.value = false
  Object.assign(form, { id: undefined, name: '', path: '', icon: '', parentId: parentId || undefined, sortOrder: 0, status: 1, visible: 1 })
  dialogVisible.value = true
}

function showEditDialog(row: MenuItem) {
  isEdit.value = true
  // parentId 由后端 Long→String 序列化为字符串，需统一比较与转换
  const pidNum = Number(row.parentId)
  Object.assign(form, {
    id: row.id,
    name: row.name,
    path: row.path || '',
    icon: row.icon || '',
    parentId: !row.parentId || pidNum === 0 ? undefined : row.parentId,
    sortOrder: row.sortOrder ?? 0,
    status: row.status ?? 1,
    visible: row.visible ?? 1
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    // status/visible 显式 Number 转换 + 默认值，避免 undefined 被 JSON.stringify 忽略
    const payload = {
      name: form.name,
      path: form.path,
      icon: form.icon,
      parentId: form.parentId || 0,
      sortOrder: form.sortOrder,
      status: Number(form.status ?? 1),
      visible: Number(form.visible ?? 1)
    }
    if (isEdit.value) { await updateMenu({ id: form.id!, ...payload }); ElMessage.success('更新成功') }
    else { await createMenu(payload); ElMessage.success('创建成功') }
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
.page-view { display: flex; flex-direction: column; gap: 16px; }

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
