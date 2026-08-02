import request from "@/utils/request"

// 新建情绪日记
export const createDiary = (data) => request.post("/diaries", data)

// 查询自己的日记（分页）
export const getDiaryPage = (params) => request.get("/diaries", { params })

// 日记详情
export const getDiaryDetail = (id) => request.get(`/diaries/${id}`)

// 更新日记
export const updateDiary = (id, data) => request.put(`/diaries/${id}`, data)

// 删除日记
export const deleteDiary = (id) => request.delete(`/diaries/${id}`)

// 个人趋势聚合
export const getDiaryTrends = (params) => request.get("/diaries/trends", { params })

// 分页查询情绪日记（管理端）
export const getEmotionDiaryPage = (params) =>
  request.get("/admin/diaries", { params })

// 删除情绪日记（管理端）
export const deleteEmotionDiary = (id) =>
  request.delete(`/admin/diaries/${id}`)
