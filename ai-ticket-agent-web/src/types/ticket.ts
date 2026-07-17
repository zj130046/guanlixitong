export type TicketPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'
export type TicketStatus = 'CREATED' | 'ASSIGNED' | 'ACCEPTED' | 'PROCESSING' | 'FOLLOWING' | 'COMPLETED' | 'REJECTED' | 'ARCHIVED'

export interface TicketRecord {
  id: number
  title: string
  description: string
  category?: string
  department?: string
  priority: TicketPriority
  status: TicketStatus
  assignee?: string
  createdAt: string
  updatedAt: string
}

export interface TicketCreateRequest {
  title: string
  description: string
  category?: string
  department?: string
  priority: TicketPriority
}
