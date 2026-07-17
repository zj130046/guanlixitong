import { http } from './client'

export interface ChatConversation {
  id: number
  userId?: number
  status: string
  transferredTicketId?: number
  title?: string
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  id: number
  conversationId: number
  senderType: 'USER' | 'AI' | 'SYSTEM' | 'AGENT'
  senderId?: number
  content: string
  createdAt: string
}

export function createConversation() {
  return http.post<{ conversationId: number; status: string }, { conversationId: number; status: string }>('/chat/conversations')
}

export function listConversations() {
  return http.get<ChatConversation[], ChatConversation[]>('/chat/conversations')
}

export function deleteConversation(id: number | string) {
  return http.delete<{ success: boolean }, { success: boolean }>(`/chat/conversations/${id}`)
}

export function listMessages(conversationId: number | string) {
  return http.get<ChatMessage[], ChatMessage[]>(`/chat/conversations/${conversationId}/messages`)
}

export function sendMessage(conversationId: number | undefined, message: string) {
  return http.post<{ conversationId: number; answer: string; matchType?: string; faqId?: number },
    { conversationId: number; answer: string; matchType?: string; faqId?: number }>(
    '/chat/messages', { conversationId, message }
  )
}

export function transferHuman(conversationId?: number) {
  return http.post<{ conversationId: number; ticketId: number; status: string },
    { conversationId: number; ticketId: number; status: string }>(
    '/chat/transfer-human', { conversationId }
  )
}

/**
 * 流式发送消息（SSE）
 * 返回 Promise，流式过程中通过回调逐 token 更新
 */
export function streamSendMessage(
  message: string,
  conversationId?: number,
  onToken?: (token: string) => void
): Promise<{ conversationId: number; answer: string }> {
  const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
  const token = localStorage.getItem('AI_TICKET_USER_TOKEN') || ''
  const url = `${baseURL}/chat/messages/stream?message=${encodeURIComponent(message)}${conversationId ? '&conversationId=' + conversationId : ''}`

  return new Promise((resolve, reject) => {
    fetch(url, { headers: { 'Authorization': token } })
      .then(response => {
        if (!response.ok) {
          response.text().then(text => {
            reject(new Error(text || '网络请求失败'))
          }).catch(() => reject(new Error('网络请求失败')))
          return
        }
        const reader = response.body!.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        let fullAnswer = ''
        let conversationIdResult = 0
        let currentEvent = 'message'

        function read() {
          reader.read().then(({ done, value }) => {
            if (done) {
              resolve({ conversationId: conversationIdResult, answer: fullAnswer })
              return
            }
            buffer += decoder.decode(value, { stream: true })
            const lines = buffer.split('\n')
            buffer = lines.pop() || ''

            for (const line of lines) {
              if (line.startsWith('event:')) {
                currentEvent = line.substring(6).trim()
                continue
              }
              if (line.startsWith('data:')) {
                const data = line.substring(5).trim()
                if (data === '[DONE]') continue

                if (currentEvent === 'error') {
                  reject(new Error(data))
                  return
                }

                if (data.startsWith('{')) {
                  try {
                    const obj = JSON.parse(data)
                    if (obj.answer !== undefined) {
                      fullAnswer = obj.answer
                      conversationIdResult = obj.conversationId
                    }
                  } catch {
                    // 普通 token
                    fullAnswer += data
                    onToken?.(data)
                  }
                } else if (data) {
                  fullAnswer += data
                  onToken?.(data)
                }
              }
            }
            read()
          }).catch(reject)
        }
        read()
      })
      .catch(reject)
  })
}
