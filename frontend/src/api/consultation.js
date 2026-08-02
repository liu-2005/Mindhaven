import request from '@/utils/request'

// 分页查询咨询会话（用户端）
export const getSessionPage = (params) =>
  request.get('/chat/sessions', { params })

// 会话详情
export const getSessionDetail = (sessionId) =>
  request.get(`/chat/sessions/${sessionId}`)

// 获取会话消息列表
export const getSessionMessages = (sessionId, params = {}) =>
  request.get(`/chat/sessions/${sessionId}/messages`, { params })

// 获取会话情绪分析结果
export const getSessionEmotion = (sessionId) =>
  request.get(`/chat/sessions/${sessionId}/emotion`)

// 新建咨询会话
export const createSession = (data) =>
  request.post('/chat/sessions', data)

// 更新会话（重命名/归档）
export const updateSession = (sessionId, data) =>
  request.patch(`/chat/sessions/${sessionId}`, data)

// 删除咨询会话
export const deleteSession = (sessionId) =>
  request.delete(`/chat/sessions/${sessionId}`)

// 流式对话（SSE）——固定协议：message / risk / done / error
export const streamChat = (sessionId, userMessage, { onMessage, onRisk, onDone } = {}) => {
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  const requestId = crypto.randomUUID ? crypto.randomUUID() : `req-${Date.now()}-${Math.random()}`
  return fetch(`${baseURL}/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream',
      'Authorization': `Bearer ${localStorage.getItem('token') || ''}`,
    },
    body: JSON.stringify({ sessionId, requestId, message: userMessage }),
  }).then(async (response) => {
    if (!response.ok) {
      const errText = await response.text().catch(() => '')
      throw new Error(errText || `HTTP ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let fullText = ''
    let currentEvent = ''
    let gotRealContent = false

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed) {
          currentEvent = ''
          continue
        }
        if (trimmed.startsWith('event:')) {
          currentEvent = trimmed.slice(6).trim()
          continue
        }
        if (trimmed.startsWith('data:')) {
          const payload = trimmed.slice(5).trim()
          if (currentEvent === 'done') {
            if (onDone) {
              try { onDone(JSON.parse(payload)) } catch { onDone({}) }
            }
            continue
          }
          if (currentEvent === 'risk') {
            if (onRisk) {
              try { onRisk(JSON.parse(payload)) } catch { onRisk({}) }
            }
            continue
          }
          if (currentEvent === 'error') {
            throw new Error(payload || '服务端返回错误事件')
          }
          try {
            const parsed = JSON.parse(payload)
            const text = parsed.delta || parsed.content || ''
            if (text) {
              gotRealContent = true
              fullText += text
              onMessage && onMessage(text, fullText)
            }
          } catch {
            if (payload) {
              gotRealContent = true
              fullText += payload
              onMessage && onMessage(payload, fullText)
            }
          }
          continue
        }
      }
    }

    if (!gotRealContent) {
      throw new Error('对话返回为空')
    }
    return fullText
  })
}
