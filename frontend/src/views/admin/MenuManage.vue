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
    <div ref="tableCardRef" class="table-card">
      <!-- 工具栏 -->
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-button class="btn-outline" @click="showCreateDialog()"><el-icon><Plus /></el-icon>添加菜单</el-button>
          <el-button @click="toggleExpandAll">{{ expandAll ? '收起' : '展开' }}</el-button>
        </div>
        <div class="toolbar-right">
          <el-tooltip content="刷新">
            <el-button circle aria-label="刷新" @click="loadData"><el-icon><Refresh /></el-icon></el-button>
          </el-tooltip>
          <el-tooltip :content="sortTooltip">
            <el-button circle aria-label="排序" @click="toggleSort">
              <el-icon><component :is="sortIcon" /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip :content="isFullscreen ? '退出全屏' : '全屏'">
            <el-button circle aria-label="全屏" @click="toggleFullscreen">
              <el-icon><FullScreen /></el-icon>
            </el-button>
          </el-tooltip>
          <el-popover placement="bottom-end" trigger="click" :width="220">
            <template #reference>
              <el-button circle aria-label="列设置">
                <el-icon><Setting /></el-icon>
              </el-button>
            </template>
            <div class="column-settings">
              <div class="column-settings-title">显示列</div>
              <el-checkbox-group v-model="visibleColumns" @change="handleColumnVisibilityChange">
                <el-checkbox v-for="item in columnOptions" :key="item.key" :value="item.key">{{ item.label }}</el-checkbox>
              </el-checkbox-group>
            </div>
          </el-popover>
        </div>
      </div>

      <!-- 树形表格 -->
      <el-table ref="tableRef" :data="displayData" v-loading="loading" empty-text="暂无菜单数据，点击左上角「添加菜单」创建"
        row-key="id" :key="expandAll" :default-expand-all="expandAll"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }">
        <el-table-column v-if="isColumnVisible('name')" prop="name" label="菜单名称" min-width="200" />
        <el-table-column v-if="isColumnVisible('type')" label="菜单类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="!row.path && row.children && row.children.length > 0" size="small" type="info" effect="light">目录</el-tag>
            <el-tag v-else size="small" type="primary" effect="light">菜单</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isColumnVisible('sortOrder')" prop="sortOrder" label="排序号" width="90" align="center" />
        <el-table-column v-if="isColumnVisible('path')" prop="path" label="路由" min-width="150">
          <template #default="{ row }">
            <span v-if="row.path">{{ row.path }}</span>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="isColumnVisible('permission')" label="权限标识" min-width="120">
          <template #default>
            <span style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="isColumnVisible('createTime')" label="创建时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.createTime) }}</template></el-table-column>
        <el-table-column v-if="isColumnVisible('status')" prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small" effect="light">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-links">
              <el-button type="primary" link @click="showCreateDialog(row.id)">
                <el-icon><Plus /></el-icon>添加子菜单
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

    <!-- 新增/编辑抽屉 -->
    <FormDrawer v-model:visible="dialogVisible" :title="isEdit ? '编辑菜单' : '添加菜单'" :loading="submitting" @submit="handleSubmit">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="form-standard">
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
        <el-form-item label="上级菜单"><el-tree-select v-model="form.parentId" :data="treeData" node-key="id" :props="{ label: 'name', children: 'children' }" check-strictly clearable placeholder="留空则为顶级菜单" style="width:100%" /></el-form-item>
        <el-form-item label="排序号"><el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width:180px" /></el-form-item>
        <el-form-item label="是否启用"><el-radio-group v-model="form.status"><el-radio :value="1">启用</el-radio><el-radio :value="0">禁用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="是否显示"><el-radio-group v-model="form.visible"><el-radio :value="1">显示</el-radio><el-radio :value="0">隐藏</el-radio></el-radio-group></el-form-item>
      </el-form>
    </FormDrawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, reactive, markRaw } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Edit, Delete, Refresh, Sort, SortUp, SortDown, FullScreen, Setting,
  HomeFilled, User, UserFilled, OfficeBuilding, Calendar,
  Menu, Grid, List, Picture, Camera, Bell, Message,
  Avatar, Coin, Goods, DataLine, Location, Phone,
  Lock, Key, Tools, Box, Folder, FolderOpened, Files, Document,
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
const tableCardRef = ref<HTMLDivElement>()
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const expandAll = ref(true)
const isFullscreen = ref(false)
const filterName = ref('')
const filterPath = ref(''); const filterStatus = ref(undefined as number | undefined)
const createTimeRange = ref<string[]>([])
type SortMode = 'asc' | 'desc'
const sortMode = ref<SortMode>('asc')
const columnOptions = [
  { key: 'name', label: '菜单名称' },
  { key: 'type', label: '菜单类型' },
  { key: 'sortOrder', label: '排序号' },
  { key: 'path', label: '路由' },
  { key: 'permission', label: '权限标识' },
  { key: 'createTime', label: '创建时间' },
  { key: 'status', label: '状态' }
] as const
const visibleColumns = ref<string[]>(columnOptions.map((item) => item.key))
let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() { if (searchTimer) clearTimeout(searchTimer); searchTimer = setTimeout(applyFilter, 300) }

const sortTooltip = computed(() => sortMode.value === 'asc' ? '按排序号升序' : '按排序号降序')
const sortIcon = computed(() => sortMode.value === 'asc' ? SortUp : SortDown)

const form = reactive({ id: undefined as string | undefined, name: '', path: '', icon: '', parentId: undefined as string | undefined, sortOrder: 0, status: 1, visible: 1 })
const rules: FormRules = { name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }] }

const iconMap: Record<string, any> = markRaw({
  HomeFilled, User, UserFilled, OfficeBuilding, Calendar,
  Menu, Grid, List, Picture, Camera, Bell, Message,
  Avatar, Coin, Goods, DataLine, Location, Phone,
  Lock, Key, Tools, Box, Folder, FolderOpened, Files, Document,
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

function cloneAndSortTree(items: MenuItem[], mode: SortMode): MenuItem[] {
  return [...items]
    .map((item) => item.children && item.children.length > 0
      ? { ...item, children: cloneAndSortTree(item.children, mode) }
      : { ...item })
    .sort((a, b) => {
      const sortDiff = (a.sortOrder ?? 0) - (b.sortOrder ?? 0)
      if (sortDiff !== 0) return mode === 'asc' ? sortDiff : -sortDiff
      const timeDiff = (a.createTime || '').localeCompare(b.createTime || '')
      return mode === 'asc' ? timeDiff : -timeDiff
    })
}

const displayData = computed(() => cloneAndSortTree(filteredData.value, sortMode.value))

function toggleSort() {
  sortMode.value = sortMode.value === 'asc' ? 'desc' : 'asc'
}

function isColumnVisible(key: string) {
  return visibleColumns.value.includes(key)
}

function handleColumnVisibilityChange(value: string[]) {
  if (value.length === 0) {
    visibleColumns.value = ['name']
    ElMessage.warning('至少保留一列')
  }
}

function onFullscreenChange() {
  isFullscreen.value = document.fullscreenElement === tableCardRef.value
}

async function toggleFullscreen() {
  try {
    if (isFullscreen.value) {
      await document.exitFullscreen()
    } else if (tableCardRef.value) {
      await tableCardRef.value.requestFullscreen()
    }
  } catch {
    ElMessage.error('全屏切换失败，请检查浏览器权限')
  }
}

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

function showCreateDialog(parentId?: string) {
  isEdit.value = false
  Object.assign(form, { id: undefined, name: '', path: '', icon: '', parentId: parentId || undefined, sortOrder: 0, status: 1, visible: 1 })
  dialogVisible.value = true
}

function showEditDialog(row: MenuItem) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    name: row.name,
    path: row.path || '',
    icon: row.icon || '',
    parentId: !row.parentId || row.parentId === '0' ? undefined : row.parentId,
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
      parentId: form.parentId || '0',
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

async function handleDelete(id: string) {
  try {
    await ElMessageBox.confirm('确定删除该菜单?', '提示', { type: 'warning' })
    await deleteMenu(id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* */ }
}

onMounted(() => {
  loadData()
  document.addEventListener('fullscreenchange', onFullscreenChange)
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', onFullscreenChange)
})
</script>

<style scoped>
.page-view { display: flex; flex-direction: column; gap: 16px; }
.table-card:fullscreen { background: var(--bg-card); overflow: auto; }
.column-settings-title { margin-bottom: 10px; font-size: 13px; font-weight: 600; color: var(--text-primary); }
.column-settings .el-checkbox { display: flex; margin-right: 0; }
.column-settings .el-checkbox + .el-checkbox { margin-top: 4px; }
</style>
