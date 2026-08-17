<template>
  <el-dialog
    v-model="visible"
    title="选择人员"
    width="860px"
    top="6vh"
    class="user-select-dialog"
    :close-on-click-modal="false"
    @open="onOpen"
  >
    <div class="usd-body">
      <!-- 顶部：搜索 + 已选人员 -->
      <el-input
        v-model="keyword"
        placeholder="搜索姓名 / 拼音 / 用户名 / 手机号"
        clearable
        :prefix-icon="Search"
        class="usd-search"
      />
      <div class="usd-search-hint">支持姓名、拼音、简拼（如 zhangsan / zs）、手机号、邮箱</div>
      <div class="usd-selected">
        <template v-if="workingUsers.length">
          <el-tag
            v-for="u in workingUsers"
            :key="u.id"
            closable
            size="small"
            @close="toggleUser(u.id, false)"
          >
            {{ u.realName || u.username }}
          </el-tag>
          <el-button link size="small" type="danger" @click="clearAll">清空</el-button>
        </template>
        <span v-else class="usd-selected-empty">尚未选择人员</span>
      </div>

      <div class="usd-main" v-loading="loading">
        <!-- 左侧：部门树（勾选部门 = 全选该部门含子部门人员） -->
        <div class="usd-dept">
          <div
            class="usd-dept-special"
            :class="{ active: deptFilter === ALL }"
            @click="selectDept(ALL)"
          >
            <el-icon><User /></el-icon>
            <span class="usd-dept-name">全部人员</span>
            <el-tag size="small" type="info" effect="light" round>{{ contacts.length }}</el-tag>
          </div>
          <el-tree
            :data="deptTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            default-expand-all
            :expand-on-click-node="false"
            highlight-current
            ref="deptTreeRef"
            @node-click="(data: Department) => selectDept(data.id)"
          >
            <template #default="{ data }">
              <div class="usd-dept-node">
                <el-checkbox
                  :model-value="isDeptChecked(data.id)"
                  :indeterminate="isDeptIndeterminate(data.id)"
                  @click.stop
                  @change="(val: any) => toggleDept(data.id, !!val)"
                />
                <span class="usd-dept-name">{{ data.name }}</span>
                <el-tag v-if="deptUserCount(data.id) > 0" size="small" type="info" effect="light" round>
                  {{ deptUserCount(data.id) }}
                </el-tag>
              </div>
            </template>
          </el-tree>
          <div
            class="usd-dept-special"
            :class="{ active: deptFilter === UNASSIGNED }"
            @click="selectDept(UNASSIGNED)"
          >
            <el-icon><User /></el-icon>
            <span class="usd-dept-name">未分配人员</span>
            <el-tag v-if="unassignedCount > 0" size="small" type="info" effect="light" round>{{ unassignedCount }}</el-tag>
          </div>
        </div>

        <!-- 右侧：人员复选列表 -->
        <div class="usd-list">
          <div
            v-for="u in rightList"
            :key="u.id"
            class="usd-user"
            :class="{ selected: workingIdSet.has(u.id) }"
            @click="toggleUser(u.id, !workingIdSet.has(u.id))"
          >
            <el-checkbox :model-value="workingIdSet.has(u.id)" @click.stop @change="(val: any) => toggleUser(u.id, !!val)" />
            <UserAvatar :avatar="u.avatar" :username="u.realName || u.username" size="sm" />
            <div class="usd-user-info">
              <span class="usd-user-name">{{ u.realName || u.username }}</span>
              <span class="usd-user-dept">{{ u.departmentName || '未分配部门' }}</span>
            </div>
            <span class="usd-user-meta">{{ u.phone || u.email || '' }}</span>
          </div>
          <div v-if="!rightList.length && !loading" class="usd-empty">
            <el-icon :size="36" color="#cbd5e1"><User /></el-icon>
            <p>暂无匹配人员</p>
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleConfirm">确定{{ workingUsers.length ? `（${workingUsers.length} 人）` : '' }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { ElTree } from 'element-plus'
import { Search, User } from '@element-plus/icons-vue'
import { listContacts } from '@/api/user'
import { getDepartmentTree } from '@/api/department'
import { matchUserByKeyword } from '@/utils/pinyinMatch'
import UserAvatar from '@/components/UserAvatar.vue'
import type { UserInfo } from '@/types/user'
import type { Department } from '@/types/department'

/**
 * 通用人员选择弹窗（飞书风格：左部门树 + 右人员复选列表 + 拼音搜索）
 * <p>
 * 勾选部门节点 = 全选该部门（含子部门）人员；搜索时跨部门全量匹配。
 * 打开时以 props.selectedIds 初始化工作区，点「确定」才回传父组件，取消不生效。
 * </p>
 */
const props = defineProps<{
  modelValue: boolean
  /** 父组件当前已选人员 ID 列表 */
  selectedIds?: string[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'confirm', ids: string[]): void
}>()

const ALL = '__all__'
const UNASSIGNED = '__unassigned__'

const visible = computed({
  get: () => props.modelValue,
  set: (val: boolean) => emit('update:modelValue', val)
})

const loading = ref(false)
const contacts = ref<UserInfo[]>([])
const deptTree = ref<Department[]>([])
const deptTreeRef = ref<InstanceType<typeof ElTree>>()
const keyword = ref('')
/** null=全部；ALL/UNASSIGNED=特殊分组；string=指定部门(含子部门) */
const deptFilter = ref<string>(ALL)
/** 弹窗内工作区已选（确定后才同步父组件） */
const workingIds = ref<string[]>([])

const workingIdSet = computed(() => new Set(workingIds.value))
const workingUsers = computed(() =>
  workingIds.value
    .map(id => contacts.value.find(u => u.id === id))
    .filter((u): u is UserInfo => !!u)
)

function onOpen() {
  workingIds.value = [...(props.selectedIds || [])]
  keyword.value = ''
  deptFilter.value = ALL
  deptTreeRef.value?.setCurrentKey(null)
  if (!contacts.value.length || !deptTree.value.length) {
    loadData()
  }
}

async function loadData() {
  loading.value = true
  try {
    const [usersRes, deptRes] = await Promise.all([listContacts(), getDepartmentTree()])
    contacts.value = usersRes.data || []
    deptTree.value = deptRes.data || []
  } catch { /* */ } finally {
    loading.value = false
  }
}

/** 在部门树中查找节点 */
function findDeptNode(nodes: Department[], id: string): Department | null {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children) {
      const found = findDeptNode(node.children, id)
      if (found) return found
    }
  }
  return null
}

/** 收集部门及其所有子部门 ID */
function collectDeptIds(id: string): string[] {
  const node = findDeptNode(deptTree.value, id)
  if (!node) return [id]
  const ids: string[] = [node.id]
  const walk = (n: Department) => {
    if (n.children) {
      for (const child of n.children) {
        ids.push(child.id)
        walk(child)
      }
    }
  }
  walk(node)
  return ids
}

/** 指定部门（含子部门）下的用户 */
function deptUsers(id: string): UserInfo[] {
  const deptIds = new Set(collectDeptIds(id))
  return contacts.value.filter(u => u.departmentId != null && deptIds.has(u.departmentId))
}

/** 部门人数（仅本部门，用于树上角标） */
const deptCountMap = computed(() => {
  const map = new Map<string, number>()
  for (const u of contacts.value) {
    if (u.departmentId != null) {
      map.set(u.departmentId, (map.get(u.departmentId) || 0) + 1)
    }
  }
  return map
})

function deptUserCount(id: string): number {
  return deptCountMap.value.get(id) || 0
}

const unassignedCount = computed(() =>
  contacts.value.filter(u => u.departmentId == null).length
)

/** 右侧列表：搜索时跨部门全量匹配；否则按左侧选中部门范围 */
const rightList = computed(() => {
  if (keyword.value.trim()) {
    return contacts.value.filter(u => matchUserByKeyword(u, keyword.value))
  }
  if (deptFilter.value === UNASSIGNED) {
    return contacts.value.filter(u => u.departmentId == null)
  }
  if (deptFilter.value !== ALL) {
    return deptUsers(deptFilter.value)
  }
  return contacts.value
})

function selectDept(id: string) {
  keyword.value = ''
  deptFilter.value = id
}

/** 部门勾选状态：全部人员在选 → checked；部分在选 → indeterminate */
function isDeptChecked(id: string): boolean {
  const users = deptUsers(id)
  return users.length > 0 && users.every(u => workingIdSet.value.has(u.id))
}

function isDeptIndeterminate(id: string): boolean {
  const users = deptUsers(id)
  const selected = users.filter(u => workingIdSet.value.has(u.id)).length
  return selected > 0 && selected < users.length
}

/** 勾选/取消部门：全选或全移除该部门（含子部门）人员 */
function toggleDept(id: string, checked: boolean) {
  const ids = deptUsers(id).map(u => u.id)
  const set = new Set(workingIds.value)
  for (const uid of ids) {
    if (checked) set.add(uid)
    else set.delete(uid)
  }
  workingIds.value = Array.from(set)
}

/** 切换单个人员选中状态 */
function toggleUser(id: string, checked: boolean) {
  const set = new Set(workingIds.value)
  if (checked) set.add(id)
  else set.delete(id)
  workingIds.value = Array.from(set)
}

function clearAll() {
  workingIds.value = []
}

function handleConfirm() {
  emit('confirm', [...workingIds.value])
  visible.value = false
}
</script>

<style scoped>
.usd-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.usd-search {
  flex-shrink: 0;
}
.usd-search-hint {
  font-size: 12px;
  color: var(--text-muted, #909399);
  margin-top: -4px;
}

/* 已选人员 */
.usd-selected {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 32px;
  padding: 8px 12px;
  background: var(--bg-page, #f5f7fa);
  border-radius: 8px;
}
.usd-selected-empty {
  font-size: 13px;
  color: var(--text-muted, #909399);
}

.usd-main {
  display: flex;
  gap: 12px;
  height: 440px;
}

/* 左侧部门树 */
.usd-dept {
  width: 240px;
  flex-shrink: 0;
  border: 1px solid var(--border-light, #ebeef5);
  border-radius: 8px;
  overflow-y: auto;
  padding: 8px;
}
.usd-dept-special {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  padding: 7px 10px;
  margin: 2px 0;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}
.usd-dept-special:hover {
  background: var(--bg-page, #f5f7fa);
}
.usd-dept-special.active {
  background: var(--primary, #4f6ef7);
  color: #fff;
}
.usd-dept-special.active :deep(.el-tag) {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  border-color: transparent;
}
.usd-dept-node {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  flex: 1;
  min-width: 0;
}
.usd-dept-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.usd-dept :deep(.el-tree) {
  --el-tree-node-content-height: 32px;
  background: transparent;
}
.usd-dept :deep(.el-tree-node__content) {
  border-radius: 6px;
}

/* 右侧人员列表 */
.usd-list {
  flex: 1;
  border: 1px solid var(--border-light, #ebeef5);
  border-radius: 8px;
  overflow-y: auto;
  padding: 6px;
}
.usd-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.usd-user:hover {
  background: var(--bg-page, #f5f7fa);
}
.usd-user.selected {
  background: var(--el-color-primary-light-9, #eef2ff);
}
.usd-user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}
.usd-user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary, #303133);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.usd-user-dept {
  font-size: 12px;
  color: var(--text-muted, #909399);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.usd-user-meta {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--text-secondary, #909399);
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.usd-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 100%;
  color: var(--text-muted, #909399);
  font-size: 13px;
}
</style>
