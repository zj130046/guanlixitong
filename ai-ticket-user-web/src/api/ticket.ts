import { http } from './client'

export interface Ticket {
  id: number
  title: string
  description: string
  category?: string
  department?: string
  priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'
  status: string
  source: string
  userId?: number
  assigneeAgentId?: number
  assignee?: string
  requesterName?: string
  conversationId?: number
  timeoutAt?: string
  completedAt?: string
  createdAt: string
  updatedAt: string
  events?: TicketEvent[]
}

export interface TicketEvent {
  id: number
  ticketId: number
  eventType: string
  fromStatus?: string
  toStatus?: string
  operatorType: string
  operatorId?: number
  remark?: string
  createdAt: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

// ========== 用户端 ==========

export function createTicket(data: {
  title: string
  description: string
  category?: string
  department?: string
  priority?: string
}) {
  return http.post<Ticket, Ticket>('/user/tickets', data)
}

export function listUserTickets(params: {
  page?: number
  size?: number
  status?: string
  keyword?: string
}) {
  return http.get<PageResult<Ticket>, PageResult<Ticket>>('/user/tickets', { params })
}

export function getTicket(id: number | string) {
  return http.get<Ticket, Ticket>(`/user/tickets/${id}`)
}

// 满意度提交
export function submitSatisfaction(data: { ticketId: number; userId?: number; score: number; comment?: string }) {
  return http.post('/statistics/satisfaction', data)
}

// ========== 状态/优先级映射 ==========

export const statusColor: Record<string, string> = {
  CREATED: 'info',
  ASSIGNED: 'warning',
  ACCEPTED: 'primary',
  PROCESSING: 'primary',
  FOLLOWING: 'primary',
  COMPLETED: 'success',
  REJECTED: 'danger',
  ARCHIVED: 'info'
}

export const statusLabel: Record<string, string> = {
  CREATED: '已创建',
  ASSIGNED: '待接单',
  ACCEPTED: '已接单',
  PROCESSING: '处理中',
  FOLLOWING: '跟进中',
  COMPLETED: '已完结',
  REJECTED: '已驳回',
  ARCHIVED: '已归档'
}

export const priorityLabel: Record<string, string> = {
  LOW: '低',
  NORMAL: '普通',
  HIGH: '高',
  URGENT: '紧急'
}

export const priorityColor: Record<string, string> = {
  LOW: 'info',
  NORMAL: '',
  HIGH: 'warning',
  URGENT: 'danger'
}
