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
        meta: { title: '咨询记录', icon: 'ChatDotRound' },
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

// 读取当前登录用户类型：1=前台 2=后台，无登录返回 0
const getUserType = () => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  return Number(userInfo.userType) || 0
}

// 登录后的默认落地页
const defaultPathByType = (type) => (type === 2 ? '/back/dashboard' : '/')

// 是否后台路径
const isBackPath = (path) => path.startsWith('/back')

// 全局前置守卫：鉴权 + 登录态跳转 + 身份权限校验
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userType = getUserType()

  // 1. 未登录
  if (!token) {
    // 访问后台或其它受保护页 → 跳登录页并记录回跳
    if (to.meta.requiresAuth || isBackPath(to.path)) {
      ElMessage.warning('请先登录')
      return next({ path: '/auth/login', query: { redirect: to.fullPath } })
    }
    // 访问首页/登录/注册 → 正常放行
    return next()
  }

  // 2. 已登录
  // 2a. 访问登录/注册页 → 按身份回到对应首页
  if (to.meta.isAuthPage) {
    return next({ path: defaultPathByType(userType) })
  }

  // 2b. 前台用户（userType=1）访问后台 → 跳首页
  if (isBackPath(to.path) && userType !== 2) {
    ElMessage.warning('无权限访问后台')
    return next({ path: '/' })
  }

  // 2c. 后台管理员访问首页 → 可放行也可跳后台，这里放行让管理员也能看前台
  // 其它情况正常放行
  next()
})

export default router