import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import BackendLayout from '@/components/BackendLayout.vue'

const routes = [
  {
    path: '/auth',
    redirect: '/auth/login',
    children: [
      {
        path: 'login',
        name: 'Login',
        component: () => import('@/views/Login.vue'),
        meta: { isAuthPage: true },
      },
      {
        path: 'register',
        name: 'Register',
        component: () => import('@/views/Register.vue'),
        meta: { isAuthPage: true },
      },
    ],
  },
  {
    path: '/',
    name: 'FrontHome',
    component: () => import('@/views/FrontHome.vue'),
  },
  {
    path: '/consultation',
    name: 'Consultation',
    component: () => import('@/views/Consultation.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/diary',
    name: 'Diary',
    component: () => import('@/views/Diary.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/knowledge',
    name: 'KnowledgeFront',
    component: () => import('@/views/KnowledgeFront.vue'),
  },
  {
    path: '/article/:id',
    name: 'ArticleDetail',
    component: () => import('@/views/ArticleDetail.vue'),
  },
  {
    path: '/back',
    component: BackendLayout,
    redirect: '/back/dashboard',
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据分析', icon: 'DataAnalysis' },
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/Knowledge.vue'),
        meta: { title: '知识文章', icon: 'Document' },
      },
      {
        path: 'consultations',
        name: 'Consultations',
        component: () => import('@/views/Consultations.vue'),
        meta: { title: '我的咨询记录', icon: 'ChatDotRound' },
      },
      {
        path: 'risk-events',
        name: 'RiskEvents',
        component: () => import('@/views/RiskEvents.vue'),
        meta: { title: '风险工作台', icon: 'ChatDotRound', permission: 'risk:read' },
      },
      {
        path: 'emotional',
        name: 'Emotional',
        component: () => import('@/views/Emotional.vue'),
        meta: { title: '情绪日记', icon: 'Sunny' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

const ADMIN_ROLES = ['CONTENT_ADMIN', 'SAFETY_ADMIN', 'USER_ADMIN']

// 读取当前登录用户信息（来自服务端）
const readUserInfo = () => {
  try { return JSON.parse(localStorage.getItem('userInfo') || '{}') } catch { return {} }
}

// 是否管理员角色
const isAdmin = (userInfo) => (userInfo.roles || []).some(r => ADMIN_ROLES.includes(r))

// 是否后台路径
const isBackPath = (path) => path.startsWith('/back')

// 全局前置守卫：鉴权 + 登录态跳转 + 身份权限校验
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userInfo = readUserInfo()

  // 1. 未登录
  if (!token) {
    if (to.meta.requiresAuth || isBackPath(to.path)) {
      ElMessage.warning('请先登录')
      return next({ path: '/auth/login', query: { redirect: to.fullPath } })
    }
    return next()
  }

  // 2. 已登录
  // 2a. 访问登录/注册页 → 按身份回到对应首页
  if (to.meta.isAuthPage) {
    return next({ path: isAdmin(userInfo) ? '/back/dashboard' : '/' })
  }

  // 2b. 非管理员访问后台 → 提示无权限
  if (isBackPath(to.path) && !isAdmin(userInfo)) {
    ElMessage.warning('无权限访问后台')
    return next({ path: '/' })
  }

  next()
})

export default router
