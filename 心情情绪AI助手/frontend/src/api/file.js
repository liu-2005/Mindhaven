import request from '@/utils/request'

/**
 * 文件上传
 * POST /file/upload
 *
 * @param {Object} params
 * @param {File} params.file - 文件主体
 * @param {string} params.businessType - 类型，如 ARTICLE
 * @param {string} params.businessId - 关联业务id
 * @param {string} params.businessField - 字段名，如 cover
 * @returns 返回上传后的文件信息
 */
export const uploadFile = ({ file, businessType, businessId, businessField }) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('businessType', businessType)
  formData.append('businessId', businessId)
  formData.append('businessField', businessField)

  // 上传文件时必须移除默认的 application/json Content-Type，
  // 让浏览器自动设置 multipart/form-data 并带上 boundary，否则后端解析失败返回 500
  return request.post('/file/upload', formData, {
    headers: { 'Content-Type': undefined }
  })
}
