<template>
  <div class="detail-page">
    <header class="topbar">
      <div class="topbar-inner">
        <div class="brand" @click="goHome">
          <img src="@/assets/images/smile.svg" alt="logo" class="brand-logo" />
          <span class="brand-name">心晴 MindHaven</span>
        </div>
        <nav class="nav-links">
          <a class="nav-link" @click="goHome">首页</a>
          <a class="nav-link" @click="goConsultation">AI咨询</a>
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

    <div class="articleDetail-container" v-loading="loading">
      <div class="header-section">
        <div class="header-content">
          <span class="header-icon">📖</span>
          <div>
            <h1>文章详情</h1>
            <p>知识就是力量</p>
          </div>
        </div>
      </div>

      <div class="content" v-if="article">
        <div class="diary-card">
          <div class="title">文章阅读</div>

          <div class="sub-title flex-box">
            <el-tag class="category-tag" v-if="categoryName" type="info">{{ categoryName }}</el-tag>
            <div class="item">
              <el-icon><User /></el-icon>
              <span>{{ article.authorName || article.author?.name || '心晴' }}</span>
            </div>
            <div class="item">
              <el-icon><Clock /></el-icon>
              <span>{{ formatTime(article.publishTime || article.createTime || article.createdAt) }}</span>
            </div>
            <div class="item">
              <el-icon><View /></el-icon>
              <span>{{ article.readCount || article.viewCount || 0 }} 阅读</span>
            </div>
          </div>

          <h2 class="article-title">{{ article.title }}</h2>

          <div class="summary-content" v-if="summary">
            <span class="summary-label">摘要</span>
            <p>{{ summary }}</p>
          </div>

          <div class="content-wrapper" v-html="article.content || article.contentHtml || ''" />

          <div class="tags-content" v-if="tags.length">
            <div class="tags-title">🏷 文章标签</div>
            <div class="tags-list">
              <el-tag v-for="t in tags" :key="t" size="default">{{ t }}</el-tag>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && !article" description="文章不存在" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Clock, View } from '@element-plus/icons-vue'
import { ElMessageBox, ElLoading } from 'element-plus'
import { logout } from '@/api/user'
import { getArticleDetail } from '@/api/knowledge'

const route = useRoute()
const router = useRouter()

const loggedIn = ref(!!localStorage.getItem('token'))
const readUserInfo = () => { try { return JSON.parse(localStorage.getItem('userInfo') || '{}') } catch { return {} } }
const userInfo = readUserInfo()
const username = userInfo.nickname || userInfo.username || '用户'

const loading = ref(false)
const article = ref(null)

const categoryName = computed(() => article.value?.categoryName || article.value?.category?.name || '')
const summary = computed(() => article.value?.summary || '')
const tags = computed(() => {
  if (!article.value) return []
  return article.value.tagArray || article.value.tags?.split(/[,，\s]+/).filter(Boolean) || []
})

const formatTime = (ts) => {
  if (!ts) return ''
  const d = new Date(typeof ts === 'number' ? ts : Date.parse(ts))
  if (isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}/${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const fetchArticle = async () => {
  const id = route.params.id
  if (!id) return
  loading.value = true
  try {
    const res = await getArticleDetail(id)
    article.value = res.data || res
  } catch (err) {
    console.error('[文章详情] 加载失败：', err)
  } finally {
    loading.value = false
  }
}

const goHome = () => router.push('/')
const goConsultation = () => router.push('/consultation')
const goDiary = () => router.push('/diary')
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
    router.push('/auth/login')
  }).catch(() => {})
}

onMounted(fetchArticle)
</script>

<style lang="scss" scoped>
$primary: #14b8a6;
$primary-dark: #0d9488;
$accent: #f97316;
$text-dark: #1f2937;
$text-body: #4b5563;
$text-muted: #9ca3af;

.detail-page {
  min-height: 100vh;
  background: #f9fafb;
}

.topbar {
  height: 64px; background: #fff; border-bottom: 1px solid #e5e7eb;
  position: sticky; top: 0; z-index: 10;
}
.topbar-inner {
  max-width: 1400px; height: 100%; margin: 0 auto; padding: 0 28px;
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
.nav-link:hover { color: $primary; font-weight: 600; }
.nav-actions { display: flex; align-items: center; gap: 14px;
  .nav-username { font-size: 15px; color: $text-dark; }
  :deep(.el-button) { border-radius: 20px; }
  :deep(.el-button--primary) { background: $primary; border-color: $primary;
    &:hover { background: $primary-dark; border-color: $primary-dark; }
  }
}

.articleDetail-container {
  background: linear-gradient(160deg, #fef3f2 0%, #f0fdfa 60%, #f5f3ff 100%);

  .header-section {
    background: linear-gradient(135deg, #14b8a6 0%, #2dd4bf 50%, #f97316 100%);
    color: white; padding: 48px; text-align: center;
    .header-content {
      display: flex; align-items: center; justify-content: center; gap: 12px;
      .header-icon { font-size: 36px; }
      h1 { font-size: 30px; font-weight: 800; margin: 0; }
      p { font-size: 15px; opacity: 0.9; margin: 6px 0 0; }
    }
  }

  .content {
    margin: 0 auto; width: 980px; max-width: 100%; padding: 20px;
    .diary-card {
      background: rgba(255,255,255,0.8); backdrop-filter: blur(8px);
      border-radius: 14px; border: 1px solid rgba(255,255,255,0.6);
      box-shadow: 0 6px 28px rgba(20,184,166,0.08);
      padding: 30px;
      margin-bottom: 20px;

      .title { margin-bottom: 16px; font-size: 20px; font-weight: 600; color: #374151; }

      .flex-box { display: flex; align-items: center; flex-wrap: wrap; gap: 16px; margin-bottom: 20px;
        .item { display: flex; align-items: center; gap: 4px; font-size: 14px; color: $text-muted; }
        .category-tag { margin-right: 4px; }
      }

      .article-title { font-size: 28px; font-weight: 800; color: #111827; margin: 24px 0 12px; line-height: 1.4; }

      .summary-content {
        background: rgba(20, 184, 166, 0.08);
        border-left: 4px solid $primary;
        padding: 14px 18px;
        border-radius: 0 10px 10px 0;
        margin-bottom: 24px;
        .summary-label { font-size: 13px; font-weight: 700; color: $primary-dark; display: block; margin-bottom: 6px; }
        p { font-size: 14px; color: $text-body; line-height: 1.7; margin: 0; }
      }

      .content-wrapper {
        font-size: 15px; line-height: 1.85; color: #374151;
        :deep(p) { margin-bottom: 12px; }
        :deep(h1),:deep(h2),:deep(h3),:deep(h4),:deep(h5),:deep(h6) {
          margin: 20px 0 10px; color: #111827; font-weight: 700;
        }
        :deep(h2) { font-size: 20px; border-bottom: 2px solid #e5e7eb; padding-bottom: 6px; }
        :deep(h3) { font-size: 17px; }
        :deep(ul),:deep(ol) { padding-left: 20px; margin-bottom: 12px; }
        :deep(li) { margin-bottom: 6px; }
        :deep(blockquote) {
          border-left: 3px solid $primary; padding: 8px 16px; margin: 12px 0;
          background: rgba(20,184,166,0.04); border-radius: 0 8px 8px 0; color: #6b7280;
        }
        :deep(img) { max-width: 100%; border-radius: 8px; margin: 12px 0; }
      }

      .tags-content {
        margin-top: 24px; padding-top: 18px; border-top: 1px solid #e5e7eb;
        .tags-title { margin-bottom: 10px; font-size: 14px; font-weight: 600; color: #374151; }
        .tags-list { display: flex; flex-wrap: wrap; gap: 8px; }
      }
    }
  }
}
</style>