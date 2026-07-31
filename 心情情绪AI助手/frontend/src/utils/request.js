import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
})

// 请求拦截器：自动携带 token
service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      // 后端约定 header 名为 token
      config.headers.token = token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器：统一处理响应和错误
service.interceptors.response.use(
  response => {
    // HTTP 状态码 2xx 即视为请求成功，直接返回后端数据
    const res = response.data
    // 兼容后端用 code 表示业务状态:
    //   成功: code === 0 / 200 / "0" / "200" / "success"
    //   失败: code === "-1" / 401 / 其他非成功值
    if (res && res.code !== undefined && res.code !== null) {
      const code = String(res.code)
      const successCodes = ['0', '200', 'success']
      if (!successCodes.includes(code)) {
        if (!response.config?.silent) ElMessage.error(res.message || res.msg || '请求失败')
        // 401: token 失效或未登录
        if (code === '401') {
          localStorage.removeItem('token')
        }
        return Promise.reject(new Error(res.message || res.msg || 'Error'))
      }
    }
    return res
  },
  error => {
    const status = error.response?.status
    // 支持单个请求静默：config.silent = true 时不弹错误提示
    const silent = error.config?.silent
    let message = '网络错误，请稍后重试'
    switch (status) {
      case 400: message = '请求参数错误'; break
      case 401: message = '未授权，请重新登录'; break
      case 403: message = '拒绝访问'; break
      case 404: message = '请求资源不存在'; break
      case 500: message = '服务器内部错误'; break
      case 502: message = '网关错误'; break
      case 504: message = '网关超时'; break
    }
    if (!silent) ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default service
