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

// ========== 工单池 ==========

export function ticketPool(params: {
  page?: number
  size?: number
  category?: string
  priority?: string
}) {
  return http.get<PageResult<Ticket>, PageResult<Ticket>>('/agent/tickets/pool', { params })
}

// ========== 我的工单 ==========

export function myTickets(params: {
  page?: number
  size?: number
  status?: string
  keyword?: string
}) {
  return http.get<PageResult<Ticket>, PageResult<Ticket>>('/agent/tickets/mine', { params })
}

// ========== 工单详情 & 操作 ==========

export function getAgentTicket(id: number | string) {
  return http.get<Ticket, Ticket>(`/agent/tickets/${id}`)
}

export function acceptTicket(id: number) {
  return http.post<Ticket, Ticket>(`/agent/tickets/${id}/accept`)
}

export function processTicket(id: number, remark?: string) {
  return http.post<Ticket, Ticket>(`/agent/tickets/${id}/process`, { remark })
}

export function followTicket(id: number, remark?: string) {
  return http.post<Ticket, Ticket>(`/agent/tickets/${id}/follow`, { remark })
}

export function completeTicket(id: number, remark?: string) {
  return http.post<Ticket, Ticket>(`/agent/tickets/${id}/complete`, { remark })
}

export function rejectTicket(id: number, remark?: string) {
  return http.post<Ticket, Ticket>(`/agent/tickets/${id}/reject`, { remark })
}

/** 坐席归档已完结工单 */
export function archiveTicket(id: number, remark?: string) {
  return http.post<Ticket, Ticket>(`/agent/tickets/${id}/archive`, { remark })
}

/** 坐席调整工单优先级 */
export function adjustPriority(id: number, priority: string, remark?: string) {
  return http.post<Ticket, Ticket>(`/agent/tickets/${id}/adjust-priority`, { priority, remark })
}

// ========== 超时预警 ==========

export function timeoutWarnings() {
  return http.get<Ticket[], Ticket[]>('/agent/tickets/warnings')
}

// ========== 统计数据 ==========

export function agentOverview() {
  return http.get<Record<string, any>, Record<string, any>>('/agent/tickets/overview')
}

export function ticketTrend(days = 7) {
  return http.get<any[], any[]>('/agent/tickets/trend', { params: { days } })
}

export function statusDistribution() {
  return http.get<any[], any[]>('/agent/tickets/status-distribution')
}

export function categoryDistribution() {
  return http.get<any[], any[]>('/agent/tickets/category-distribution')
}

// ========== 全局统计（坐席端复用） ==========

/** 满意度统计 */
export function getSatisfactionStats() {
  return http.get<Record<string, any>, Record<string, any>>('/statistics/satisfaction-stats')
}

// ========== 坐席信息 ==========

export function getAgentMe() {
  return http.get<any, any>('/agent/me')
}

export function goOnline() {
  return http.post<any, any>('/agent/online')
}

export function goOffline() {
  return http.post<any, any>('/agent/offline')
}

// ========== 工具映射 ==========

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
