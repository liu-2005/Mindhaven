import request from '@/utils/request'

// 获取分类树
export const getCategoryTree = () => request.get('/knowledge/categories')

// 公开文章分页列表
export const getArticlePage = (params, options = {}) =>
  request.get('/knowledge/articles', { params, ...options })

// 获取公开文章详情
export const getArticleDetail = (id) => request.get(`/knowledge/articles/${id}`)

// 管理端文章详情（含草稿）
export const getAdminArticleDetail = (id) => request.get(`/admin/knowledge/articles/${id}`)

// 新增文章（管理端）
export const addArticle = (data) => request.post('/admin/knowledge/articles', data)

// 更新文章（管理端）
export const updateArticle = (id, data) => request.put(`/admin/knowledge/articles/${id}`, data)

// 更新文章状态（DRAFT/PUBLISHED/OFFLINE/ARCHIVED）
export const updateArticleStatus = (id, status) =>
  request.put(`/admin/knowledge/articles/${id}/status`, { status })

// 删除文章（管理端）
export const deleteArticle = (id) => request.delete(`/admin/knowledge/articles/${id}`)
