<template>
  <div class="front-home">
    <!-- 顶部导航 -->
    <header class="navbar" :class="{ scrolled }">
      <div class="nav-inner">
        <div class="brand" @click="scrollTo('hero')">
          <img src="@/assets/images/smile.png" alt="logo" class="brand-logo" />
          <span class="brand-name">心晴 MindHaven</span>
        </div>
        <nav class="nav-links">
          <a class="nav-link" :class="{ active: activeNav === 'home' }" @click="goHome">首页</a>
          <a class="nav-link" :class="{ active: activeNav === 'consult' }" @click="goConsult">AI咨询</a>
          <a class="nav-link" :class="{ active: activeNav === 'diary' }" @click="goDiary">情绪日记</a>
          <a class="nav-link" :class="{ active: activeNav === 'knowledge' }" @click="goKnowledge">知识库</a>
        </nav>
        <div class="nav-actions">
          <template v-if="!loggedIn">
            <el-button round @click="goLogin">登录</el-button>
            <el-button type="primary" round @click="goRegister">注册</el-button>
          </template>
          <template v-else>
            <span class="nav-username">{{ username }}</span>
            <el-button v-if="isAdmin" type="primary" round @click="goBackend">进入后台</el-button>
            <el-button round @click="handleLogout">退出登录</el-button>
          </template>
        </div>
      </div>
    </header>

    <!-- Hero 主视觉 -->
    <section id="hero" class="hero">
      <div class="hero-inner">
        <div class="hero-text">
          <h1 class="hero-title">每一种心情<br />都值得被记录</h1>
          <p class="hero-subtitle">
            写下今天的感受，让 AI 陪你读懂自己。<br />
            专业文章与咨询，在你需要的时候都在。
          </p>
          <div class="hero-actions">
            <el-button type="primary" size="large" round @click="onStart">
              {{ loggedIn ? (isAdmin ? '进入后台' : '继续探索') : '开始使用' }}
              <el-icon class="el-icon--right"><ArrowRight /></el-icon>
            </el-button>
            <el-button size="large" round @click="scrollTo('features')">
              了解更多
            </el-button>
          </div>
          <div class="hero-stats">
            <div class="stat-item">
              <span class="stat-num">日记</span>
              <span class="stat-label">记录心情</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-num">科普</span>
              <span class="stat-label">认识情绪</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-num">AI</span>
              <span class="stat-label">温暖陪伴</span>
            </div>
          </div>
        </div>
        <div class="hero-visual">
          <img src="@/assets/hero-illustration.jpg" alt="hero" class="hero-img" />
        </div>
      </div>
      <div class="hero-wave"></div>
    </section>

    <!-- 功能特性 -->
    <section id="features" class="features section">
      <div class="section-head">
        <h2 class="section-title">你能做什么</h2>
        <p class="section-desc">四个工具，陪你照顾每一天的情绪</p>
      </div>
      <div class="feature-grid">
        <div
          v-for="item in features"
          :key="item.title"
          class="feature-card"
          :style="{ '--accent': item.color }"
        >
          <div class="feature-icon">
            <el-icon :size="28"><component :is="item.icon" /></el-icon>
          </div>
          <h3 class="feature-title">{{ item.title }}</h3>
          <p class="feature-desc">{{ item.desc }}</p>
        </div>
      </div>
    </section>

    <!-- AI 咨询 -->
    <section id="consult" class="consult section">
      <div class="section-head">
        <h2 class="section-title">AI 咨询</h2>
        <p class="section-desc">随时随地，和 AI 倾诉你的烦恼</p>
      </div>
      <div class="consult-card">
        <div class="consult-avatar">
          <el-icon :size="40"><ChatDotRound /></el-icon>
        </div>
        <div class="consult-body">
          <h3>智能情绪陪伴</h3>
          <p>遇到困扰却又不想打扰别人？AI 咨询 24 小时在线，听你说、陪你聊，帮你梳理情绪脉络。</p>
          <ul class="consult-list">
            <li>· 实时对话，情绪识别与共情回应</li>
            <li>· 按账号管理个人会话，支持删除历史</li>
            <li>· 智能建议，给出贴心的小贴士</li>
          </ul>
          <el-button type="primary" round @click="goConsultation">立即体验</el-button>
        </div>
      </div>
    </section>

    <!-- 情绪日记 -->
    <section id="diary" class="diary section">
      <div class="section-head">
        <h2 class="section-title">情绪日记</h2>
        <p class="section-desc">记录每一天的心情，看见自己的成长</p>
      </div>
      <div class="diary-card">
        <div class="diary-body">
          <h3>每天写几句，看见自己</h3>
          <p>情绪日记帮你把说不清的感受落在文字里，AI 会自动分析你的情绪倾向，让自我觉察变得简单自然。</p>
          <ul class="diary-list">
            <li>· 一句话也能记录，零门槛上手</li>
            <li>· AI 情绪分析，量化你的心情曲线</li>
            <li>· 历史回看，发现情绪起伏的规律</li>
          </ul>
          <el-button type="primary" round @click="onStartDiary">开始记录</el-button>
        </div>
        <div class="diary-visual">
          <div class="mood-grid">
            <div v-for="m in moods" :key="m.label" class="mood-item" :style="{ '--m': m.color }">
              <el-icon :size="24"><component :is="m.icon" /></el-icon>
              <span>{{ m.label }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 知识库 -->
    <section id="articles" class="articles section">
      <div class="section-head">
        <h2 class="section-title">知识库</h2>
        <p class="section-desc">心理学没那么远，从这里开始</p>
      </div>
      <div v-loading="articleLoading" class="article-grid">
        <template v-if="articleList.length">
          <div
            v-for="art in articleList"
            :key="art.id"
            class="article-card"
            @click="goArticle(art)"
          >
            <div class="article-cover">
              <img
                v-if="art.coverImage || art.cover || art.coverUrl"
                :src="resolveCover(art)"
                :alt="art.title"
                @error="onCoverError"
              />
              <div v-else class="cover-placeholder" :style="art.coverColor ? { '--placeholder-bg': art.coverColor } : {}">
                <span v-if="art.coverEmoji" class="cover-emoji">{{ art.coverEmoji }}</span>
                <el-icon v-else :size="32"><Picture /></el-icon>
              </div>
              <span v-if="art.categoryName || art.category?.name" class="article-tag">
                {{ art.categoryName || art.category.name }}
              </span>
            </div>
            <div class="article-body">
              <h3 class="article-title">{{ art.title }}</h3>
              <p class="article-summary">{{ art.summary || stripHtml(art.content) }}</p>
              <div class="article-meta">
                <span class="meta-author">{{ art.authorName || art.author?.name || '心晴' }}</span>
                <span class="meta-read">
                  <el-icon><View /></el-icon>
                  {{ art.readCount || art.viewCount || 0 }}
                </span>
              </div>
            </div>
          </div>
        </template>
        <el-empty v-else-if="!articleLoading" description="暂无文章" />
      </div>
    </section>

    <!-- 关于 -->
    <section id="about" class="about section">
      <div class="about-inner">
        <h2 class="section-title light">关于心晴</h2>
        <p class="about-desc">
          心晴是一个情绪健康平台。<br />
          我们相信，情绪没有好坏，只有被看见和没被看见。<br />
          写下来，读懂它，慢慢来。
        </p>
        <el-button type="primary" size="large" round @click="onJoin">{{ loggedIn ? '进入后台' : '立即加入' }}</el-button>
      </div>
    </section>

    <!-- 页脚 -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <img src="@/assets/images/smile.png" alt="logo" class="footer-logo" />
          <span>心晴 MindHaven</span>
        </div>
        <p class="footer-text">情绪没有好坏，只有被看见和没被看见</p>
        <p class="footer-copy">© 2026 心晴 MindHaven. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import {
  ArrowRight, Picture, View,
  EditPen, ChatDotRound, Document, Sunny,
} from '@element-plus/icons-vue'
import { getArticlePage } from '@/api/knowledge'
import { logout } from '@/api/user'

const router = useRouter()

const activeNav = ref('home')

// 从 localStorage 读取登录态
const readUserInfo = () => {
  try { return JSON.parse(localStorage.getItem('userInfo') || '{}') } catch { return {} }
}
const userInfo = ref(readUserInfo())
const token = ref(localStorage.getItem('token') || '')
const loggedIn = computed(() => !!token.value)
const isAdmin = computed(() => (userInfo.value.roles || []).some(r => ['CONTENT_ADMIN', 'SAFETY_ADMIN', 'USER_ADMIN'].includes(r)))
const username = computed(() => userInfo.value.nickname || userInfo.value.username || userInfo.value.name || '用户')

const goBackend = () => router.push('/back/dashboard')

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    const loading = ElLoading.service({ fullscreen: true, text: '正在退出...' })
    try {
      await logout()
    } catch (e) {
      console.error('登出接口调用失败', e)
    } finally {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      token.value = ''
      userInfo.value = {}
      loading.close()
      ElMessage.success('已退出登录')
      router.push('/auth/login')
    }
  }).catch(() => {})
}

const features = [
  { title: '情绪日记', desc: '每天写几句，记录心情，看见自己的变化。', icon: EditPen, color: '#14b8a6' },
  { title: 'AI 情绪分析', desc: 'AI 读懂你的情绪，给出贴心的建议。', icon: Sunny, color: '#f97316' },
  { title: '知识文章', desc: '靠谱的心理学内容，想了解的这里都有。', icon: Document, color: '#8b5cf6' },
  { title: 'AI陪伴', desc: '和AI梳理感受，获得日常陪伴。', icon: ChatDotRound, color: '#ec4899' },
]

const moods = [
  { label: '平静', icon: Sunny, color: '#14b8a6' },
  { label: '开心', icon: Sunny, color: '#f97316' },
  { label: '焦虑', icon: EditPen, color: '#8b5cf6' },
  { label: '低落', icon: EditPen, color: '#ec4899' },
]

// 滚动监听：导航栏背景切换
const scrolled = ref(false)
const onScroll = () => { scrolled.value = window.scrollY > 20 }
onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  fetchArticles()
})
onUnmounted(() => window.removeEventListener('scroll', onScroll))

// 锚点平滑滚动
const scrollTo = (id) => {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
}

const goLogin = () => router.push('/auth/login')
const goConsultation = () => router.push('/consultation')
const goRegister = () => router.push('/auth/register')

const onStart = () => {
  if (!loggedIn.value) return goLogin()
  if (isAdmin.value) return goBackend()
  router.push('/diary')
}
const onStartDiary = () => {
  if (!loggedIn.value) return goLogin()
  router.push('/diary')
}
const onJoin = () => {
  if (!loggedIn.value) return goRegister()
  if (isAdmin.value) return goBackend()
  scrollTo('features')
}

const goHome = () => {
  activeNav.value = 'home'
  scrollTo('hero')
}

const goConsult = () => {
  router.push('/consultation')
}

const goDiary = () => router.push('/diary')

const goKnowledge = () => router.push('/knowledge')

// 最新文章
const articleList = ref([])
const articleLoading = ref(false)

const fetchArticles = async () => {
  if (!loggedIn.value) {
    articleList.value = [
      { id: 0, title: '什么是心理健康？', summary: '了解心理健康的基本概念', readCount: 1280, authorName: '心晴', coverColor: '#14b8a6', coverEmoji: '🧠' },
      { id: 0, title: '如何缓解日常焦虑', summary: '几个简单有效的小方法', readCount: 960, authorName: '心晴', coverColor: '#f97316', coverEmoji: '🌿' },
      { id: 0, title: '情绪管理的五个技巧', summary: '学会和自己的情绪相处', readCount: 756, authorName: '心晴', coverColor: '#8b5cf6', coverEmoji: '💡' },
      { id: 0, title: '睡眠与心理健康', summary: '好睡眠是好心情的基础', readCount: 610, authorName: '心晴', coverColor: '#ec4899', coverEmoji: '🌙' },
      { id: 0, title: '建立健康的社交关系', summary: '人际关系对心理的重要影响', readCount: 520, authorName: '心晴', coverColor: '#3b82f6', coverEmoji: '🤝' },
      { id: 0, title: '正念冥想入门指南', summary: '每天十分钟，找回内心的平静', readCount: 480, authorName: '心晴', coverColor: '#f59e0b', coverEmoji: '🧘' },
    ]
    return
  }
  articleLoading.value = true
  try {
    const res = await getArticlePage({ page: 1, size: 6 }, { silent: true })
    const data = res.data || res
    articleList.value = data.items || []
  } catch (e) {
    console.error('获取文章失败', e)
  } finally {
    articleLoading.value = false
  }
}

const resolveCover = (art) => art.coverImage || art.cover || art.coverUrl || ''
const stripHtml = (html = '') => html.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').trim().slice(0, 80)
const onCoverError = (e) => { e.target.style.display = 'none' }
const goArticle = (art) => {
  if (!loggedIn.value) return goLogin()
  router.push(`/article/${art.id}`)
}
</script>

<style lang="scss" scoped>
$primary: #14b8a6;
$primary-dark: #0d9488;
$primary-light: #ccfbf1;
$text-dark: #1f2937;
$text-body: #4b5563;
$text-muted: #9ca3af;

.front-home {
  background: #fff;
}

/* 导航栏 */
.navbar {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 100;
  transition: all 0.3s;

  &.scrolled {
    background: rgba(255, 255, 255, 0.92);
    backdrop-filter: blur(12px);
    box-shadow: 0 1px 16px rgba(20, 184, 166, 0.08);
  }
}
.nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 72px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.brand-logo {
  width: 36px; height: 36px;
}
.brand-name {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #14b8a6, #f97316);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.nav-links {
  display: flex;
  gap: 32px;
}
.nav-link {
  font-size: 15px;
  color: $text-body;
  cursor: pointer;
  position: relative;
  transition: color 0.2s;

  &:hover { color: $primary; }

  &.active {
    color: $primary;
    font-weight: 600;

    &::after {
      content: '';
      position: absolute;
      left: 0; right: 0;
      bottom: -6px;
      height: 2px;
      background: $primary;
      border-radius: 2px;
    }
  }
}
.nav-actions {
  display: flex;
  align-items: center;
  gap: 12px;

  .nav-username {
    font-size: 14px;
    color: $text-body;
    font-weight: 500;
  }

  :deep(.el-button) {
    font-weight: 500;
  }
  :deep(.el-button--primary) {
    background: $primary;
    border-color: $primary;

    &:hover {
      background: $primary-dark;
      border-color: $primary-dark;
    }
  }
}

/* Hero */
.hero {
  position: relative;
  min-height: 600px;
  padding: 130px 24px 90px;
  background: linear-gradient(135deg, #f0fdfa 0%, #fef3f2 100%);
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -60px; right: -60px;
    width: 320px; height: 320px;
    background: radial-gradient(circle, rgba(20, 184, 166, 0.12), transparent 70%);
    border-radius: 50%;
  }
  &::after {
    content: '';
    position: absolute;
    bottom: 40px; left: -80px;
    width: 260px; height: 260px;
    background: radial-gradient(circle, rgba(249, 115, 22, 0.08), transparent 70%);
    border-radius: 50%;
  }
}
.hero-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 40px;
  position: relative;
  z-index: 1;
}
.hero-text {
  flex: 1;
  color: $text-dark;
}
.hero-title {
  font-size: 48px;
  font-weight: 800;
  line-height: 1.3;
  margin-bottom: 20px;
  background: linear-gradient(135deg, #1f2937 0%, #14b8a6 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.hero-subtitle {
  font-size: 18px;
  line-height: 1.8;
  color: $text-body;
  margin-bottom: 32px;
}
.hero-actions {
  display: flex;
  gap: 16px;
  margin-bottom: 48px;

  :deep(.el-button--primary) {
    background: $primary;
    border-color: $primary;
    &:hover {
      background: $primary-dark;
      border-color: $primary-dark;
    }
  }
}
.hero-stats {
  display: flex;
  align-items: center;
  gap: 24px;
}
.stat-item {
  display: flex;
  flex-direction: column;
}
.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: $primary;
}
.stat-label {
  font-size: 13px;
  color: $text-muted;
}
.stat-divider {
  width: 1px;
  height: 32px;
  background: #e5e7eb;
}
.hero-visual {
  flex: 0 0 420px;
  text-align: right;
}
.hero-img {
  max-width: 100%;
  max-height: 420px;
  border-radius: 24px;
  filter: drop-shadow(0 20px 40px rgba(20, 184, 166, 0.15));
}
.hero-wave {
  position: absolute;
  bottom: -1px; left: 0; right: 0;
  height: 60px;
  background: #fff;
  clip-path: polygon(0 40%, 100% 0, 100% 100%, 0 100%);
}

/* 通用 section */
.section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 80px 24px;
}
.section-head {
  text-align: center;
  margin-bottom: 56px;
}
.section-title {
  font-size: 32px;
  font-weight: 700;
  color: $text-dark;
  margin-bottom: 12px;

  &.light { color: #fff; }
}
.section-desc {
  font-size: 16px;
  color: $text-body;
}

/* 功能卡片 */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}
.feature-card {
  padding: 32px 24px;
  background: #fff;
  border-radius: 16px;
  border: 1px solid #f3f4f6;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 12px 32px rgba(20, 184, 166, 0.1);
    border-color: var(--accent);
  }
}
.feature-icon {
  width: 56px; height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: var(--accent);
  margin-bottom: 20px;
}
.feature-title {
  font-size: 18px;
  font-weight: 600;
  color: $text-dark;
  margin-bottom: 10px;
}
.feature-desc {
  font-size: 14px;
  line-height: 1.7;
  color: $text-body;
}

/* 文章卡片 */
.articles {
  background: #f9fafb;
  max-width: none;
  padding-left: 24px;
  padding-right: 24px;

  .section-head,
  .article-grid {
    max-width: 1200px;
    margin-left: auto;
    margin-right: auto;
  }
}
.article-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 28px;
}
.article-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 16px 40px rgba(0, 0, 0, 0.08);

    .article-title { color: $primary; }
  }
}
.article-cover {
  position: relative;
  height: 180px;
  overflow: hidden;
  background: #f3f4f6;

  img {
    width: 100%; height: 100%;
    object-fit: cover;
    transition: transform 0.4s;
  }
  .article-card:hover & img {
    transform: scale(1.06);
  }
}
.cover-placeholder {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 20px; font-weight: 700;
  background: var(--placeholder-bg, linear-gradient(135deg, #f3f4f6, #e5e7eb));
}
.cover-emoji { font-size: 40px; }
.article-tag {
  position: absolute;
  top: 12px; left: 12px;
  padding: 4px 12px;
  background: rgba(20, 184, 166, 0.9);
  color: #fff;
  font-size: 12px;
  border-radius: 20px;
}
.article-body {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.article-title {
  font-size: 17px;
  font-weight: 600;
  color: $text-dark;
  margin-bottom: 10px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.2s;
}
.article-summary {
  font-size: 14px;
  line-height: 1.6;
  color: $text-body;
  margin-bottom: 16px;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.article-meta {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: $text-muted;
}
.meta-read {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* AI 咨询 */
.consult-card {
  display: flex;
  gap: 40px;
  align-items: center;
  background: linear-gradient(135deg, #f0fdfa 0%, #fef3f2 100%);
  border-radius: 24px;
  padding: 48px;
}
.consult-avatar {
  flex: 0 0 120px;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: #14b8a6;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12px 32px rgba(20, 184, 166, 0.25);
}
.consult-body {
  flex: 1;

  h3 { font-size: 22px; color: #1f2937; margin-bottom: 12px; font-weight: 700; }
  p { font-size: 15px; color: #4b5563; line-height: 1.7; margin-bottom: 16px; }
  .consult-list { margin-bottom: 24px;
    li { font-size: 14px; color: #6b7280; line-height: 2; }
  }
  :deep(.el-button--primary) {
    background: #14b8a6; border-color: #14b8a6;
    &:hover { background: #0d9488; border-color: #0d9488; }
  }
}

/* 情绪日记 */
.diary-card {
  display: flex;
  gap: 40px;
  align-items: center;
  background: #fff;
  border: 1px solid #f3f4f6;
  border-radius: 24px;
  padding: 48px;
  box-shadow: 0 8px 32px rgba(20, 184, 166, 0.06);
}
.diary-body {
  flex: 1;

  h3 { font-size: 22px; color: #1f2937; margin-bottom: 12px; font-weight: 700; }
  p { font-size: 15px; color: #4b5563; line-height: 1.7; margin-bottom: 16px; }
  .diary-list { margin-bottom: 24px;
    li { font-size: 14px; color: #6b7280; line-height: 2; }
  }
  :deep(.el-button--primary) {
    background: #14b8a6; border-color: #14b8a6;
    &:hover { background: #0d9488; border-color: #0d9488; }
  }
}
.diary-visual { flex: 0 0 240px; }
.mood-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.mood-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 12px;
  background: #fff;
  border-radius: 14px;
  border: 1px solid var(--m);
  color: var(--m);
  font-size: 13px;
}

/* 关于 */
.about {
  max-width: none;
  padding: 100px 24px;
  background: linear-gradient(135deg, #14b8a6 0%, #2dd4bf 100%);
  text-align: center;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -40px; left: 10%;
    width: 200px; height: 200px;
    background: rgba(255, 255, 255, 0.08);
    border-radius: 50%;
  }
  &::after {
    content: '';
    position: absolute;
    bottom: -60px; right: 15%;
    width: 240px; height: 240px;
    background: rgba(255, 255, 255, 0.06);
    border-radius: 50%;
  }
}
.about-inner {
  max-width: 720px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}
.about-desc {
  font-size: 16px;
  line-height: 2;
  color: rgba(255, 255, 255, 0.95);
  margin: 20px 0 32px;
}
.about :deep(.el-button--primary) {
  background: #fff;
  border-color: #fff;
  color: $primary-dark;
  font-weight: 600;

  &:hover {
    background: #f0fdfa;
    border-color: #f0fdfa;
    color: $primary;
  }
}

/* 页脚 */
.footer {
  background: #1f2937;
  padding: 48px 24px 32px;
}
.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
}
.footer-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 12px;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}
.footer-logo {
  width: 28px; height: 28px;
}
.footer-text {
  color: #9ca3af;
  font-size: 14px;
  margin-bottom: 8px;
}
.footer-copy {
  color: #6b7280;
  font-size: 13px;
}

/* 响应式 */
@media (max-width: 992px) {
  .hero-inner { flex-direction: column; text-align: center; }
  .hero-visual { flex: none; }
  .hero-actions, .hero-stats { justify-content: center; }
  .feature-grid { grid-template-columns: repeat(2, 1fr); }
  .article-grid { grid-template-columns: repeat(2, 1fr); }
  .hero-title { font-size: 36px; }
}
@media (max-width: 640px) {
  .nav-links { display: none; }
  .feature-grid { grid-template-columns: 1fr; }
  .article-grid { grid-template-columns: 1fr; }
  .hero-title { font-size: 28px; }
  .hero-subtitle { font-size: 15px; }
}
</style>
