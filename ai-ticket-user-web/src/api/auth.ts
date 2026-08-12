import { http } from './client'
import type { LoginRequest, LoginResponse, RegisterRequest, ResetPasswordRequest } from '../types/auth'

/** 用户登录 */
export function login(data: LoginRequest) {
  return http.post<LoginResponse, LoginResponse>('/user/auth/login', data)
}

/** 用户注册 */
export function register(data: RegisterRequest) {
  return http.post<LoginResponse, LoginResponse>('/user/auth/register', data)
}

/** 密码重置 */
export function resetPassword(data: ResetPasswordRequest) {
  return http.post('/user/auth/reset-password', data)
}

/** 通用登出 */
export function logout() {
  return http.post('/auth/logout')
}
