import { http, TOKEN_KEY } from './client'

export interface Ticket {
  id: number
  title: string
  description: string
  category?: string
  department?: string
  priority: string
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

// ========== 工单管理 ==========

export function adminTickets(params: {
  page?: number
  size?: number
  status?: string
  category?: string
  priority?: string
  keyword?: string
  agentId?: number
}) {
  return http.get<PageResult<Ticket>, PageResult<Ticket>>('/admin/tickets', { params })
}

export function getAdminTicket(id: number | string) {
  return http.get<Ticket, Ticket>(`/admin/tickets/${id}`)
}

export function updateAdminTicket(id: number, data: Record<string, any>) {
  return http.put<Ticket, Ticket>(`/admin/tickets/${id}`, data)
}

/** 导出工单 CSV，返回 Blob 用于下载 */
export async function exportTickets(params?: {
  status?: string
  category?: string
  priority?: string
  keyword?: string
  agentId?: number
}) {
  const token = localStorage.getItem(TOKEN_KEY)
  const searchParams = new URLSearchParams()
  if (params?.status) searchParams.set('status', params.status)
  if (params?.category) searchParams.set('category', params.category)
  if (params?.priority) searchParams.set('priority', params.priority)
  if (params?.keyword) searchParams.set('keyword', params.keyword)
  if (params?.agentId) searchParams.set('agentId', String(params.agentId))
  const url = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'}/admin/tickets/export?${searchParams}`
  const res = await fetch(url, {
    headers: { Authorization: token || '' }
  })
  if (!res.ok) throw new Error('导出失败')
  const blob = await res.blob()
  const disposition = res.headers.get('content-disposition') || ''
  const match = disposition.match(/filename\*?=(?:UTF-8'')?([^;]+)/)
  const filename = match ? decodeURIComponent(match[1]) : `tickets-${Date.now()}.csv`
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = filename
  a.click()
  URL.revokeObjectURL(a.href)
}

// ========== 统计数据 ==========

export function adminOverview() {
  return http.get<Record<string, any>, Record<string, any>>('/admin/tickets/overview')
}

export function ticketTrend(days = 7) {
  return http.get<any[], any[]>('/admin/tickets/trend', { params: { days } })
}

export function ticketStatusDistribution() {
  return http.get<any[], any[]>('/admin/tickets/status-distribution')
}

export function ticketCategoryDistribution() {
  return http.get<any[], any[]>('/admin/tickets/category-distribution')
}

// ========== 全局统计 ==========

export function statisticsOverview() {
  return http.get<Record<string, any>, Record<string, any>>('/statistics/overview')
}

export function consultationTrend(days = 7) {
  return http.get<any[], any[]>('/statistics/consultation-trend', { params: { days } })
}

export function ticketTrendStats(days = 7) {
  return http.get<any[], any[]>('/statistics/ticket-trend', { params: { days } })
}

export function ticketStatusStats() {
  return http.get<any[], any[]>('/statistics/ticket-status-distribution')
}

export function ticketCategoryStats() {
  return http.get<any[], any[]>('/statistics/ticket-category-distribution')
}

export function agentRanking(limit = 10) {
  return http.get<any[], any[]>('/statistics/agent-ranking', { params: { limit } })
}

export function satisfactionStats() {
  return http.get<Record<string, any>, Record<string, any>>('/statistics/satisfaction-stats')
}

// ========== 用户管理 ==========

export function listUsers(params?: { page?: number; size?: number; keyword?: string; status?: string }) {
  return http.get<PageResult<any>, PageResult<any>>('/admin/users', { params })
}

export function createUser(data: Record<string, any>) {
  return http.post<any, any>('/admin/users', data)
}

export function updateUser(id: number, data: Record<string, any>) {
  return http.put<any, any>(`/admin/users/${id}`, data)
}

export function updateUserStatus(id: number, status: string) {
  return http.post<any, any>(`/admin/users/${id}/status`, { status })
}

// ========== 坐席管理 ==========

export function listAgents(params?: { page?: number; size?: number; keyword?: string; groupId?: number; status?: string }) {
  return http.get<PageResult<any>, PageResult<any>>('/agent/admin/agents', { params })
}

export function createAgent(data: Record<string, any>) {
  return http.post<any, any>('/agent/admin/agents', data)
}

export function updateAgent(id: number, data: Record<string, any>) {
  return http.put<any, any>(`/agent/admin/agents/${id}`, data)
}

export function listAgentGroups() {
  return http.get<any[], any[]>('/agent/admin/groups')
}

export function createAgentGroup(data: Record<string, any>) {
  return http.post<any, any>('/agent/admin/groups', data)
}

export function updateAgentGroup(id: number, data: Record<string, any>) {
  return http.put<any, any>(`/agent/admin/groups/${id}`, data)
}

// ========== FAQ 管理 ==========

export function listFaqCategories() {
  return http.get<any[], any[]>('/admin/faq/categories')
}

export function createFaqCategory(data: Record<string, any>) {
  return http.post<any, any>('/admin/faq/categories', data)
}

export function updateFaqCategory(id: number, data: Record<string, any>) {
  return http.put<any, any>(`/admin/faq/categories/${id}`, data)
}

export function deleteFaqCategory(id: number) {
  return http.delete(`/admin/faq/categories/${id}`)
}

export function listFaqEntries(params?: { page?: number; size?: number; keyword?: string; categoryId?: number }) {
  return http.get<PageResult<any>, PageResult<any>>('/admin/faq/entries', { params })
}

export function getFaqEntry(id: number) {
  return http.get<any, any>(`/admin/faq/entries/${id}`)
}

export function createFaqEntry(data: Record<string, any>) {
  return http.post<any, any>('/admin/faq/entries', data)
}

export function updateFaqEntry(id: number, data: Record<string, any>) {
  return http.put<any, any>(`/admin/faq/entries/${id}`, data)
}

export function deleteFaqEntry(id: number) {
  return http.delete(`/admin/faq/entries/${id}`)
}

// ========== 月度报表 ==========

export function listMonthlyReports() {
  return http.get<any[], any[]>('/statistics/monthly-reports')
}

export function generateMonthlyReport() {
  return http.post<any, any>('/statistics/monthly-reports/generate')
}

export function rebuildFaqVectors() {
  return http.post<any, any>('/admin/faq/vectors/rebuild')
}

// ========== FAQ 标签 ==========

export function listFaqTags() {
  return http.get<any[], any[]>('/admin/faq/tags')
}

export function createFaqTag(data: Record<string, any>) {
  return http.post<any, any>('/admin/faq/tags', data)
}

export function deleteFaqTag(id: number) {
  return http.delete(`/admin/faq/tags/${id}`)
}

// ========== FAQ 导入导出 ==========

export function importFaqEntries(csv: string) {
  return http.post<any, any>('/admin/faq/entries/import', { csv })
}

export function exportFaqEntries() {
  return http.get('/admin/faq/entries/export', { responseType: 'blob' })
}

// ========== FAQ 配置 ==========

export function getFaqConfig() {
  return http.get<Record<string, any>, Record<string, any>>('/admin/faq/config')
}

export function testFaqMatch(question: string) {
  return http.get<any, any>('/admin/faq/match-test', { params: { question } })
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
