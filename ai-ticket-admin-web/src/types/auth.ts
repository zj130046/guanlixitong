export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  username: string
  identityType: 'USER' | 'AGENT' | 'ADMIN'
}
