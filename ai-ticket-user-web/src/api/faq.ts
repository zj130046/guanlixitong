import { http } from './client'

export interface FaqCategory {
  id: number
  name: string
  parentId?: number
  sortOrder: number
}

export interface FaqEntry {
  id: number
  categoryId?: number
  question: string
  answer: string
  keywords?: string
  enabled: number
  createdAt: string
  updatedAt: string
}

export function listFaqCategories() {
  return http.get<FaqCategory[], FaqCategory[]>('/faq/categories')
}

// 别名，保持兼容
export const getCategories = listFaqCategories

export function listFaqEntries(params?: { keyword?: string; categoryId?: number }) {
  return http.get<FaqEntry[], FaqEntry[]>('/faq/entries', { params })
}

// 分页版本
export function getFaqEntries(params?: { page?: number; size?: number; keyword?: string; categoryId?: number }) {
  return http.get<{ records: FaqEntry[]; total: number }, { records: FaqEntry[]; total: number }>('/faq/entries', { params })
}
