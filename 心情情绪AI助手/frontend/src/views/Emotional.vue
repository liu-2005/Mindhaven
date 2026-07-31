<template>
  <div class="page-container">
    <PageHeader title="情绪日志" />

    <SearchBar :model="query" @search="handleSearch" @reset="handleReset">
      <el-form-item label="用户ID">
        <el-input v-model="query.userId" placeholder="请输入用户ID" clearable style="width: 240px;" />
      </el-form-item>
      <el-form-item label="情绪评分">
        <el-select v-model="query.scoreRange" placeholder="请选择评分范围" style="width: 200px;" >
          <el-option label="全部" value="" />
          <el-option label="低分（1-3）" value="low" />
          <el-option label="中分（4-6）" value="mid" />
          <el-option label="高分（7-10）" value="high" />
        </el-select>
      </el-form-item>
    </SearchBar>

    <el-table v-loading="loading" :data="tableData" stripe style="width: 100%; margin-top: 16px;">
      <el-table-column label="ID" width="100" align="center">
        <template #default="{ row }">{{ row.userId ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="头像" width="100" align="center">
        <template #default="{ row }">
          <img :src="getAvatar(row)" class="table-avatar" alt="头像" />
        </template>
      </el-table-column>
      <el-table-column label="记录日期" width="180" align="center">
        <template #default="{ row }">{{ formatTime(row.diaryDate || row.diary_date || row.createdAt || row.createTime || row.create_time) }}</template>
      </el-table-column>
      <el-table-column label="情绪评分" width="280" align="center">
        <template #default="{ row }">
          <div class="star-rating">
            <span
              v-for="n in 10"
              :key="n"
              class="star"
              :class="{ filled: n <= getScore(row) }"
            >★</span>
            
          </div>
        </template>
      </el-table-column>
      <el-table-column label="生活指标" width="140" align="center">
        <template #default="{ row }">
          <div class="tag-list">
            <el-tag
              v-for="(item, idx) in getLifeIndicators(row)"
              :key="idx"
              :type="['primary', 'success', 'warning', 'info', 'danger'][idx % 5]"
              size="small"
              class="indicator-tag"
            >{{ item }}</el-tag>
            <span v-if="!getLifeIndicators(row).length" class="empty-text">-</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="情绪触发因素" width="150" align="center">
        <template #default="{ row }">
          <div class="trigger-text">{{ getTriggers(row) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="日记内容" min-width="260" align="center" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="diary-text">{{ getDiary(row) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" align="center" fixed="right">
        <template #default="{ row }">
          <div class="action-btns"><el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button><el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button></div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        :current-page="pagination.currentPage"
        :page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="情绪日记详情" width="800px" :close-on-click-modal="false" class="detail-dialog">
      <div class="detail-user-info">
        <img :src="getAvatar(currentRow)" class="detail-avatar" alt="头像" />
        <div class="detail-user-meta">
          <div class="detail-user-name">{{ currentRow.nickname || currentRow.username || '-' }}</div>
          <div class="detail-user-detail">
            <span>昵称：{{ currentRow.nickname || '-' }}</span>
            <span style="margin-left: 16px;">用户ID：{{ currentRow.userId ?? '-' }}</span>
          </div>
          <div class="detail-user-detail">
            <span>记录ID：{{ currentRow.id ?? '-' }}</span>
            <span style="margin-left: 16px;">记录日期：{{ formatTime(currentRow.diaryDate || currentRow.diary_date || currentRow.createdAt || currentRow.createTime || currentRow.create_time) }}</span>
          </div>
        </div>
      </div>

      <div class="detail-section-title">情绪状态</div>
      <div class="emotion-status-grid">
        <div class="emotion-status-item">
          <div class="emotion-status-label">情绪评分</div>
          <div class="detail-score">
            <span
              v-for="n in 10"
              :key="n"
              class="star"
              :class="{ filled: n <= getScore(currentRow) }"
            >★</span>
            <span class="score-num">{{ getScore(currentRow) }}/10</span>
          </div>
        </div>
        <div class="emotion-status-item">
          <div class="emotion-status-label">主要情绪</div>
          <el-tag :type="getEmotionTagType(getMainEmotion(currentRow))" size="small">{{ getMainEmotion(currentRow) }}</el-tag>
        </div>
        <div class="emotion-status-item">
          <div class="emotion-status-label">睡眠质量</div>
          <el-tag v-if="currentRow.sleepQuality != null && currentRow.sleepQuality !== ''" type="primary" size="small">{{ currentRow.sleepQuality }}/10</el-tag>
          <span v-else class="empty-text">-</span>
        </div>
        <div class="emotion-status-item">
          <div class="emotion-status-label">压力水平</div>
          <el-tag v-if="currentRow.stressLevel != null && currentRow.stressLevel !== ''" type="warning" size="small">{{ currentRow.stressLevel }}/10</el-tag>
          <span v-else class="empty-text">-</span>
        </div>
      </div>

      <div class="detail-section-title">日记内容</div>
      <div class="diary-sub-item">
        <div class="diary-sub-label">情绪触发因素</div>
        <div class="detail-content-text">{{ getTriggers(currentRow) }}</div>
      </div>
      <div class="diary-sub-item">
        <div class="diary-sub-label">日记内容</div>
        <div class="detail-content-text">{{ getDiary(currentRow) }}</div>
      </div>

      <div class="detail-section-title">AI情绪分析结果</div>
      <div class="emotion-status-grid">
        <div class="emotion-status-item">
          <div class="emotion-status-label">主要情绪</div>
          <el-tag :type="getEmotionTagType(getAiMainEmotion(currentRow))" size="small">{{ getAiMainEmotion(currentRow) }}</el-tag>
        </div>
        <div class="emotion-status-item">
          <div class="emotion-status-label">情绪强度</div>
          <div class="intensity-display">
            <div class="intensity-bar-wrapper">
              <div class="intensity-bar-bg">
                <div class="intensity-bar-fill" :style="{ width: getEmotionIntensity(currentRow) + '%', background: getIntensityColor(currentRow) }"></div>
              </div>
              <span class="intensity-num">{{ getEmotionIntensity(currentRow) }}%</span>
            </div>
          </div>
        </div>
        <div class="emotion-status-item">
          <div class="emotion-status-label">风险等级</div>
          <el-tag :type="getRiskLevelTagType(getAiAnalysis(currentRow)?.riskLevel)" size="small">{{ getRiskLevelText(getAiAnalysis(currentRow)?.riskLevel) }}</el-tag>
        </div>
        <div class="emotion-status-item">
          <div class="emotion-status-label">情绪性质</div>
          <el-tag :type="getEmotionNatureTagType(getEmotionNature(currentRow))" size="small">{{ getEmotionNatureText(getEmotionNature(currentRow)) }}</el-tag>
        </div>
      </div>

      <div class="detail-section-title">专业建议</div>
      <div class="detail-content-text">{{ getProfessionalAdvice(currentRow) }}</div>

      <div class="detail-section-title">风险描述</div>
      <div class="detail-content-text">{{ getRiskDescription(currentRow) }}</div>

      <div class="detail-section-title">改善建议</div>
      <div class="detail-content-text">{{ getImprovementSuggestion(currentRow) }}</div>

      <div class="detail-section-title">时间信息</div>
      <div class="time-info-grid">
        <div class="time-info-item">
          <span class="time-info-label">创建时间</span>
          <span class="time-info-value">{{ formatTime(currentRow.createdAt || currentRow.createTime || currentRow.create_time || currentRow.gmtCreate || currentRow.gmt_create) }}</span>
        </div>
        <div class="time-info-item">
          <span class="time-info-label">更新时间</span>
          <span class="time-info-value">{{ formatTime(currentRow.updatedAt || currentRow.updateTime || currentRow.update_time || currentRow.gmtUpdate || currentRow.gmt_update || currentRow.modifyTime || currentRow.modify_time) }}</span>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { reactive, ref, onMounted } from "vue"
import SearchBar from "@/components/SearchBar.vue"
import PageHeader from "@/components/PageHeader.vue"
import { ElMessage, ElMessageBox } from "element-plus"
import { getEmotionDiaryPage, deleteEmotionDiary } from "@/api/emotionDiary"
import defaultAvatar from "@/assets/images/user.jpg"

const getAvatar = (row) => {
  const url = row.avatar || row.userAvatar || row.user?.avatar || row.headImg || row.profileImage || row.profile_image
  return url || defaultAvatar
}

const formatTime = (val) => {
  if (!val) return "-"
  // 纯日期字符串如 '2026-07-30' 直接返回日期部分
  if (typeof val === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(val)) return val
  const d = new Date(val)
  if (isNaN(d.getTime())) return String(val)
  const pad = (n) => String(n).padStart(2, "0")
  return d.getFullYear() + "-" + pad(d.getMonth() + 1) + "-" + pad(d.getDate()) + " " + pad(d.getHours()) + ":" + pad(d.getMinutes())
}

const getScore = (row) => {
  const val = row.moodScore || row.mood_score || row.score || row.emotionScore || row.emotion_score || 0
  return Math.min(10, Math.max(0, Number(val) || 0))
}

// 解析 AI 情绪分析 JSON 字符串
const getAiAnalysis = (row) => {
  const raw = row.aiEmotionAnalysis || row.ai_emotion_analysis
  if (!raw) return null
  if (typeof raw === 'object') return raw
  try { return JSON.parse(raw) } catch (e) { return null }
}

// 主要情绪：优先取后端字段，否则按评分映射到情绪状态数组（1-10 → 索引 0-9）
const emotionStatusList = ['绝望崩溃', '消沉抑郁', '焦虑烦躁', '低落不悦', '平静淡然', '轻松惬意', '愉悦舒心', '欢欣满足', '兴奋欣喜', '极致幸福']
const getMainEmotion = (row) => {
  const direct = row.dominantEmotion || row.dominant_emotion || row.emotion || row.emotionType || row.mainEmotion || row.primaryEmotion || row.emotion_type || row.main_emotion
  if (direct) return direct
  const score = getScore(row)
  if (score <= 0) return '-'
  return emotionStatusList[Math.min(9, Math.max(0, score - 1))]
}

// 情绪标签类型映射（来自课件.md）
const getEmotionTagType = (emotion) => {
  const emotionTypes = {
    '绝望崩溃': 'danger',
    '消沉抑郁': 'info',
    '焦虑烦躁': 'warning',
    '低落不悦': 'info',
    '平静淡然': 'success',
    '轻松惬意': 'success',
    '愉悦舒心': 'success',
    '欢欣满足': 'success',
    '兴奋欣喜': 'warning',
    '极致幸福': 'danger',
    '开心': 'success',
    '快乐': 'success',
    '平静': 'info',
    '兴奋': 'warning',
    '愤怒': 'danger',
    '悲伤': 'info',
    '焦虑': 'warning'
  }
  return emotionTypes[emotion] || 'info'
}

// AI情绪分析：主要情绪（优先取AI分析JSON中的 primaryEmotion/label）
const getAiMainEmotion = (row) => {
  const ai = getAiAnalysis(row)
  if (ai) {
    const direct = ai.primaryEmotion || ai.label || ai.emotion || ai.mainEmotion
    if (direct) return direct
  }
  return row.dominantEmotion || getMainEmotion(row)
}

// AI情绪分析：情绪强度（0-100 百分比）
const getEmotionIntensity = (row) => {
  const ai = getAiAnalysis(row)
  if (ai && ai.emotionScore != null) return Math.min(100, Math.max(0, Number(ai.emotionScore) || 0))
  const val = row.emotionIntensity || row.emotion_intensity || row.intensity
  return Math.min(100, Math.max(0, Number(val) || 0))
}

// 情绪强度颜色：结合情绪性质判断（正面情绪高强度=绿色，负面情绪高强度=红色）
const getIntensityColor = (row) => {
  const intensity = getEmotionIntensity(row)
  const nature = getEmotionNature(row)
  // 强度低于 40 视为较弱，统一用浅灰
  if (intensity < 40) return '#909399'
  if (nature === '负面') {
    // 负面情绪：强度越高越红
    if (intensity >= 80) return '#f56c6c'
    return '#e6a23c'
  }
  if (nature === '正面') {
    // 正面情绪：强度越高越绿
    if (intensity >= 80) return '#67c23a'
    return '#95d475'
  }
  // 中性：蓝色
  return '#409eff'
}

// 风险等级标签类型（来自课件.md）
const getRiskLevelTagType = (riskLevel) => {
  const riskTagMap = { 0: 'success', 1: 'info', 2: 'warning', 3: 'danger' }
  return riskTagMap[riskLevel] || 'info'
}

// 风险等级文本（来自课件.md）
const getRiskLevelText = (riskLevel) => {
  const riskTextMap = { 0: '正常', 1: '关注', 2: '预警', 3: '危机' }
  if (riskLevel === null || riskLevel === undefined || riskLevel === '') return '未知风险等级'
  return riskTextMap[riskLevel] || '未知风险等级'
}

// 情绪性质：基于 AI 分析的 isNegative 字段
const getEmotionNature = (row) => {
  const ai = getAiAnalysis(row)
  if (ai && ai.isNegative !== undefined && ai.isNegative !== null) return ai.isNegative ? '负面' : '正面'
  if (row.emotionNature != null) return row.emotionNature
  if (row.emotion_nature != null) return row.emotion_nature
  return '-'
}

const getEmotionNatureTagType = (nature) => {
  if (nature === '正面' || nature === '积极') return 'success'
  if (nature === '负面' || nature === '消极') return 'danger'
  return 'info'
}

const getEmotionNatureText = (nature) => {
  if (!nature || nature === '-') return '-'
  return nature
}

// 专业建议（AI 分析中的 suggestion）
const getProfessionalAdvice = (row) => {
  const ai = getAiAnalysis(row)
  if (ai && ai.suggestion) return ai.suggestion
  return row.professionalAdvice || row.professional_advice || row.aiAdvice || row.advice || '-'
}

// 风险描述（AI 分析中的 riskDescription）
const getRiskDescription = (row) => {
  const ai = getAiAnalysis(row)
  if (ai && ai.riskDescription) return ai.riskDescription
  return row.riskDescription || row.risk_description || '-'
}

// 改善建议（AI 分析中的 improvementSuggestions 数组）
const getImprovementSuggestion = (row) => {
  const ai = getAiAnalysis(row)
  if (ai && ai.improvementSuggestions) {
    if (Array.isArray(ai.improvementSuggestions)) return ai.improvementSuggestions.join('；')
    return ai.improvementSuggestions
  }
  return row.improvementSuggestion || row.improvement_suggestion || '-'
}

const getLifeIndicators = (row) => {
  const items = []
  const sq = row.sleepQuality
  const sl = row.stressLevel
  if (sq !== null && sq !== undefined && sq !== "") items.push("睡眠 " + sq + "/10")
  if (sl !== null && sl !== undefined && sl !== "") items.push("压力 " + sl + "/10")
  return items
}

const getTriggers = (row) => {
  return row.emotionTriggers || row.emotion_triggers || row.triggers || row.trigger || "-"
}

const getDiary = (row) => {
  return row.diaryContent || row.diary_content || row.diary || row.content || row.note || "-"
}

const query = reactive({ userId: "", scoreRange: "" })
const pagination = reactive({ currentPage: 1, size: 10, total: 0 })
const loading = ref(false)
const tableData = ref([])

// 详情弹窗
const detailDialogVisible = ref(false)
const currentRow = ref({})

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
  current: pagination.currentPage,
  size: pagination.size,
  userId: query.userId || undefined,
}
if (query.scoreRange) {
  if (query.scoreRange === "low") { params.minMoodScore = 1; params.maxMoodScore = 3 }
  else if (query.scoreRange === "mid") { params.minMoodScore = 4; params.maxMoodScore = 6 }
  else if (query.scoreRange === "high") { params.minMoodScore = 7; params.maxMoodScore = 10 }
}
    console.log('[调试] 请求参数:', params)
    const res = await getEmotionDiaryPage(params)
    console.log('[调试] 接口原始返回:', res)
    const data = res.data || res
    let list = data.records || data.list || data.rows
    if (!Array.isArray(list)) list = []
    tableData.value = list
    pagination.total = data.total ?? list.length
    if (tableData.value.length) console.log("[情绪日志调试] row:", tableData.value[0])
  } catch (e) { console.error("获取失败", e) }
  finally { loading.value = false }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该记录吗？', '警告', { type: 'warning' })
    await deleteEmotionDiary(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') console.error('删除失败', e)
  }
}

const handleViewDetail = (row) => {
  currentRow.value = row
  detailDialogVisible.value = true
}

const handlePageChange = (page) => { console.log('[分页] 切换到第', page, '页'); pagination.currentPage = page; fetchList() }
const handleSizeChange = (size) => { console.log('[分页] 每页', size, '条'); pagination.size = size; pagination.currentPage = 1; fetchList() }
const handleSearch = () => { pagination.currentPage = 1; fetchList() }
const handleReset = () => { query.userId = ""; query.scoreRange = ""; pagination.currentPage = 1; fetchList() }
onMounted(() => { fetchList() })
</script>
<style lang="scss" scoped>
.star-rating { display: flex; align-items: center; gap: 2px; justify-content: center; }
.star { font-size: 20px; color: #e4e7ed; transition: color 0.2s; }
.star.filled { color: #f7ba2a; }
.score-num { margin-left: 6px; font-size: 13px; color: #606266; font-weight: 500; }
.tag-list { display: flex; flex-wrap: wrap; gap: 6px; justify-content: center; }
.indicator-tag { margin: 0; }
.empty-text { color: #c0c4cc; }
.trigger-text { text-align: center; font-size: 13px; color: #606266; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.diary-text { text-align: center; font-size: 13px; color: #606266; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 20px; }
:deep(.el-table__body tr td) { padding-top: 14px; padding-bottom: 14px; }
.table-avatar { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; }
.action-btns { display: flex; gap: 20px; justify-content: center; }
.detail-user-info { display: flex; align-items: center; gap: 16px; padding-bottom: 16px; border-bottom: 1px solid #ebeef5; }
.detail-avatar { width: 56px; height: 56px; border-radius: 50%; object-fit: cover; }
.detail-user-meta { flex: 1; }
.detail-user-name { font-size: 16px; font-weight: 600; color: #303133; margin-bottom: 6px; }
.detail-user-detail { font-size: 13px; color: #606266; line-height: 1.8; }
.detail-section-title { font-size: 14px; font-weight: 600; color: #303133; margin: 18px 0 10px; padding-left: 8px; border-left: 3px solid #409eff; }
.emotion-status-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.emotion-status-item { background: #f5f7fa; padding: 12px 16px; border-radius: 4px; }
.emotion-status-label { font-size: 12px; color: #909399; margin-bottom: 8px; }
.detail-score { display: flex; align-items: center; gap: 2px; }
.detail-score .star { font-size: 20px; }
.detail-content-text { font-size: 14px; color: #606266; line-height: 1.8; background: #f5f7fa; padding: 12px 16px; border-radius: 4px; white-space: pre-wrap; word-break: break-word; }
.diary-sub-item { margin-bottom: 12px; }
.diary-sub-label { font-size: 12px; color: #909399; margin-bottom: 6px; }
.intensity-display { display: flex; align-items: center; }
.intensity-bar-wrapper { display: flex; align-items: center; gap: 8px; width: 100%; }
.intensity-bar-bg { flex: 1; height: 16px; background: #ebeef5; border-radius: 8px; overflow: hidden; }
.intensity-bar-fill { height: 100%; border-radius: 8px; transition: width 0.3s ease; }
.intensity-num { font-size: 13px; color: #606266; font-weight: 500; white-space: nowrap; min-width: 36px; text-align: right; }
.time-info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.time-info-item { background: #f5f7fa; padding: 12px 16px; border-radius: 4px; display: flex; flex-direction: column; gap: 6px; }
.time-info-label { font-size: 12px; color: #909399; }
.time-info-value { font-size: 13px; color: #303133; }
:deep(.detail-dialog .el-dialog__body) { max-height: 60vh; overflow-y: auto; padding-right: 12px; }
:deep(.detail-dialog .el-dialog__body::-webkit-scrollbar) { width: 6px; }
:deep(.detail-dialog .el-dialog__body::-webkit-scrollbar-thumb) { background: #c0c4cc; border-radius: 3px; }
:deep(.detail-dialog .el-dialog__body::-webkit-scrollbar-track) { background: #f5f7fa; }
</style>
