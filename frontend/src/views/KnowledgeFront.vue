<template>
  <div class="knowledge-page">
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
          <a class="nav-link active">知识库</a>
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

    <div class="knowledge-container">
      <div class="header-section">
        <div class="header-content">
          <span class="header-icon">📚</span>
          <div>
            <h1>知识库</h1>
            <p>靠谱的心理学内容，从这里开始</p>
          </div>
        </div>
      </div>

      <div class="content" v-loading="loading">
        <!-- 推荐侧栏 -->
        <aside class="recommend-section">
          <div class="section-title">
            <span>🔥 热门推荐</span>
          </div>
          <div class="recommend-list">
            <div
              v-for="item in recommends"
              :key="item.id"
              class="recommend-item"
              @click="goDetail(item.id)"
            >
              <div class="rec-title">{{ item.title }}</div>
              <div class="read-count">
                <el-icon><View /></el-icon>
                <span>{{ item.readCount || item.viewCount || 0 }} 阅读</span>
              </div>
            </div>
          </div>
        </aside>

        <!-- 文章列表 -->
        <main class="article-list">
          <template v-if="articles.length">
            <div v-for="art in articles" :key="art.id" class="article-item" @click="goDetail(art.id)">
              <div class="article-cover">
                <img v-if="art.coverImage || art.cover || art.coverUrl" :src="art.coverImage || art.cover || art.coverUrl" :alt="art.title" @error="onCoverError" />
                <el-icon v-else :size="40"><Picture /></el-icon>
              </div>
              <div class="info">
                <div class="title">
                  <h3>{{ art.title }}</h3>
                  <el-tag v-if="art.categoryName || art.category?.name" size="small" type="info">
                    {{ art.categoryName || art.category?.name }}
                  </el-tag>
                </div>
                <p class="summary">{{ art.summary || stripHtml(art.content) }}</p>
                <div class="meta">
                  <span class="meta-author">{{ art.authorName || art.author?.name || '心晴' }}</span>
                  <span class="meta-read">
                    <el-icon><View /></el-icon> {{ art.readCount || art.viewCount || 0 }}
                  </span>
                  <span class="meta-time">{{ formatTime(art.publishTime || art.createTime || art.createdAt) }}</span>
                </div>
              </div>
            </div>
          </template>
          <el-empty v-else description="暂无文章" />
        </main>
      </div>

      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { View, Picture } from '@element-plus/icons-vue'
import { ElMessageBox, ElLoading } from 'element-plus'
import { logout } from '@/api/user'
import { getArticlePage } from '@/api/knowledge'

const router = useRouter()

const loggedIn = ref(!!localStorage.getItem('token'))
const readUserInfo = () => { try { return JSON.parse(localStorage.getItem('userInfo') || '{}') } catch { return {} } }
const userInfo = readUserInfo()
const username = userInfo.nickname || userInfo.username || '用户'

const loading = ref(false)
const articles = ref([])
const recommends = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const fetchArticles = async () => {
  loading.value = true
  try {
    const res = await getArticlePage({ page: pageNum.value, size: pageSize.value }, { silent: true })
    const data = res.data || res
    const list = data.items || []
    articles.value = list
    total.value = data.total ?? list.length
    // 取前 5 篇作为推荐
    recommends.value = list.slice(0, 5).map((a) => ({ id: a.id, title: a.title, readCount: a.viewCount || 0 }))
  } catch (err) {
    console.error('[知识库] 加载失败：', err)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (p) => {
  pageNum.value = p
  fetchArticles()
}

const formatTime = (ts) => {
  if (!ts) return ''
  const d = new Date(typeof ts === 'number' ? ts : Date.parse(ts))
  if (isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}/${pad(d.getMonth() + 1)}/${pad(d.getDate())}`
}

const stripHtml = (html = '') => (html || '').replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').trim().slice(0, 120)

const onCoverError = (e) => { e.target.style.display = 'none' }

const goDetail = (id) => {
  router.push(`/article/${id}`)
}

const goHome = () => router.push('/')
const goConsultation = () => router.push('/consultation')
const goDiary = () => router.push('/diary')
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

onMounted(fetchArticles)
</script>

<style lang="scss" scoped>
$primary: #14b8a6;
$primary-dark: #0d9488;
$accent: #f97316;
$text-dark: #1f2937;
$text-body: #4b5563;
$text-muted: #9ca3af;

.knowledge-page {
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
.nav-link:hover, .nav-link.active { color: $primary; font-weight: 600; }
.nav-actions { display: flex; align-items: center; gap: 14px;
  .nav-username { font-size: 15px; color: $text-dark; }
  :deep(.el-button) { border-radius: 20px; }
  :deep(.el-button--primary) { background: $primary; border-color: $primary;
    &:hover { background: $primary-dark; border-color: $primary-dark; }
  }
}

.knowledge-container {
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
    display: flex; gap: 20px;
    margin: 0 auto; width: 1200px; max-width: 100%; padding: 20px;

    .recommend-section {
      width: 280px; flex-shrink: 0;
      background: rgba(255,255,255,0.8); backdrop-filter: blur(8px);
      border-radius: 14px; border: 1px solid rgba(255,255,255,0.6);
      box-shadow: 0 6px 28px rgba(20,184,166,0.08);
      padding: 20px; height: fit-content;
      position: sticky; top: 84px;

      .section-title {
        font-size: 16px; font-weight: 700; color: #1f2937;
        margin-bottom: 16px; display: flex; align-items: center; gap: 6px;
      }
      .recommend-list {
        display: flex; flex-direction: column; gap: 14px;
        .recommend-item {
          border-left: 3px solid $primary; padding-left: 12px; cursor: pointer;
          transition: border-color 0.2s;
          &:hover { border-color: $accent; }
          .rec-title {
            font-size: 14px; color: $text-dark; font-weight: 500;
            display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
          }
          .read-count {
            margin-top: 8px; font-size: 12px; color: $text-muted;
            display: flex; align-items: center; gap: 4px;
          }
        }
      }
    }

    .article-list {
      flex: 1;
      .article-item {
        background: rgba(255,255,255,0.8); backdrop-filter: blur(8px);
        border-radius: 14px; border: 1px solid rgba(255,255,255,0.6);
        box-shadow: 0 6px 28px rgba(20,184,166,0.06);
        padding: 20px; margin-bottom: 16px; display: flex; gap: 20px;
        cursor: pointer; transition: transform 0.2s, box-shadow 0.2s;
        &:hover { transform: translateY(-3px); box-shadow: 0 12px 36px rgba(20,184,166,0.12); }

        .article-cover {
          width: 180px; height: 120px; flex-shrink: 0;
          border-radius: 10px; overflow: hidden;
          background: linear-gradient(135deg, #f3f4f6, #e5e7eb);
          display: flex; align-items: center; justify-content: center;
          color: #d1d5db;
          img { width: 100%; height: 100%; object-fit: cover; }
        }
        .info {
          flex: 1; display: flex; flex-direction: column;
          .title {
            display: flex; align-items: center; gap: 10px; margin-bottom: 8px;
            h3 { font-size: 18px; font-weight: 700; color: $text-dark; margin: 0; }
          }
          .summary {
            font-size: 14px; line-height: 1.6; color: $text-body; flex: 1;
            display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
            margin-bottom: 12px;
          }
          .meta {
            display: flex; align-items: center; gap: 16px;
            font-size: 13px; color: $text-muted;
            .meta-read { display: flex; align-items: center; gap: 4px; }
          }
        }
      }
    }
  }

  .pagination-wrapper {
    display: flex; justify-content: center; padding: 0 0 40px;
  }
}
</style>