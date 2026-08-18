<template>
  <div class="page-view">
    <SearchBar @search="onFilterChange" @reset="resetQuery">
      <template #collapsed>
        <el-input v-model="query.keyword" placeholder="搜索标题 / 问题 / 答案 / 标签" clearable @input="onSearchInput" @keyup.enter="onSearchInput" />
      </template>
      <template #expanded>
        <div class="search-item"><label>关键词</label><el-input v-model="query.keyword" placeholder="标题 / 问题 / 答案 / 标签" clearable @input="onSearchInput" @keyup.enter="onSearchInput" /></div>
        <div class="search-item"><label>分类</label><el-select v-model="query.category" placeholder="全部" clearable @change="onFilterChange"><el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" /></el-select></div>
        <div class="search-item"><label>状态</label><el-select v-model="query.status" placeholder="全部" clearable @change="onFilterChange"><el-option label="启用" :value="1" /><el-option label="禁用" :value="0" /></el-select></div>
      </template>
    </SearchBar>

    <div ref="tableCardRef" class="table-card-fullscreen">
      <TableCard :total="total" v-model:page="query.page" v-model:size="query.size" @size-change="onSizeChange" @current-change="loadData">
      <template #toolbar-left>
        <el-button class="btn-outline" @click="showCreateDialog"><el-icon><Plus /></el-icon>新增条目</el-button>
      </template>
      <template #toolbar-right>
        <TableToolbarActions :fullscreen-target="tableCardRef" v-model:sort-order="sortOrder" :columns="columnOptions" v-model:visible-columns="visibleColumns" @refresh="loadData" />
      </template>

      <el-table :data="displayData" v-loading="loading" empty-text="暂无知识条目，点击左上角「新增条目」创建">
        <el-table-column type="index" :index="(index: number) => (query.page - 1) * query.size + index + 1" label="序号" width="70" align="center" />
        <el-table-column v-if="isColumnVisible('title')" prop="title" label="标题 / 来源" min-width="160" show-overflow-tooltip />
        <el-table-column v-if="isColumnVisible('category')" label="分类" width="110" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small" effect="light" round>{{ row.categoryName || row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isColumnVisible('question')" prop="question" label="常见问法" min-width="220" show-overflow-tooltip />
        <el-table-column v-if="isColumnVisible('tags')" label="标签" min-width="140">
          <template #default="{ row }">
            <span v-if="row.tags" class="tag-text">{{ row.tags }}</span>
            <span v-else style="color: var(--text-muted)">-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="isColumnVisible('sort')" prop="sort" label="排序" width="70" align="center" />
        <el-table-column v-if="isColumnVisible('status')" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small" effect="light">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isColumnVisible('updateTime')" label="更新时间" width="170"><template #default="{ row }">{{ formatDateTime(row.updateTime || row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-links">
              <el-button type="primary" link @click="showDetail(row)">
                <el-icon><View /></el-icon>详情
              </el-button>
              <el-button type="primary" link @click="showEditDialog(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
              <el-dropdown trigger="click" popper-class="action-menu-popper" @command="(cmd: string) => handleRowCommand(cmd, row)">
                <el-button type="primary" link>
                  更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="toggle">
                      <el-icon><Switch /></el-icon>{{ row.status === 1 ? '禁用' : '启用' }}
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided class="danger-item">
                      <el-icon><Delete /></el-icon>删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
      </TableCard>
    </div>

    <FormDrawer v-model:visible="dialogVisible" :title="isEdit ? '编辑知识条目' : '新增知识条目'" :loading="submitting" @submit="handleSubmit">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="form-standard">
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width:100%">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="条目标题 / 来源" prop="title">
          <el-input v-model="form.title" placeholder="如：预约规则·提前预约天数" />
        </el-form-item>
        <el-form-item label="常见问法" prop="question">
          <el-input v-model="form.question" type="textarea" :rows="2" placeholder="用户可能提出的问题" />
        </el-form-item>
        <el-form-item label="答案内容" prop="answer">
          <el-input v-model="form.answer" type="textarea" :rows="6" placeholder="基于事实的答案，回答时会注明本条来源" />
        </el-form-item>
        <el-form-item label="标签（选填）">
          <el-input v-model="form.tags" placeholder="逗号分隔，如：提前,预约,天数" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" style="width:180px" />
        </el-form-item>
        <el-form-item label="状态" v-if="isEdit">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </FormDrawer>

    <el-drawer v-model="detailVisible" title="知识条目详情" size="520px" direction="rtl" destroy-on-close>
      <div v-if="currentDetail" class="detail-body">
        <div class="detail-head">
          <h3 class="detail-title">{{ currentDetail.title }}</h3>
          <el-tag :type="currentDetail.status === 1 ? 'success' : 'warning'" size="small" effect="light">
            {{ currentDetail.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="分类">
            <el-tag type="info" size="small" effect="light" round>{{ currentDetail.categoryName || currentDetail.category }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="常见问法">{{ currentDetail.question }}</el-descriptions-item>
          <el-descriptions-item label="标签">{{ currentDetail.tags || '-' }}</el-descriptions-item>
          <el-descriptions-item label="排序">{{ currentDetail.sort }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(currentDetail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDateTime(currentDetail.updateTime || currentDetail.createTime) }}</el-descriptions-item>
        </el-descriptions>
        <div class="detail-answer">
          <div class="detail-answer-label">答案内容</div>
          <div class="detail-answer-text">{{ currentDetail.answer }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="editFromDetail">编辑</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Switch, ArrowDown, View } from '@element-plus/icons-vue'
import { listKnowledge, createKnowledge, updateKnowledge, toggleKnowledgeStatus, deleteKnowledge } from '@/api/knowledge'
import SearchBar from '@/components/SearchBar.vue'
import TableCard from '@/components/TableCard.vue'
import TableToolbarActions from '@/components/TableToolbarActions.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import { sortByProperty } from '@/utils/table'
import { useTableToolbar } from '@/composables/useTableToolbar'
import { formatDateTime } from '@/utils/datetime'
import type { TableColumnOption } from '@/types/table'
import type { KnowledgeEntry } from '@/types/knowledge'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<KnowledgeEntry[]>([])
const tableCardRef = ref<HTMLDivElement>()
const columnOptions: TableColumnOption[] = [
  { key: 'title', label: '标题 / 来源' },
  { key: 'category', label: '分类' },
  { key: 'question', label: '常见问法' },
  { key: 'tags', label: '标签' },
  { key: 'sort', label: '排序' },
  { key: 'status', label: '状态' },
  { key: 'updateTime', label: '更新时间' }
]
const { sortOrder, visibleColumns, isColumnVisible } = useTableToolbar(columnOptions)
const displayData = computed(() => sortByProperty(tableData.value, sortOrder.value, (row) => row.sort ?? 0))
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const detailVisible = ref(false)
const currentDetail = ref<KnowledgeEntry | null>(null)

const categories = [
  { value: 'RULES', label: '预约规则' },
  { value: 'FLOW', label: '流程指引' },
  { value: 'EXCEPTION', label: '异常处理' },
  { value: 'ANNOUNCEMENT', label: '公告运营' },
]

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  category: undefined as string | undefined,
  status: undefined as number | undefined,
})

const form = reactive({
  id: undefined as string | undefined,
  category: '',
  title: '',
  question: '',
  answer: '',
  tags: '',
  sort: 0,
  status: 1,
})

const rules: FormRules = {
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  title: [{ required: true, message: '请输入条目标题', trigger: 'blur' }],
  question: [{ required: true, message: '请输入常见问法', trigger: 'blur' }],
  answer: [{ required: true, message: '请输入答案内容', trigger: 'blur' }],
}

function onSizeChange() { query.page = 1; loadData() }
function onFilterChange() { query.page = 1; loadData() }
let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { query.page = 1; loadData() }, 300)
}

function resetQuery() {
  query.keyword = ''
  query.category = undefined
  query.status = undefined
  query.page = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.category) params.category = query.category
    if (query.status != null) params.status = query.status
    const res = await listKnowledge(params)
    tableData.value = res.data.records
    total.value = Number(res.data.total) || 0
  } catch { /* 错误已由拦截器提示 */ } finally {
    loading.value = false
  }
}

function showCreateDialog() {
  isEdit.value = false
  Object.assign(form, { id: undefined, category: '', title: '', question: '', answer: '', tags: '', sort: 0, status: 1 })
  dialogVisible.value = true
}

function showEditDialog(row: KnowledgeEntry) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    category: row.category,
    title: row.title,
    question: row.question,
    answer: row.answer,
    tags: row.tags || '',
    sort: row.sort ?? 0,
    status: row.status,
  })
  dialogVisible.value = true
}

function showDetail(row: KnowledgeEntry) {
  currentDetail.value = row
  detailVisible.value = true
}

function editFromDetail() {
  if (!currentDetail.value) return
  const row = currentDetail.value
  detailVisible.value = false
  showEditDialog(row)
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateKnowledge({
        id: form.id as string,
        category: form.category,
        title: form.title,
        question: form.question,
        answer: form.answer,
        tags: form.tags,
        sort: form.sort,
        status: form.status,
      })
      ElMessage.success('更新成功')
    } else {
      await createKnowledge({
        category: form.category,
        title: form.title,
        question: form.question,
        answer: form.answer,
        tags: form.tags,
        sort: form.sort,
        status: form.status,
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch { /* 错误已由拦截器提示 */ } finally {
    submitting.value = false
  }
}

async function handleToggle(row: KnowledgeEntry) {
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${action}该条目吗？${action === '禁用' ? '禁用后 AI 检索将不再返回该条目。' : ''}`, '提示', { type: 'warning' })
    await toggleKnowledgeStatus(row.id)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch { /* 用户取消 */ }
}

async function handleDelete(row: KnowledgeEntry) {
  try {
    await ElMessageBox.confirm('确定删除该知识条目吗？此操作不可恢复。', '警告', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'error' })
    await deleteKnowledge(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* 用户取消 */ }
}

function handleRowCommand(cmd: string, row: KnowledgeEntry) {
  if (cmd === 'toggle') handleToggle(row)
  else if (cmd === 'delete') handleDelete(row)
}

onMounted(loadData)
</script>

<style scoped>
.tag-text {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.detail-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  word-break: break-all;
}

.detail-answer-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.detail-answer-text {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
