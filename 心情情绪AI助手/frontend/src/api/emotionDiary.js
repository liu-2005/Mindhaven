import request from "@/utils/request"

// 分页查询情绪日记（管理端）
export const getEmotionDiaryPage = (params) =>
  request.get("/emotion-diary/admin/page", { params })

// 删除情绪日记（管理端）
export const deleteEmotionDiary = (id) =>
  request.delete(`/emotion-diary/admin/${id}`)

// 创建或更新情绪日记（用户端）
export const saveEmotionDiary = (data) =>
  request.post("/emotion-diary", data)