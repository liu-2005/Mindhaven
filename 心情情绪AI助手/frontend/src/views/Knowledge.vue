<template>
  <div class="page-container">
    <PageHeader title="知识文章">
      <template #extra>
        <el-button type="primary" @click="handleAdd">新增</el-button>
      </template>
    </PageHeader>

    <SearchBar :model="query" @search="handleSearch" @reset="handleReset">
      <el-form-item label="文章标题">
        <el-input v-model="query.title" placeholder="请输入标题" clearable style="width: 200px;" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="query.categoryId" placeholder="请选择分类" clearable style="width: 220px;">
          <el-option
            v-for="item in categoryList"
            :key="item.id"
            :label="item.name || item.categoryName || item.title || item.label || item.text || `分类${item.id}`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="请选择状态" style="width: 160px;">
          <el-option label="全部" value="" />
          <el-option label="草稿" value="0" />
          <el-option label="已发布" value="1" />
          <el-option label="已下线" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="作者">
        <el-input v-model="query.authorName" placeholder="请输入作者" clearable style="width: 160px;" @keyup.enter="handleSearch" />
      </el-form-item>
    </SearchBar>

    <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%; margin-top: 16px;">
      <el-table-column prop="title" label="文章标题" min-width="200" show-overflow-tooltip fixed="left" />
      <el-table-column prop="categoryName" label="分类" width="130" align="center">
        <template #default="{ row }">
          {{ row.categoryName || row.category?.name || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="authorName" label="作者" width="130" align="center">
        <template #default="{ row }">
          {{ row.authorName || row.author?.name || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="阅读量" width="100" align="center">
        <template #default="{ row }">
          {{ row.readCount || row.read_count || row.viewCount || row.view_count || 0 }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180" align="center">
        <template #default="{ row }">
          {{ formatTime(row.createTime || row.createdAt || row.create_time || row.gmtCreate || row.gmt_create || row.createdTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            size="small"
            :type="isPublished(row.status) ? 'warning' : 'success'"
            @click="handleToggleStatus(row)"
          >
            {{ isPublished(row.status) ? '下线' : '发布' }}
          </el-button>
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </div>

    <ArticleFormDialog
      v-model="dialogVisible"
      :type="dialogType"
      :article="editingArticle"
      :category-list="categoryList"
      @submit-success="fetchList"
    />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import SearchBar from '@/components/SearchBar.vue'
import ArticleFormDialog from '@/components/ArticleFormDialog.vue'
import {
  getArticlePage,
  getCategoryTree,
  getArticleDetail,
  updateArticleStatus,
  deleteArticle,
} from '@/api/knowledge'

const query = reactive({
  title: '',
  categoryId: '',
  status: '',
  authorName: '',
})

const pagination = reactive({
  currentPage: 1,
  size: 10,
  total: 0,
})

const loading = ref(false)
const tableData = ref([])
const categoryList = ref([])

const dialogVisible = ref(false)
const dialogType = ref('add')
const editingArticle = ref({})

const flattenCategory = (list) => {
  const result = []
  const walk = (arr) => {
    if (!Array.isArray(arr)) return
    arr.forEach((item) => {
      result.push(item)
      if (item.children && item.children.length) walk(item.children)
    })
  }
  walk(list)
  return result
}

const fetchCategory = async () => {
  try {
    const res = await getCategoryTree()
    let list = []
    if (Array.isArray(res)) list = res
    else if (Array.isArray(res.data)) list = res.data
    else if (res.data && Array.isArray(res.data.list)) list = res.data.list
    else if (res.data && Array.isArray(res.data.records)) list = res.data.records
    categoryList.value = flattenCategory(list)
  } catch (e) {
    console.error('获取分类失败', e)
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      title: query.title || undefined,
      categoryId: query.categoryId || undefined,
      status: query.status === '' ? undefined : query.status,
      authorName: query.authorName || undefined,
      currentPage: pagination.currentPage,
      size: pagination.size,
    }
    const res = await getArticlePage(params)
    const data = res.data || res
    tableData.value = data.records || data.list || data.rows || data || []
    pagination.total = data.total ?? (tableData.value.length)
  } catch (e) {
    console.error('获取列表失败', e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  fetchList()
}

const handleReset = () => {
  query.title = ''
  query.categoryId = ''
  query.status = ''
  query.authorName = ''
  pagination.currentPage = 1
  fetchList()
}

const statusText = (s) => {
  const map = { 0: '草稿', '0': '草稿', 1: '已发布', '1': '已发布', 2: '已下线', '2': '已下线' }
  return map[s] || '未知'
}
const statusTagType = (s) => {
  if (s === 0 || s === '0') return 'info'
  if (s === 1 || s === '1') return 'success'
  if (s === 2 || s === '2') return 'warning'
  return 'info'
}
const isPublished = (s) => s == 1

const formatTime = (val) => {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d.getTime())) return String(val)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const handleToggleStatus = async (row) => {
  const current = isPublished(row.status)
  const next = current ? 2 : 1
  const actionText = current ? '下线' : '发布'
  try {
    await ElMessageBox.confirm(`确定要${actionText}该文章吗？`, '提示', { type: 'warning' })
    await updateArticleStatus(row.id, next)
    ElMessage.success(`${actionText}成功`)
    fetchList()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该文章吗？此操作不可恢复。', '警告', { type: 'warning', confirmButtonClass: 'el-button--danger' })
    await deleteArticle(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleAdd = () => {
  dialogType.value = 'add'
  editingArticle.value = {}
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogType.value = 'edit'
  dialogVisible.value = true
  editingArticle.value = { ...row }
  getArticleDetail(row.id).then((res) => {
    const detail = res.data || res
    editingArticle.value = { ...editingArticle.value, ...detail }
  }).catch((e) => {
    console.error('获取文章详情失败', e)
  })
}

onMounted(() => {
  fetchCategory()
  fetchList()
})
</script>

<style lang="scss" scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
