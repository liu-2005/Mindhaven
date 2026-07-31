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
          <h2 class="title">今日情绪记录</h2>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top">

            <!-- 记录日期 -->
            <div class="section">
              <p>选择日期</p>
              <el-date-picker v-model="form.diaryDate" type="date" placeholder="选择日期" style="width:220px" />
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
              <el-input v-model="form.diaryContent" type="textarea" :rows="5" placeholder="写下今天的感受和想法..." style="margin-bottom:16px" />

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
                  保存日记
                </el-button>
              </div>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import { logout } from '@/api/user'
import { saveEmotionDiary } from '@/api/emotionDiary'

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

const today = new Date().toISOString().slice(0, 10)
const form = reactive({
  diaryDate: today,
  moodScore: 0,
  dominantEmotion: '',
  emotionTriggers: '',
  diaryContent: '',
  sleepQuality: 0,
  stressLevel: 0,
})

const rules = {
  diaryDate: [{ required: true, message: '请选择日期', trigger: 'blur' }],
  moodScore: [{ required: true, message: '请选择心情', trigger: 'change' }],
  dominantEmotion: [{ required: true, message: '请选择情绪', trigger: 'change' }],
  emotionTriggers: [{ required: true, message: '请填写触发因素', trigger: 'blur' }],
  diaryContent: [{ required: true, message: '请写下今日感想', trigger: 'blur' }],
  sleepQuality: [{ required: true, message: '请选择睡眠质量', trigger: 'change' }],
  stressLevel: [{ required: true, message: '请选择压力水平', trigger: 'change' }],
}

const handleSubmit = () => {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await saveEmotionDiary({ ...form })
      ElMessage.success('日记保存成功')
    } catch (err) {
      console.error('[情绪日记] 保存失败：', err)
    } finally {
      submitting.value = false
    }
  })
}

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