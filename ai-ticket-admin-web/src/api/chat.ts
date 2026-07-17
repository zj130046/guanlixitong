import { http } from './client'

export function createConversation() {
  return http.post<{ conversationId: string }, { conversationId: string }>('/chat/conversations')
}

export function sendMessage(message: string) {
  return http.post<{ answer: string }, { answer: string }>('/chat/messages', { message })
}

export function transferHuman(conversationId?: string) {
  return http.post<{ conversationId: string; status: string }, { conversationId: string; status: string }>('/chat/transfer-human', { conversationId })
}
