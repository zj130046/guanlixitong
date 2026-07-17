import { http } from './client'
import type { LoginRequest, LoginResponse } from '../types/auth'

export function login(data: LoginRequest) {
  return http.post<LoginResponse, LoginResponse>('/agent/auth/login', data)
}

export function logout() {
  return http.post('/auth/logout')
}
