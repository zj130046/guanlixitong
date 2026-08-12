export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  username: string
  identityType: 'USER' | 'AGENT' | 'ADMIN'
}

export interface RegisterRequest {
  username: string
  password: string
  phone?: string
  email?: string
}

export interface ResetPasswordRequest {
  username: string
  oldPassword: string
  newPassword: string
}
