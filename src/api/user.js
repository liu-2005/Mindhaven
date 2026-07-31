import request from '@/utils/request'

// 用户登录
export const login = (data) => request.post('/user/login', data)

// 用户注册
export const register = (data) => request.post('/register', data)

// 获取当前用户信息
export const getUserInfo = () => request.get('/user/info')

// 退出登录
export const logout = () => request.post('/user/logout')

// 分页查询用户
export const getUserPage = (params) =>
  request.get('/user/page', { params })
