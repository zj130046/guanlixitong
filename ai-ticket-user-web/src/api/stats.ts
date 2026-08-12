import { http } from './client'

export interface StatisticsOverview {
  todayConsultations: number
  totalConversations: number
  totalTickets: number
  aiReplyRate: number
  manualTransferRate: number
  ticketCompletionRate: number
  satisfactionScore: number
  pendingTickets: number
  processingTickets: number
  completedTickets: number
  userCount: number
  agentCount: number
  onlineAgents: number
  faqCount: number
}

export function getStatisticsOverview() {
  return http.get<StatisticsOverview, StatisticsOverview>('/statistics/overview')
}
