import request from '@/utils/request'

// 分页查询咨询会话（用户端）
export const getSessionPage = (params) =>
  request.get('/psychological-chat/sessions', { params })

// 消息列表/情绪分析用数字 id（去掉 session_ 前缀）
const numericSessionId = (id) => {
  if (!id) return ''
  const sid = String(id)
  return sid.startsWith('session_') ? sid.slice(8) : sid
}

// 获取会话消息列表（自动尝试两种路径）
export const getSessionMessages = async (sessionId) => {
  const nid = numericSessionId(sessionId)
  const paths = [
    `/psychological-chat/sessions/${nid}/messages`,
    `/psychological-chat/session/${nid}/messages`,
  ]
  for (const path of paths) {
    try {
      return await request.get(path)
    } catch (err) {
      console.warn(`[消息列表] ${path} 失败：`, err.message)
      if (paths.indexOf(path) === paths.length - 1) throw err
    }
  }
}

// 获取会话情绪分析结果
export const getSessionEmotion = (sessionId) =>
  request.get(`/psychological-chat/session/${normalizeSessionId(sessionId)}/emotion`)

// 新建咨询会话
export const createSession = (data) =>
  request.post('/psychological-chat/session/start', data)

// 删除咨询会话
export const deleteSession = (sessionId) =>
  request.delete(`/psychological-chat/sessions/${sessionId}`)

// 规范化 sessionId：历史会话列表返回数字 id，新建会话返回 session_xxx 字符串
// 流式对话接口需要 session_xxx 格式
const normalizeSessionId = (id) => {
  if (!id) return ''
  const sid = String(id)
  return sid.startsWith('session_') ? sid : `session_${sid}`
}

// 流式对话（SSE）
export const streamChat = (sessionId, userMessage, onMessage) => {
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  const sid = normalizeSessionId(sessionId)
  return fetch(`${baseURL}/psychological-chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream',
      'token': localStorage.getItem('token') || '',
    },
    body: JSON.stringify({ sessionId: sid, userMessage }),
  }).then(async (response) => {
    console.log('[stream] HTTP', response.status, 'contentType:', response.headers.get('content-type'))
    if (!response.ok) {
      const errText = await response.text().catch(() => '')
      console.error('[stream] HTTP错误：', errText)
      throw new Error(`HTTP ${response.status} ${errText}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let fullText = ''
    let currentEvent = ''
    let gotRealContent = false
    let chunkIndex = 0

    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        console.log('[stream] 流结束, gotRealContent:', gotRealContent, 'fullText:', fullText)
        break
      }
      const chunk = decoder.decode(value, { stream: true })
      console.log(`[stream] chunk#${chunkIndex++}:`, JSON.stringify(chunk))
      buffer += chunk
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed) {
          currentEvent = ''
          continue
        }

        // event: 前缀
        if (trimmed.startsWith('event:')) {
          currentEvent = trimmed.slice(6).trim()
          continue
        }

        // data: 前缀
        if (trimmed.startsWith('data:')) {
          const payload = trimmed.slice(5).trim()
          if (currentEvent === 'done' || payload === '[DONE]') continue
          if (currentEvent === 'error') {
            throw new Error(payload || '服务端返回错误事件')
          }
          let isJson = false
          try {
            const parsed = JSON.parse(payload)
            isJson = true
            // 业务错误检测
            if (parsed.code !== undefined && String(parsed.code) !== '0' && String(parsed.code) !== '200' && String(parsed.code) !== 'success') {
              throw new Error(parsed.msg || parsed.message || '对话服务异常')
            }
            // 提取文本：后端格式 {code,msg,data:{type,content}}
            // 优先从 data 子对象取内容
            let text = ''
            if (parsed.data && typeof parsed.data === 'object') {
              text = parsed.data.content || parsed.data.text || parsed.data.message || parsed.data.delta || parsed.data.reply || parsed.data.answer || parsed.data.response || ''
            } else if (typeof parsed.data === 'string') {
              text = parsed.data
            }
            // 兜底：顶层字段
            if (!text) {
              text = parsed.content || parsed.text || parsed.delta || parsed.reply || parsed.answer || parsed.response || ''
            }
            // 排除 msg="操作成功" 这种非内容字段
            if (typeof text !== 'string') text = String(text || '')
            if (text) {
              gotRealContent = true
              fullText += text
              onMessage && onMessage(text, fullText)
            } else if (isJson) {
              console.log('[stream] JSON解析成功但未取到文本字段, keys:', Object.keys(parsed), '完整:', payload)
            }
          } catch (e) {
            if (isJson) throw e
            if (payload && payload !== '[DONE]') {
              gotRealContent = true
              fullText += payload
              onMessage && onMessage(payload, fullText)
            }
          }
          continue
        }

        // 无前缀
        let isJson2 = false
        try {
          const parsed = JSON.parse(trimmed)
          isJson2 = true
          if (parsed.code !== undefined && String(parsed.code) !== '0' && String(parsed.code) !== '200' && String(parsed.code) !== 'success') {
            throw new Error(parsed.msg || parsed.message || '对话服务异常')
          }
          let text = ''
          if (parsed.data && typeof parsed.data === 'object') {
            text = parsed.data.content || parsed.data.text || parsed.data.message || parsed.data.delta || parsed.data.reply || parsed.data.answer || parsed.data.response || ''
          } else if (typeof parsed.data === 'string') {
            text = parsed.data
          }
          if (!text) {
            text = parsed.content || parsed.text || parsed.delta || parsed.reply || parsed.answer || parsed.response || ''
          }
          if (typeof text !== 'string') text = String(text || '')
          if (text) {
            gotRealContent = true
            fullText += text
            onMessage && onMessage(text, fullText)
          } else if (isJson2) {
            console.log('[stream] 无前缀JSON但未取到文本字段, keys:', Object.keys(parsed), '完整:', trimmed)
          }
        } catch (e) {
          if (isJson2) throw e
          if (trimmed) {
            gotRealContent = true
            fullText += trimmed
            onMessage && onMessage(trimmed, fullText)
          }
        }
      }
    }

    if (!gotRealContent) {
      throw new Error('对话返回为空')
    }
    return fullText
  })
}
