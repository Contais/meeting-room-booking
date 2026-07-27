<template>
  <div class="contacts-page">
    <div class="contacts-container">
      <!-- 左侧部门树 -->
      <div class="dept-sidebar">
        <div class="sidebar-header">
          <h3>组织架构</h3>
        </div>
        <div class="dept-tree-wrapper">
          <el-tree
            ref="deptTreeRef"
            :data="deptTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            default-expand-all
            highlight-current
            :expand-on-click-node="false"
            @node-click="handleDeptClick"
          >
            <template #default="{ data }">
              <div class="dept-node">
                <el-icon class="dept-icon"><OfficeBuilding /></el-icon>
                <span class="dept-name">{{ data.name }}</span>
                <el-tag v-if="getDeptUserCount(data.id) > 0" size="small" type="info" effect="light" round>
                  {{ getDeptUserCount(data.id) }}
                </el-tag>
              </div>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 右侧用户列表 -->
      <div class="contacts-content">
        <div class="content-header">
          <h2 class="page-title">{{ currentDeptName || '全部人员' }}</h2>
          <div class="search-bar">
            <el-input
              v-model="keyword"
              placeholder="搜索姓名、用户名、手机号、邮箱"
              clearable
              style="width: 320px"
              @input="onSearchInput"
              @keyup.enter="loadContacts"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
        </div>

        <div v-loading="loading" class="contacts-list">
          <div v-if="groupedUsers.length === 0 && !loading" class="empty-state">
            <el-icon :size="48" color="#cbd5e1"><User /></el-icon>
            <p>暂无匹配的联系人</p>
          </div>

          <template v-else>
            <div v-for="group in groupedUsers" :key="group.deptId" class="contact-group">
              <div class="group-title">
                <el-icon><OfficeBuilding /></el-icon>
                <span>{{ group.deptName }}</span>
                <el-tag size="small" type="info" effect="light" round>{{ group.users.length }}人</el-tag>
              </div>
              <div class="user-grid">
                <div
                  v-for="user in group.users"
                  :key="user.id"
                  class="user-card"
                  @click="showUserDetail(user)"
                >
                  <div class="user-avatar" :style="getAvatarStyle(user)">
                    <template v-if="getAvatarIcon(user)">
                      <el-icon :size="24"><component :is="getAvatarIcon(user)" /></el-icon>
                    </template>
                    <template v-else>
                      {{ getInitial(user) }}
                    </template>
                  </div>
                  <div class="user-info">
                    <h4 class="user-name">{{ user.realName || user.username }}</h4>
                    <p class="user-dept">{{ user.departmentName || '未分配部门' }}</p>
                    <div class="user-contact">
                      <el-tooltip v-if="user.phone" :content="user.phone" placement="top">
                        <span class="contact-item">
                          <el-icon><Phone /></el-icon>
                          <span>{{ user.phone }}</span>
                        </span>
                      </el-tooltip>
                      <el-tooltip v-if="user.email" :content="user.email" placement="top">
                        <span class="contact-item contact-email">
                          <el-icon><Message /></el-icon>
                          <span>{{ user.email }}</span>
                        </span>
                      </el-tooltip>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, markRaw } from 'vue'
import { ElMessage } from 'element-plus'
import { OfficeBuilding, User, Search, Phone, Message } from '@element-plus/icons-vue'
import { listContacts } from '@/api/user'
import { getDepartmentTree } from '@/api/department'
import type { UserInfo } from '@/types/user'
import type { Department } from '@/types/department'

const loading = ref(false)
const contacts = ref<UserInfo[]>([])
const deptTree = ref<Department[]>([])
const keyword = ref('')
const selectedDeptId = ref<number | null>(null)
const currentDeptName = ref('')
let searchTimer: ReturnType<typeof setTimeout> | null = null

const avatarGradients = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
  'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)',
  'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)',
  'linear-gradient(135deg, #a1c4fd 0%, #c2e9fb 100%)',
  'linear-gradient(135deg, #d299c2 0%, #fef9d7 100%)',
  'linear-gradient(135deg, #89f7fe 0%, #66a6ff 100%)',
  'linear-gradient(135deg, #fddb92 0%, #d1fdff 100%)',
]

function parseAvatarData(avatar?: string): { icon: string; gradient: number } {
  if (!avatar) return { icon: '', gradient: 0 }
  try {
    const data = JSON.parse(avatar)
    return { icon: data.icon || '', gradient: data.gradient ?? 0 }
  } catch {
    return { icon: '', gradient: 0 }
  }
}

function getAvatarStyle(user: UserInfo): Record<string, string> {
  const { gradient } = parseAvatarData(user.avatar)
  const idx = (gradient ?? 0) % avatarGradients.length
  return {
    background: avatarGradients[idx],
    color: '#fff',
  }
}

function getAvatarIcon(user: UserInfo): any {
  const { icon } = parseAvatarData(user.avatar)
  if (!icon) return null
  const iconMap: Record<string, any> = markRaw({ User, OfficeBuilding })
  return iconMap[icon] || null
}

function getInitial(user: UserInfo): string {
  return (user.realName || user.username || 'U').charAt(0).toUpperCase()
}

function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(loadContacts, 300)
}

function handleDeptClick(data: any) {
  selectedDeptId.value = data.id
  currentDeptName.value = data.name
  loadContacts()
}

function getDeptUserCount(deptId: number): number {
  return contacts.value.filter(u => u.departmentId === deptId).length
}

const filteredContacts = computed(() => {
  let list = contacts.value
  if (selectedDeptId.value) {
    list = list.filter(u => u.departmentId === selectedDeptId.value)
  }
  return list
})

const groupedUsers = computed(() => {
  const groups: Map<number, { deptId: number; deptName: string; users: UserInfo[] }> = new Map()
  const allUsers = filteredContacts.value

  for (const user of allUsers) {
    const deptId = user.departmentId || 0
    const deptName = user.departmentName || '未分配部门'
    if (!groups.has(deptId)) {
      groups.set(deptId, { deptId, deptName, users: [] })
    }
    groups.get(deptId)!.users.push(user)
  }

  return Array.from(groups.values()).sort((a, b) => a.deptId - b.deptId)
})

async function loadContacts() {
  loading.value = true
  try {
    const res = await listContacts({
      keyword: keyword.value || undefined,
      departmentId: selectedDeptId.value || undefined,
    })
    contacts.value = res.data || []
  } catch { /* */ } finally {
    loading.value = false
  }
}

async function loadDeptTree() {
  try {
    const res = await getDepartmentTree()
    deptTree.value = res.data || []
  } catch { /* */ }
}

function showUserDetail(user: UserInfo) {
  if (user.phone) {
    ElMessage.info(`${user.realName || user.username}：${user.phone}`)
  }
}

onMounted(() => {
  loadDeptTree()
  loadContacts()
})
</script>

<style scoped>
.contacts-page {
  height: 100%;
}

.contacts-container {
  display: flex;
  gap: 20px;
  height: calc(100vh - 120px);
}

/* 左侧部门树 */
.dept-sidebar {
  width: 260px;
  flex-shrink: 0;
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-light);
}
.sidebar-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.dept-tree-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.dept-node {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}
.dept-icon {
  color: var(--primary);
  font-size: 16px;
}
.dept-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 右侧内容区 */
.contacts-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border-light);
  overflow: hidden;
}

.content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-light);
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.contacts-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

.contact-group {
  margin-bottom: 28px;
}
.contact-group:last-child {
  margin-bottom: 0;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-light);
}

.user-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: var(--bg-page);
  border-radius: 12px;
  border: 1px solid var(--border-light);
  cursor: pointer;
  transition: all 0.25s ease;
}
.user-card:hover {
  border-color: var(--primary);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.1);
  transform: translateY(-2px);
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 2px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-dept {
  font-size: 12px;
  color: var(--text-muted);
  margin: 0 0 8px 0;
}

.user-contact {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.contact-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.contact-email {
  max-width: 100%;
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 80px 0;
  color: var(--text-muted);
}
.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}
</style>
