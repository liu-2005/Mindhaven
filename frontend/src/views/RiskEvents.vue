<template>
  <div>
    <h2>风险工作台</h2>
    <p class="hint">列表仅展示风险摘要。查看详情需要额外权限和访问理由，处置操作会记录审计。</p>
    <div class="filters">
      <el-select v-model="level" clearable placeholder="风险等级" style="width:160px" @change="search"><el-option v-for="(name, value) in levels" :key="value" :label="name" :value="value" /></el-select>
      <el-select v-model="status" clearable placeholder="处置状态" style="width:160px" @change="search"><el-option v-for="(name, value) in statuses" :key="value" :label="name" :value="value" /></el-select>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table :data="items" v-loading="loading" empty-text="暂无符合条件的风险事件">
      <el-table-column prop="id" label="事件" width="80" />
      <el-table-column label="风险" width="100"><template #default="{ row }"><el-tag :type="row.riskLevel === 'CRISIS' ? 'danger' : 'warning'">{{ levels[row.riskLevel] || row.riskLevel }}</el-tag></template></el-table-column>
      <el-table-column prop="redactedSummary" label="摘要" min-width="220" />
      <el-table-column label="状态" width="100"><template #default="{ row }">{{ statuses[row.status] }}</template></el-table-column>
      <el-table-column prop="detectedAt" label="发现时间" width="190" />
      <el-table-column label="操作" width="150"><template #default="{ row }"><el-button v-if="canReadDetail" link @click="view(row)">详情</el-button><el-button v-if="canResolve" link type="primary" @click="openResolve(row)">处置</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="20" :total="total" layout="prev, pager, next, total" @current-change="load" />
    <el-dialog v-model="detailVisible" title="风险事件详情" width="min(560px, 92vw)"><template v-if="detail"><p>事件 {{ detail.id }} · {{ levels[detail.riskLevel] }}</p><p>{{ detail.redactedSummary }}</p><p>模型：{{ detail.modelVersion || '未记录' }}</p><p>处置记录：{{ detail.resolutionNote || '暂无' }}</p></template></el-dialog>
    <el-dialog v-model="resolveVisible" title="记录风险处置" width="min(560px, 92vw)">
      <el-select v-model="nextStatus"><el-option v-for="(name, value) in statuses" :key="value" :value="value" :label="name" /></el-select>
      <el-input v-model="note" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="填写处置说明；请勿复制完整对话或个人敏感信息" />
      <template #footer><el-button @click="resolveVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="resolve">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRiskEvents, getRiskDetail, updateRiskStatus } from '@/api/risk'
const levels = { CONCERN: '需关注', WARNING: '高风险', CRISIS: '紧急风险', NORMAL: '正常' }
const statuses = { OPEN: '待处理', REVIEWING: '处理中', RESOLVED: '已处置', FALSE_POSITIVE: '已判误报' }
let user = {}; try { user = JSON.parse(localStorage.getItem('userInfo') || '{}') } catch {}
const canReadDetail = computed(() => (user.permissions || []).includes('risk:read-sensitive'))
const canResolve = computed(() => (user.permissions || []).includes('risk:resolve'))
const items = ref([]), page = ref(1), total = ref(0), level = ref(''), status = ref(''), loading = ref(false)
const detail = ref(null), detailVisible = ref(false), resolveVisible = ref(false), selectedId = ref(null), nextStatus = ref('REVIEWING'), note = ref(''), saving = ref(false)
let requestId = 0
async function load() {
  const id = ++requestId; loading.value = true
  try { const response = await getRiskEvents({ page: page.value, size: 20, level: level.value || undefined, status: status.value || undefined }); if (id !== requestId) return; items.value = response.data.items; total.value = response.data.total }
  catch {} finally { if (id === requestId) loading.value = false }
}
function search() { page.value = 1; load() }
async function view(row) {
  let reason
  try { reason = (await ElMessageBox.prompt('请填写本次查看的工作理由（5–200字）', '访问理由', { inputValidator: value => value && value.trim().length >= 5 && value.length <= 200 || '请填写5–200字的理由' })).value } catch { return }
  try { detail.value = (await getRiskDetail(row.id, reason.trim())).data; detailVisible.value = true } catch {}
}
function openResolve(row) { selectedId.value = row.id; nextStatus.value = row.status === 'OPEN' ? 'REVIEWING' : row.status; note.value = ''; resolveVisible.value = true }
async function resolve() {
  if (!note.value.trim()) { ElMessage.warning('请填写处置说明'); return }
  if (saving.value) return
  saving.value = true
  try { await updateRiskStatus(selectedId.value, { status: nextStatus.value, note: note.value.trim() }); resolveVisible.value = false; ElMessage.success('处置记录已保存'); await load() } catch {} finally { saving.value = false }
}
onMounted(load)
</script>
<style scoped>
.hint { color: #64748b; margin: 16px 0; line-height: 1.6; }.filters { display:flex; flex-wrap:wrap; gap:12px; margin-bottom:20px; } :deep(.el-pagination) { margin-top:20px; } :deep(.el-textarea) { margin-top:16px; } p { margin:12px 0; }
</style>
