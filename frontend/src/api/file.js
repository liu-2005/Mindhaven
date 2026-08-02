import request from '@/utils/request'

/**
 * 文件上传
 * POST /files
 *
 * @param {Object} params
 * @param {File} params.file - 文件主体
 * @param {string} params.businessType - 类型，如 ARTICLE
 * @param {string} params.businessId - 关联业务id
 * @param {string} params.businessField - 字段名，如 cover
 * @returns 返回上传后的文件信息 {fileId, filePath}
 */
export const uploadFile = ({ file, businessType, businessId, businessField }) => {
  const formData = new FormData()
  formData.append('file', file)
  if (businessType) formData.append('businessType', businessType)
  if (businessId !== undefined && businessId !== null) formData.append('businessId', businessId)
  if (businessField) formData.append('businessField', businessField)

  // 上传文件时必须移除默认的 application/json Content-Type，
  // 让浏览器自动设置 multipart/form-data 并带上 boundary
  return request.post('/files', formData, {
    headers: { 'Content-Type': undefined }
  })
}
