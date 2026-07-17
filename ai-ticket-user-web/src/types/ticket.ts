export type TicketPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'
export type TicketStatus = 'CREATED' | 'ASSIGNED' | 'ACCEPTED' | 'PROCESSING' | 'FOLLOWING' | 'COMPLETED' | 'REJECTED' | 'ARCHIVED'
export type TicketSource = 'AI_CHAT' | 'USER_FORM' | 'MANUAL' | 'IMPORT'

export interface TicketEventRecord {
  id: number
  eventType: string
  fromStatus?: string
  toStatus?: string
  operatorType?: string
  operatorId?: number
  remark?: string
  createdAt: string
}

export interface TicketRecord {
  id: number
  title: string
  description: string
  category?: string
  department?: string
  priority: TicketPriority
  status: TicketStatus
  source?: TicketSource
  assignee?: string
  userId?: number
  assigneeAgentId?: number
  conversationId?: number
  requesterName?: string
  timeoutAt?: string
  completedAt?: string
  createdAt: string
  updatedAt: string
  events?: TicketEventRecord[]
}

export interface TicketCreateRequest {
  title: string
  description: string
  category?: string
  department?: string
  priority: TicketPriority
}
