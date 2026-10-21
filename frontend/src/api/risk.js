import request from '@/utils/request'
export const getRiskEvents = (params) => request.get('/admin/risk-events', { params })
export const getRiskDetail = (id, reason) => request.get(`/admin/risk-events/${id}`, { params: { reason } })
export const updateRiskStatus = (id, data) => request.put(`/admin/risk-events/${id}/status`, data)
