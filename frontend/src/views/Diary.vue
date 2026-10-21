<template>
  <div class="diary-page">
    <!-- 顶部导航 -->
    <header class="topbar">
      <div class="topbar-inner">
        <div class="brand" @click="goHome">
          <img src="@/assets/images/smile.svg" alt="logo" class="brand-logo" />
          <span class="brand-name">心晴 MindHaven</span>
        </div>
        <nav class="nav-links">
          <a class="nav-link" @click="goHome">首页</a>
          <a class="nav-link" @click="goConsultation">AI咨询</a>
          <a class="nav-link active">情绪日记</a>
          <a class="nav-link" @click="goKnowledge">知识库</a>
        </nav>
        <div class="nav-actions">
          <template v-if="!loggedIn">
            <el-button round @click="goLogin">登录</el-button>
            <el-button type="primary" round @click="goRegister">注册</el-button>
          </template>
          <template v-else>
            <span class="nav-username">{{ username }}</span>
            <el-button round @click="handleLogout">退出登录</el-button>
          </template>
        </div>
      </div>
    </header>

    <div class="emotionDiary-container">
      <!-- 头部横幅 -->
      <div class="header-section">
        <div class="header-content">
          <span class="header-icon">📝</span>
          <div>
            <h1>情绪日记</h1>
            <p>记录每一天的心情，看见自己</p>
          </div>
        </div>
      </div>

      <div class="content">
        <div class="diary-card">
          <div class="diary-heading"><h2 class="title">{{ editingId ? '编辑情绪记录' : '今日情绪记录' }}</h2><el-button v-if="editingId" @click="resetForm">写新日记</el-button></div>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top">

            <!-- 记录日期 -->
            <div class="section">
              <p>选择日期</p>
              <el-date-picker v-model="form.diaryDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:220px" />
            </div>

            <!-- 心情评分 -->
            <div class="section">
              <p>今日心情评分</p>
              <div class="mood-row">
                <div class="mood-picker">
                  <span
                    v-for="n in 10"
                    :key="n"
                    class="mood-emoji"
                    :class="{ active: form.moodScore >= n }"
                    @click="form.moodScore = n"
                  >{{ moodEmojis[n - 1] }}</span>
                </div>
                <span class="mood-score-display" :style="{ color: moodScoreColor }">{{ form.moodScore }}/10</span>
              </div>
              <span class="mood-hint" :style="{ color: moodScoreColor }">{{ moodScoreText }}</span>
            </div>

            <!-- 情绪选择卡片 -->
            <div class="section">
              <p>选择主要情绪</p>
              <div class="emotion-grid">
                <div
                  v-for="e in emotionOptions"
                  :key="e.name"
                  class="emotion-card"
                  :class="{ selected: form.dominantEmotion === e.name }"
                  @click="form.dominantEmotion = e.name"
                >
                  <span class="emotion-emoji">{{ e.icon }}</span>
                  <div class="emotion-name">{{ e.name }}</div>
                </div>
              </div>
            </div>

            <!-- 详细表单 -->
            <div class="detail-form">
              <div class="form-label">情绪触发因素</div>
              <el-input v-model="form.emotionTriggers" type="textarea" :rows="2" placeholder="今天因为什么有了这样的心情？" style="margin-bottom:16px" />

              <div class="form-label">今日感想</div>
              <el-input v-model="form.content" type="textarea" :rows="5" placeholder="写下今天的感受和想法..." style="margin-bottom:16px" />

              <div class="life-indicators">
                <div class="indicator-group">
                  <div class="form-label">睡眠质量</div>
                  <el-rate v-model="form.sleepQuality" :max="5" show-text :texts="['很差','较差','一般','不错','很好']" />
                </div>
                <div class="indicator-group">
                  <div class="form-label">压力水平</div>
                  <el-rate v-model="form.stressLevel" :max="5" show-text :texts="['轻松','少许','中等','较大','很大']" />
                </div>
              </div>

              <div class="action-buttons">
                <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit" round>
                  {{ editingId ? '保存修改' : '保存日记' }}
                </el-button>
              </div>
            </div>
          </el-form>
        </div>
        <div class="diary-card" v-loading="historyLoading">
          <div class="diary-heading"><h2 class="title">我的情绪趋势</h2><el-radio-group v-model="trendDays" @change="loadHistory"><el-radio-button :value="7">近7天</el-radio-button><el-radio-button :value="30">近30天</el-radio-button></el-radio-group></div>
          <p v-if="!trends.totalRecords" class="empty-hint">这段时间还没有记录，保存日记后就能看到趋势。</p>
          <template v-else>
            <p class="trend-summary">共记录 {{ trends.totalRecords }} 天 · 平均心情 {{ trends.averageScore }}/10</p>
            <div class="trend-chart" role="img" :aria-label="`近${trendDays}天的心情趋势，平均${trends.averageScore}分`">
              <div v-for="day in trends.daily" :key="day.date" class="trend-day" :title="`${day.date}：${day.recordCount ? day.avgScore + '/10' : '未记录'}`">
                <div class="trend-track"><span v-if="day.recordCount" :style="{ height: `${day.avgScore * 10}%` }"></span></div>
                <small>{{ day.date.slice(5) }}</small>
              </div>
            </div>
            <p class="empty-hint">空白日期表示未记录，不计入平均分。</p>
          </template>
        </div>
        <div class="diary-card" v-loading="historyLoading">
          <div class="diary-heading"><h2 class="title">我的日记</h2><el-button @click="loadHistory">刷新</el-button></div>
          <el-table :data="diaryList" empty-text="还没有日记，写下第一篇吧">
            <el-table-column prop="diaryDate" label="日期" width="120" />
            <el-table-column prop="dominantEmotion" label="情绪" width="90" />
            <el-table-column label="心情" width="90"><template #default="{ row }">{{ row.moodScore }}/10</template></el-table-column>
            <el-table-column prop="content" label="感想" min-width="150" show-overflow-tooltip />
            <el-table-column label="操作" width="170"><template #default="{ row }">
              <el-button link @click="viewDiary(row)">详情</el-button>
              <el-button link type="primary" :disabled="submitting" @click="editDiary(row)">编辑</el-button>
              <el-button link type="danger" :disabled="submitting || deletingId !== null" @click="removeDiary(row)">删除</el-button>
            </template></el-table-column>
          </el-table>
          <el-pagination v-model:current-page="historyPage" :page-size="10" :total="historyTotal" layout="prev, pager, next, total" @current-change="loadHistory" />
        </div>
      </div>
    </div>
    <el-dialog v-model="detailVisible" title="日记详情" width="min(560px, 92vw)">
      <template v-if="detailDiary">
        <p>{{ detailDiary.diaryDate }} · {{ detailDiary.dominantEmotion }} · 心情 {{ detailDiary.moodScore }}/10</p>
        <p>睡眠 {{ detailDiary.sleepQuality ?? '未填写' }}/5 · 压力 {{ detailDiary.stressLevel ?? '未填写' }}/5</p>
        <h4 class="detail-heading">触发因素</h4><p class="diary-text">{{ detailDiary.emotionTriggers || '未填写' }}</p>
        <h4 class="detail-heading">感想</h4><p class="diary-text">{{ detailDiary.content }}</p>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import { logout } from '@/api/user'
import { createDiary, getDiaryPage, getDiaryDetail, updateDiary, deleteDiary, getDiaryTrends } from '@/api/emotionDiary'

const router = useRouter()

const loggedIn = ref(!!localStorage.getItem('token'))
const readUserInfo = () => { try { return JSON.parse(localStorage.getItem('userInfo') || '{}') } catch { return {} } }
const userInfo = readUserInfo()
const username = userInfo.nickname || userInfo.username || '用户'

const moodEmojis = ['😢','😣','😟','😐','🙂','😊','😄','😁','🥰','🌟']

const moodScoreText = computed(() => {
  const s = form.moodScore
  if (!s) return '点击选择心情'
  if (s <= 2) return '心情不太好'
  if (s <= 4) return '有些低落'
  if (s <= 6) return '一般般'
  if (s <= 8) return '还不错'
  return '心情超好'
})

const moodScoreColor = computed(() => {
  const s = form.moodScore
  if (!s) return '#9ca3af'
  if (s <= 3) return '#ef4444'
  if (s <= 5) return '#8b5cf6'
  if (s <= 7) return '#10b981'
  return '#f59e0b'
})

const emotionOptions = [
  { name: '开心', icon: '😄' },
  { name: '平静', icon: '😌' },
  { name: '焦虑', icon: '😰' },
  { name: '难过', icon: '😢' },
  { name: '愤怒', icon: '😡' },
  { name: '感动', icon: '🥹' },
  { name: '期待', icon: '🤩' },
  { name: '疲惫', icon: '😫' },
]

const formRef = ref(null)
const submitting = ref(false)

const localDate = () => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` }
const today = localDate()
const editingId = ref(null)
const diaryList = ref([])
const historyPage = ref(1)
const historyTotal = ref(0)
const historyLoading = ref(false)
const deletingId = ref(null)
const trendDays = ref(7)
const trends = ref({ totalRecords: 0, averageScore: 0, daily: [] })
const detailDiary = ref(null)
const detailVisible = ref(false)
let historyRequest = 0
const form = reactive({
  diaryDate: today,
  moodScore: 0,
  dominantEmotion: '',
  emotionTriggers: '',
  content: '',
  sleepQuality: 0,
  stressLevel: 0,
})

const rules = {
  diaryDate: [{ required: true, message: '请选择日期', trigger: 'blur' }],
  moodScore: [{ required: true, message: '请选择心情', trigger: 'change' }],
  dominantEmotion: [{ required: true, message: '请选择情绪', trigger: 'change' }],
  emotionTriggers: [{ required: true, message: '请填写触发因素', trigger: 'blur' }],
  content: [{ required: true, message: '请写下今日感想', trigger: 'blur' }],
  sleepQuality: [{ required: true, message: '请选择睡眠质量', trigger: 'change' }],
  stressLevel: [{ required: true, message: '请选择压力水平', trigger: 'change' }],
}

const handleSubmit = async () => {
  if (submitting.value) return
  if (!form.diaryDate || !form.dominantEmotion || !form.content.trim() || form.moodScore < 1 || form.moodScore > 10
      || form.sleepQuality < 1 || form.sleepQuality > 5 || form.stressLevel < 1 || form.stressLevel > 5) {
    ElMessage.warning('请填写日期、主要情绪和感想，并选择心情、睡眠和压力评分')
    return
  }
    submitting.value = true
    try {
      const response = editingId.value ? await updateDiary(editingId.value, { ...form }) : await createDiary({ ...form })
      editingId.value = response.data.id
      ElMessage.success('日记保存成功')
      historyPage.value = 1
      await loadHistory()
    } catch (err) {
      if (err?.message?.includes('已有一篇')) {
        ElMessage.warning('该日期已有一篇日记，请修改后再保存')
      } else {
        console.error('[情绪日记] 保存失败：', err)
      }
    } finally {
      submitting.value = false
    }
}

async function loadHistory() {
  const requestId = ++historyRequest
  historyLoading.value = true
  try {
    const [page, trend] = await Promise.all([getDiaryPage({ page: historyPage.value, size: 10 }), getDiaryTrends({ days: trendDays.value })])
    if (requestId !== historyRequest) return
    diaryList.value = page.data.items
    historyTotal.value = page.data.total
    trends.value = trend.data
  } catch { /* Shared request handler shows the error. */ }
  finally { if (requestId === historyRequest) historyLoading.value = false }
}
function resetForm() {
  editingId.value = null
  Object.assign(form, { diaryDate: localDate(), moodScore: 0, dominantEmotion: '', emotionTriggers: '', content: '', sleepQuality: 0, stressLevel: 0 })
  formRef.value?.clearValidate()
}
async function viewDiary(row) {
  try { detailDiary.value = (await getDiaryDetail(row.id)).data; detailVisible.value = true } catch {}
}
async function editDiary(row) {
  try {
    const data = (await getDiaryDetail(row.id)).data
    editingId.value = data.id
    for (const key of Object.keys(form)) form[key] = data[key] ?? (['sleepQuality', 'stressLevel'].includes(key) ? 0 : '')
    await nextTick()
    formRef.value?.$el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  } catch {}
}
async function removeDiary(row) {
  try { await ElMessageBox.confirm(`确定删除 ${row.diaryDate} 的日记吗？`, '删除日记', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }) } catch { return }
  deletingId.value = row.id
  try {
    await deleteDiary(row.id)
    if (editingId.value === row.id) resetForm()
    if (diaryList.value.length === 1 && historyPage.value > 1) historyPage.value--
    await loadHistory()
    ElMessage.success('日记已删除')
  } catch {} finally { deletingId.value = null }
}
onMounted(() => { if (loggedIn.value) loadHistory() })

const goHome = () => router.push('/')
const goConsultation = () => router.push('/consultation')
const goKnowledge = () => router.push('/knowledge')
const goLogin = () => router.push('/auth/login')
const goRegister = () => router.push('/auth/register')

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
  }).then(async () => {
    const loading = ElLoading.service({ fullscreen: true, text: '正在退出...' })
    try { await logout() } catch (e) { console.error('登出失败', e) }
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    loading.close()
    ElMessage.success('已退出登录')
    router.push('/auth/login')
  }).catch(() => {})
}
</script>

<style lang="scss" scoped>
$primary: #14b8a6;
$primary-dark: #0d9488;
$accent: #f97316;
$text-dark: #1f2937;
$text-body: #4b5563;
$text-muted: #9ca3af;

.diary-page {
  min-height: 100vh;
  background: #f9fafb;
}
.diary-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.trend-summary, .empty-hint { margin: 12px 0; color: #64748b; line-height: 1.6; }
.trend-chart { display: flex; gap: 8px; overflow-x: auto; padding: 12px 0; }
.trend-day { min-width: 42px; flex: 1; text-align: center; color: #64748b; }
.trend-track { height: 100px; background: #f1f5f9; border-radius: 6px; display: flex; align-items: flex-end; overflow: hidden; margin-bottom: 6px; }
.trend-track span { display: block; width: 100%; background: #14b8a6; border-radius: 6px; }
.diary-text { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.8; }
.detail-heading { margin: 18px 0 8px; }
:deep(.el-pagination) { margin-top: 20px; justify-content: center; flex-wrap: wrap; }

.topbar {
  height: 64px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  position: sticky; top: 0; z-index: 10;
}
.topbar-inner {
  max-width: 1400px; height: 100%;
  margin: 0 auto; padding: 0 28px;
  display: flex; align-items: center; justify-content: space-between;
}
.brand { display: flex; align-items: center; gap: 12px; cursor: pointer; }
.brand-logo { width: 34px; height: 34px; }
.brand-name {
  font-size: 20px; font-weight: 800;
  background: linear-gradient(135deg, $primary, $accent);
  -webkit-background-clip: text; background-clip: text; color: transparent;
}
.nav-links { display: flex; gap: 32px; }
.nav-link { font-size: 16px; color: $text-body; cursor: pointer; }
.nav-link:hover, .nav-link.active { color: $primary; font-weight: 600; }
.nav-actions { display: flex; align-items: center; gap: 14px;
  .nav-username { font-size: 15px; color: $text-dark; }
  :deep(.el-button) { border-radius: 20px; }
  :deep(.el-button--primary) { background: $primary; border-color: $primary;
    &:hover { background: $primary-dark; border-color: $primary-dark; }
  }
}

.emotionDiary-container {
  background: linear-gradient(160deg, #fef3f2 0%, #f0fdfa 60%, #f5f3ff 100%);

  .header-section {
    background: linear-gradient(135deg, #14b8a6 0%, #2dd4bf 50%, #f97316 100%);
    color: white;
    padding: 48px;
    text-align: center;

    .header-content {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;

      .header-icon { font-size: 36px; }
      h1 { font-size: 30px; font-weight: 800; margin: 0; }
      p { font-size: 15px; opacity: 0.9; margin: 6px 0 0; }
    }
  }

  .content {
    margin: 0 auto;
    width: 980px;
    max-width: 100%;
    box-sizing: border-box;
    padding: 20px;

    .diary-card {
      margin-bottom: 20px;
      background: rgba(255,255,255,0.8);
      backdrop-filter: blur(8px);
      border-radius: 16px;
      padding: 30px;
      border: 1px solid rgba(255,255,255,0.6);
      box-shadow: 0 6px 28px rgba(20,184,166,0.08);

      .title {
        margin-bottom: 24px;
        font-size: 25px;
        font-weight: 600;
        color: #1f2937;
      }

      .section {
        margin-bottom: 20px;
        p { font-size: 15px; color: #6B7280; margin-bottom: 12px; }
      }

      .mood-row {
        display: flex; align-items: center; gap: 16px;
      }
      .mood-picker {
        display: flex; gap: 4px; justify-content: space-between; flex: 1;
        .mood-emoji {
          font-size: 30px; cursor: pointer; opacity: 0.3; transition: all 0.2s;
          &:hover { opacity: 0.6; transform: scale(1.2); }
          &.active { opacity: 1; transform: scale(1.2); }
        }
      }
      .mood-score-display {
        font-size: 22px; font-weight: 800; white-space: nowrap;
        min-width: 56px; text-align: right;
      }
      .mood-hint { display: block; margin-top: 6px; font-size: 13px; font-weight: 600; }

      .emotion-grid {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;

        .emotion-card {
          padding: 15px 20px;
          border: 2px solid #E5E7EB;
          border-radius: 15px;
          text-align: center;
          cursor: pointer;
          background: #F9FAFB;
          transition: all 0.2s;
          min-width: 100px;

          .emotion-emoji { font-size: 30px; display: block; margin-bottom: 6px; }
          .emotion-name { color: #374151; font-size: 14px; font-weight: 600; }

          &.selected {
            border-color: #14b8a6;
            background: #f0fdfa;
            transform: translateY(-3px);
            box-shadow: 0 4px 12px rgba(20,184,166,0.2);
          }
          &:hover:not(.selected) { border-color: #d1d5db; transform: translateY(-2px); }
        }
      }

      .detail-form {
        .form-label {
          margin: 12px 0 6px;
          color: #374151;
          font-size: 15px;
          font-weight: 600;
        }

        .life-indicators {
          display: flex;
          gap: 40px;
          margin: 16px 0;
          .indicator-group { flex: 1; }
        }

        .action-buttons {
          margin-top: 30px;
          text-align: center;
          :deep(.el-button--primary) {
            width: 220px; height: 48px; font-size: 17px; font-weight: 600;
            background: linear-gradient(135deg, #14b8a6, #2dd4bf);
            border: none; border-radius: 24px;
            &:hover { background: linear-gradient(135deg, #0d9488, #14b8a6); }
          }
        }
      }
    }
  }
}
</style>
