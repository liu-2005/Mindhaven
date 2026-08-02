import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例：基础路径统一为 /api/v1
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
})

// 请求拦截器：自动携带 Bearer token
service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

const successCodes = ['OK']

// 响应拦截器：统一处理响应和错误
service.interceptors.response.use(
  response => {
    const res = response.data
    if (res && res.code !== undefined && res.code !== null) {
      const code = String(res.code)
      if (!successCodes.includes(code)) {
        if (!response.config?.silent) ElMessage.error(res.message || res.msg || '请求失败')
        if (code === 'UNAUTHENTICATED' || code === 'TOKEN_EXPIRED') {
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          if (!response.config?.silent) window.location.href = '/auth/login'
        }
        return Promise.reject(new Error(res.message || res.msg || 'Error'))
      }
    }
    return res
  },
  error => {
    const status = error.response?.status
    const silent = error.config?.silent
    let message = '网络错误，请稍后重试'
    switch (status) {
      case 400: message = '请求参数错误'; break
      case 401:
        message = '未授权，请重新登录'
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        if (!silent) window.location.href = '/auth/login'
        break
      case 403: message = '拒绝访问'; break
      case 404: message = '请求资源不存在'; break
      case 413: message = '文件过大'; break
      case 429: message = '请求过于频繁'; break
      case 500: message = '服务器内部错误'; break
      case 502: message = '网关错误'; break
      case 504: message = '网关超时'; break
    }
    if (!silent) ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default service
