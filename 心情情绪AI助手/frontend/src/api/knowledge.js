import request from '@/utils/request'

// 获取分类树
export const getCategoryTree = () => request.get('/knowledge/category/tree')

// 知识文章分页列表
export const getArticlePage = (params, options = {}) =>
  request.get('/knowledge/article/page', { params, ...options })

// 获取知识文章详情
export const getArticleDetail = (id) => request.get(`/knowledge/article/${id}`)

// 新增文章
export const addArticle = (data) => request.post('/knowledge/article', data)

// 更新文章
export const updateArticle = (id, data) => request.put(`/knowledge/article/${id}`, data)

// 更新文章状态（1发布 2下线）
export const updateArticleStatus = (id, status) =>
  request.put(`/knowledge/article/${id}/status`, { status })

// 删除文章
export const deleteArticle = (id) => request.delete(`/knowledge/article/${id}`)
