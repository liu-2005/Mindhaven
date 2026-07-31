<template>
  <div class="consult-page">
    <svg width="0" height="0" style="position:absolute">
      <defs>
        <linearGradient id="esGrad" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="#a7f3d0" />
          <stop offset="0.4" stop-color="#5eead4" />
          <stop offset="0.7" stop-color="#fde68a" />
          <stop offset="1" stop-color="#fda4af" />
        </linearGradient>
      </defs>
    </svg>

    <header class="topbar">
      <div class="topbar-inner">
        <div class="brand" @click="goHome">
          <img src="@/assets/images/smile.svg" class="brand-logo" />
          <span class="brand-name">心晴 MindHaven</span>
        </div>
        <nav class="nav-links">
          <a class="nav-link" @click="goHome">首页</a>
          <a class="nav-link active">AI咨询</a>
          <a class="nav-link" @click="goDiary">情绪日记</a>
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

    <div class="consult-body">
      <aside class="sidebar">
        <!-- AI 信息 -->
        <div class="ai-info">
          <div class="ai-avatar">
            <img src="@/assets/images/robot.svg" class="ai-avatar-img" />
          </div>
          <div class="ai-meta">
            <h3>{{ currentAiName }}</h3>
            <p>你的情绪陪伴AI</p>
            <span class="ai-status"><i class="dot"></i>在线</span>
          </div>
          <div class="ai-tags">
            <span class="ai-tag">温柔倾听</span>
            <span class="ai-tag">情绪分析</span>
            <span class="ai-tag">心理科普</span>
          </div>
          <p class="ai-intro">我会陪你说说话、梳理情绪，也会给你一些温暖的小建议。无论今天过得怎样，都可以告诉我。</p>
        </div>

        <!-- 情绪花园 -->
        <div class="mood-garden" :key="emotionData.key">
          <h4 class="sidebar-title">情绪花园</h4>
          <div v-if="emotionData.score > 0" class="emotion-score">
            <svg class="es-ring" viewBox="0 0 120 120">
              <circle class="es-bg" cx="60" cy="60" r="50" />
              <circle class="es-fg" cx="60" cy="60" r="50" :stroke-dasharray="ringDash" stroke-dashoffset="0" />
            </svg>
            <div class="es-center">
              <span class="es-num">{{ emotionData.score }}</span>
              <span class="es-unit">分</span>
            </div>
          </div>
          <p class="es-label" :style="{ color: scoreColor }">{{ scoreText }}</p>
          <div v-if="['极度危险', '极度危机'].includes(emotionData.label)" class="risk-alert" role="alert">
            <div class="risk-alert-title">⚠️ 风险提示</div>
            <p class="risk-alert-description">
              {{ emotionData.riskDescription || '检测到当前处于紧急危机状态。' }}
            </p>
            <p class="risk-alert-action">请不要独自承受，立即联系亲友、心理援助热线或当地紧急救援服务。</p>
          </div>
          <div v-if="emotionData.suggestion" class="suggest-card">
            <div class="sc-head">
              <span class="sc-head-icon">🌱</span>
              <span class="sc-head-text">给你的小建议</span>
            </div>
            <p class="sc-body">{{ emotionData.suggestion }}</p>
          </div>
          <div v-if="emotionData.improvements.length" class="healing-card">
            <div class="hc-head">
              <span class="hc-head-icon">🌸</span>
              <span class="hc-head-text">治愈小行动</span>
            </div>
            <div class="hc-list">
              <span v-for="act in emotionData.improvements" :key="act" class="hc-item">{{ act }}</span>
            </div>
          </div>
        </div>

        <!-- 会话历史 -->
        <div class="history">
          <div class="history-head">
            <h4 class="sidebar-title">会话历史</h4>
            <el-button text size="small" @click="newSession">
              <el-icon><Plus /></el-icon>新建
            </el-button>
          </div>
          <div class="history-list">
            <div
              v-for="s in sessions"
              :key="s.id"
              class="history-item"
              :class="{ active: currentSessionId === s.id }"
              @click="selectSession(s.id)"
            >
              <el-icon class="history-icon"><ChatLineRound /></el-icon>
              <div class="history-meta">
                <span class="history-title">{{ s.title }}</span>
                <span class="history-time">{{ s.time }}</span>
                <span class="history-stats">
                  <span class="stat-chip">{{ s.messageCount || 0 }}条</span>
                  <span class="stat-chip"><el-icon><Clock /></el-icon>{{ formatDuration(s.duration) }}</span>
                </span>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- 聊天主体 -->
      <main class="chat-main">
        <div class="chat-header">
          <div class="chat-header-left">
            <h3>小晴 · 情绪陪伴</h3>
            <span class="chat-tip">您的贴心AI心理健康助手</span>
          </div>
          <el-button class="new-chat-btn" circle @click="newSession">
            <el-icon :size="20"><Plus /></el-icon>
          </el-button>
        </div>
        <div ref="chatBoxRef" class="chat-box">
          <div v-for="msg in messages" :key="msg.id" class="msg-row" :class="msg.role">
            <div class="msg-avatar" :class="msg.role">
              <img v-if="msg.role === 'ai'" src="@/assets/images/robot.svg" class="msg-avatar-img" />
              <el-icon v-else :size="22"><Avatar /></el-icon>
            </div>
            <div class="msg-bubble">{{ msg.content }}</div>
          </div>
        </div>
        <div class="chat-input">
          <el-input v-model="inputText" type="textarea" :rows="3" placeholder="说点什么吧，小晴在听..." resize="none" @keydown.enter.exact.prevent="sendMessage" />
          <el-button type="primary" :loading="sending" @click="sendMessage">
            <el-icon><Promotion /></el-icon>发送
          </el-button>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, markRaw, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ChatLineRound, Avatar, Plus, Promotion, Sunny, EditPen, Lightning, Clock } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { logout } from '@/api/user'
import { createSession, getSessionPage, getSessionMessages, getSessionEmotion, streamChat } from '@/api/consultation'

const router = useRouter()

// ======== 登录态 ========
const loggedIn = ref(!!localStorage.getItem('token'))
const readUserInfo = () => { try { return JSON.parse(localStorage.getItem('userInfo') || '{}') } catch { return {} } }
const userInfo = ref(readUserInfo())
const username = ref(userInfo.value.nickname || userInfo.value.username || '用户')

// ======== 情绪花园 ========
const moodList = ref([
  { label: '平静', count: 0, icon: markRaw(Sunny), color: '#14b8a6' },
  { label: '开心', count: 0, icon: markRaw(Sunny), color: '#f97316' },
  { label: '焦虑', count: 0, icon: markRaw(EditPen), color: '#8b5cf6' },
  { label: '低落', count: 0, icon: markRaw(Lightning), color: '#ec4899' },
])
const moodMap = { '平静': 0, '放松': 0, '安心': 0, '开心': 1, '快乐': 1, '焦虑': 2, '紧张': 2, '低落': 3, '难过': 3, '悲伤': 3 }

const emotionData = ref({ score: 0, suggestion: '', improvements: [], label: '', riskDescription: '', key: 0 })

const ringDash = computed(() => {
  const c = 2 * Math.PI * 50
  return `${(Math.min(emotionData.value.score, 100) / 100) * c} ${c}`
})
const scoreColor = computed(() => {
  const s = emotionData.value.score
  if (s >= 80) return '#f59e0b'
  if (s >= 60) return '#10b981'
  if (s >= 40) return '#8b5cf6'
  return '#f472b6'
})
const scoreText = computed(() => {
  const s = emotionData.value.score
  if (s >= 80) return '心情明媚'
  if (s >= 60) return '状态不错'
  if (s >= 40) return '平和'
  return '需要照顾'
})

function setEmotion(data) {
  const rawScore = Number(data.emotionScore ?? data.emotion_score ?? 0)
  const score = Math.min(100, Math.max(0, Number.isFinite(rawScore) ? rawScore : 0))
  emotionData.value = {
    score,
    suggestion: data.suggestion || '',
    improvements: data.improvementSuggestions || data.improvement_suggestions || [],
    label: data.label || '',
    riskDescription: data.riskDescription || data.risk_description || '',
    key: emotionData.value.key + 1,
  }
}
function resetEmotion() {
  emotionData.value = { score: 0, suggestion: '', improvements: [], label: '', riskDescription: '', key: emotionData.value.key + 1 }
}

// 临时诊断：检查情绪花园接口实际返回的字段结构。
function inspectEmotionGardenFields(data) {
  const entries = data && typeof data === 'object' ? Object.entries(data) : []

  console.groupCollapsed('[情绪花园] 接口字段检查')
  console.log('完整响应数据：', data)
  console.table(entries.map(([field, value]) => ({
    field,
    value,
    type: Array.isArray(value) ? 'array' : value === null ? 'null' : typeof value,
  })))
  console.groupEnd()
}

async function loadEmotion(sessionId) {
  try {
    const res = await getSessionEmotion(sessionId)
    const data = res.data || res
    inspectEmotionGardenFields(data)
    setEmotion(data)
    moodList.value.forEach(m => (m.count = 0))
    const keywords = data.keywords || []
    keywords.forEach(k => {
      const idx = moodMap[k]
      if (idx !== undefined) moodList.value[idx].count++
    })
  } catch (err) {
    console.error('[情绪分析] 失败:', err)
  }
}

// ======== 会话管理 ========
const sessions = ref([])
const currentSessionId = ref(null)
const currentAiName = computed(() => {
  const s = sessions.value.find(x => x.id === currentSessionId.value)
  return s?.aiName || 'AI助手'
})

function formatTime(ts) {
  if (!ts) return ''
  const t = typeof ts === 'number' ? ts : Date.parse(ts)
  if (!t) return ''
  const diff = Date.now() - t
  const min = Math.floor(diff / 60000)
  if (min < 1) return '刚刚'
  if (min < 60) return `${min}分钟前`
  const h = Math.floor(min / 60)
  if (h < 24) return `${h}小时前`
  return `${Math.floor(h / 24)}天前`
}
function formatDuration(min) {
  const m = Number(min) || 0
  if (m <= 0) return '-'
  if (m < 60) return `${m}分钟`
  return `${Math.floor(m / 60)}小时`
}

async function loadSessions() {
  try {
    const res = await getSessionPage({ pageNum: 1, pageSize: 50 })
    const data = res.data || res
    const list = data.records || data.list || data.rows || data || []
    sessions.value = list.map(s => ({
      id: s.id || s.sessionId,
      title: s.sessionTitle || s.title || '未命名',
      time: formatTime(s.startedAt || s.startTime || s.lastMessageTime),
      messageCount: s.messageCount || 0,
      duration: s.durationMinutes || 0,
    }))
  } catch (err) {
    console.error('加载会话失败:', err)
  }
}

async function selectSession(id) {
  if (currentSessionId.value === id) return
  currentSessionId.value = id
  messages.value = []
  resetEmotion()
  try {
    const res = await getSessionMessages(id)
    const data = res.data || res
    const list = data.records || data.list || data.rows || data || []
    messages.value = list.map(m => ({
      id: m.id || Date.now(),
      role: m.senderType === 1 ? 'user' : 'ai',
      content: m.content || m.message || '',
    }))
  } catch (err) {
    console.error('加载消息失败:', err)
  }
  loadEmotion(id)
}

async function newSession() {
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  const title = `小晴AI - ${now.getFullYear()}/${pad(now.getMonth() + 1)}/${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
  try {
    const res = await createSession({ initialMessage: '你好，我想和你聊聊', sessionTitle: title })
    const data = res.data || res
    const session = { id: data.sessionId, title, time: '刚刚', messageCount: 1, duration: 0 }
    sessions.value.unshift(session)
    currentSessionId.value = session.id
    messages.value = [{ id: Date.now(), role: 'ai', content: '你好呀，我是小晴，今天感觉怎么样？' }]
  } catch (err) {
    console.error('新建会话失败:', err)
  }
}

// ======== 聊天 ========
const chatBoxRef = ref(null)
const inputText = ref('')
const sending = ref(false)
const messages = ref([])
function scrollToBottom() {
  nextTick(() => {
    if (chatBoxRef.value) chatBoxRef.value.scrollTop = chatBoxRef.value.scrollHeight
  })
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || !currentSessionId.value) return
  messages.value.push({ id: Date.now(), role: 'user', content: text })
  inputText.value = ''
  scrollToBottom()
  sending.value = true
  const aiMsg = reactive({ id: Date.now() + 1, role: 'ai', content: '' })
  messages.value.push(aiMsg)
  scrollToBottom()
  try {
    await streamChat(currentSessionId.value, text, (chunk, fullText) => {
      aiMsg.content = fullText
      scrollToBottom()
    })
    if (currentSessionId.value) loadEmotion(currentSessionId.value)
  } catch (err) {
    let msg = '抱歉，我遇到了一些问题，请稍后再试。'
    try { const p = JSON.parse(err.message); if (p.msg) msg = p.msg } catch {}
    aiMsg.content = msg
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

// ======== 导航 ========
const goHome = () => router.push('/')
const goDiary = () => router.push('/diary')
const goKnowledge = () => router.push('/knowledge')
const goLogin = () => router.push('/auth/login')
const goRegister = () => router.push('/auth/register')
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    .then(async () => {
      try { await logout() } catch {}
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/auth/login')
    })
    .catch(() => {})
}

onMounted(async () => {
  await loadSessions()
  if (sessions.value.length) selectSession(sessions.value[0].id)
})
</script>

<style lang="scss" scoped>
$primary: #14b8a6;
$primary-dark: #0d9488;
$accent: #f97316;
$text-dark: #1f2937;
$text-body: #4b5563;
$text-muted: #9ca3af;
$border: #eef0f2;

.consult-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: linear-gradient(160deg, #fef3f2 0%, #f0fdfa 60%, #f5f3ff 100%);
}

/* 顶部导航 */
.topbar {
  height: 64px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid $border;
  flex-shrink: 0;
  z-index: 10;
}
.topbar-inner {
  max-width: 1400px;
  height: 100%;
  margin: 0 auto;
  padding: 0 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.brand { display: flex; align-items: center; gap: 12px; cursor: pointer; }
.brand-logo { width: 34px; height: 34px; }
.brand-name {
  font-size: 20px;
  font-weight: 800;
  background: linear-gradient(135deg, $primary, $accent);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.nav-links { display: flex; gap: 32px; }
.nav-link { font-size: 16px; color: $text-body; cursor: pointer; }
.nav-link.active, .nav-link:hover { color: $primary; font-weight: 600; }
.nav-actions { display: flex; align-items: center; gap: 14px; }
.nav-username { font-size: 15px; color: $text-dark; }

/* 主体布局 */
.consult-body {
  flex: 1;
  display: flex;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  padding: 20px;
  gap: 20px;
  overflow: hidden;
}

/* 左侧栏 */
.sidebar {
  width: 320px;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  box-shadow: 0 6px 28px rgba(20, 184, 166, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.7);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* AI 信息 */
.ai-info {
  padding: 24px 24px 22px;
  border-bottom: 1px solid $border;
  background: linear-gradient(160deg, rgba(20, 184, 166, 0.06), rgba(249, 115, 22, 0.04));
}
.ai-avatar {
  width: 64px; height: 64px;
  border-radius: 50%;
  background: #fffbeb;
  border: 1.5px solid #fde68a;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
  box-shadow: 0 8px 22px rgba(249, 115, 22, 0.15);
}
.ai-avatar-img { width: 40px; height: 40px; }
.ai-meta { text-align: center; margin-bottom: 14px; }
.ai-meta h3 { font-size: 22px; font-weight: 800; color: $text-dark; }
.ai-meta p { font-size: 14px; color: $text-body; }
.ai-status {
  font-size: 13px;
  color: $primary;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-weight: 600;
}
.ai-status .dot {
  width: 7px; height: 7px;
  background: $primary;
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgba(20, 184, 166, 0.18);
}
.ai-tags { display: flex; justify-content: center; gap: 8px; margin-bottom: 14px; }
.ai-tag {
  padding: 5px 14px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 600;
  background: rgba(20, 184, 166, 0.1);
  color: $primary-dark;
  border: 1px solid rgba(20, 184, 166, 0.18);
}
.ai-intro { font-size: 13px; line-height: 1.75; color: $text-body; text-align: center; }

/* 情绪花园 */
.mood-garden { padding: 20px 24px; border-bottom: 1px solid $border; }
.sidebar-title { font-size: 15px; color: $text-dark; font-weight: 700; margin-bottom: 14px; }
.emotion-score { position: relative; width: 100px; height: 100px; margin: 4px auto 12px; }
.es-ring { width: 100%; height: 100%; transform: rotate(-90deg); }
.es-ring .es-bg { fill: none; stroke: rgba(20, 184, 166, 0.08); stroke-width: 6; }
.es-ring .es-fg { fill: none; stroke: url(#esGrad); stroke-width: 6; stroke-linecap: round; }
.es-center {
  position: absolute; top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  display: flex; align-items: baseline; gap: 2px;
}
.es-num { font-size: 28px; font-weight: 800; color: $text-dark; }
.es-unit { font-size: 12px; color: $text-muted; }
.es-label { text-align: center; font-size: 14px; font-weight: 700; margin-bottom: 14px; }

.risk-alert {
  padding: 12px 14px;
  margin: -2px 0 12px;
  background: #fff1f2;
  border: 1px solid #fda4af;
  border-radius: 12px;
  color: #9f1239;
}
.risk-alert-title { font-size: 13px; font-weight: 800; margin-bottom: 6px; }
.risk-alert-description,
.risk-alert-action { margin: 0; font-size: 12px; line-height: 1.65; }
.risk-alert-action { margin-top: 5px; font-weight: 600; }

/* 建议卡片 */
.suggest-card {
  padding: 14px 16px;
  margin-bottom: 12px;
  background: linear-gradient(135deg, #ecfdf5, #fffbeb);
  border-radius: 14px;
  border: 1px solid rgba(20, 184, 166, 0.15);
}
.sc-head-text { font-size: 12px; font-weight: 700; color: #0d9488; }
.sc-body { font-size: 12px; line-height: 1.7; color: $text-body; margin: 8px 0 0; }

/* 治愈行动 */
.healing-card {
  padding: 14px 16px;
  background: linear-gradient(135deg, #fdf2f8, #fffbeb);
  border-radius: 14px;
  border: 1px solid rgba(244, 114, 182, 0.15);
}
.hc-head-text { font-size: 12px; font-weight: 700; color: #db2777; }
.hc-list { display: flex; flex-direction: column; gap: 6px; margin-top: 10px; }
.hc-item {
  font-size: 12px;
  color: $text-body;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 10px;
}

/* 会话历史 */
.history { flex: 1; display: flex; flex-direction: column; padding: 20px 24px; overflow: hidden; }
.history-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.history-list { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 6px; }
.history-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px; border-radius: 14px; cursor: pointer;
}
.history-item:hover { background: rgba(20, 184, 166, 0.06); }
.history-item.active {
  background: linear-gradient(135deg, rgba(20, 184, 166, 0.12), rgba(249, 164, 175, 0.1));
}
.history-icon { color: $text-muted; flex-shrink: 0; font-size: 18px; }
.history-meta { display: flex; flex-direction: column; overflow: hidden; }
.history-title {
  font-size: 14px; color: $text-dark;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.history-time { font-size: 12px; color: $text-muted; margin-top: 2px; }
.history-stats { display: flex; gap: 10px; margin-top: 4px; }
.stat-chip { font-size: 11px; color: $text-muted; display: flex; align-items: center; gap: 3px; }

/* 聊天主体 */
.chat-main {
  flex: 1;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  box-shadow: 0 6px 28px rgba(20, 184, 166, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.7);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.chat-header {
  padding: 20px 28px;
  background: linear-gradient(135deg, #ecfdf5, #d1fae5);
  border-bottom: 1px solid #a7f3d0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.chat-header h3 { font-size: 20px; color: $text-dark; font-weight: 800; }
.chat-tip { font-size: 13px; color: #0f766e; }
.new-chat-btn {
  width: 40px; height: 40px;
  background: $primary; border-color: $primary; color: #fff;
  box-shadow: 0 4px 12px rgba(20, 184, 166, 0.3);
}

/* 聊天消息 */
.chat-box { flex: 1; overflow-y: auto; padding: 28px; display: flex; flex-direction: column; gap: 20px; }
.msg-row { display: flex; gap: 12px; max-width: 75%; }
.msg-row.ai { align-self: flex-start; }
.msg-row.user { align-self: flex-end; flex-direction: row-reverse; }
.msg-avatar {
  width: 44px; height: 44px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.08);
}
.msg-avatar.ai { background: #fffbeb; border: 1px solid #fde68a; }
.msg-avatar.user { background: linear-gradient(135deg, #a78bfa, #8b5cf6); color: #fff; }
.msg-avatar-img { width: 32px; height: 32px; }
.msg-bubble {
  padding: 16px 22px;
  font-size: 17px;
  line-height: 1.75;
  white-space: pre-wrap;
  box-shadow: 0 3px 14px rgba(0, 0, 0, 0.05);
}
.msg-row.ai .msg-bubble {
  background: #fff;
  color: $text-dark;
  border: 1px solid $border;
  border-radius: 20px 20px 20px 6px;
}
.msg-row.user .msg-bubble {
  background: linear-gradient(135deg, $primary, #2dd4bf);
  color: #fff;
  border: none;
  border-radius: 20px 20px 6px 20px;
}

/* 输入区 */
.chat-input {
  padding: 18px 28px 22px;
  border-top: 1px solid $border;
  display: flex;
  gap: 14px;
  align-items: flex-end;
}
:deep(.el-textarea__inner) { border-radius: 16px; font-size: 15px; padding: 12px 16px; }
:deep(.el-button--primary) {
  background: $primary; border-color: $primary;
  border-radius: 16px; height: 72px;
  font-size: 16px; font-weight: 600; padding: 0 26px;
}
:deep(.el-button--primary:hover) { background: $primary-dark; border-color: $primary-dark; }

@media (max-width: 768px) {
  .sidebar { display: none; }
  .nav-links { display: none; }
}
</style>
