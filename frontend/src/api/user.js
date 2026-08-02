import request from '@/utils/request'

// 用户登录
export const login = (data) => request.post('/auth/login', data)

// 用户注册
export const register = (data) => request.post('/auth/register', data)

// 获取当前用户信息
export const getUserInfo = () => request.get('/users/me')

// 更新当前用户资料
export const updateUserProfile = (data) => request.patch('/users/me', data)

// 退出登录
export const logout = () => request.post('/auth/logout')

// 分页查询用户（管理端）
export const getUserPage = (params) =>
  request.get('/admin/users', { params })
