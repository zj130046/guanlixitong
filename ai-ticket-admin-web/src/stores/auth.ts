import { defineStore } from 'pinia'
import { login as requestLogin, logout as requestLogout } from '../api/auth'
import { TOKEN_KEY } from '../api/client'
import type { LoginRequest, LoginResponse } from '../types/auth'

interface AuthState {
  token: string
  username: string
  identityType: LoginResponse['identityType'] | ''
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    username: localStorage.getItem(`${TOKEN_KEY}_USERNAME`) || '',
    identityType: (localStorage.getItem(`${TOKEN_KEY}_IDENTITY`) as AuthState['identityType']) || ''
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    async login(form: LoginRequest) {
      const response = await requestLogin(form)
      this.token = response.token
      this.username = response.username
      this.identityType = response.identityType
      localStorage.setItem(TOKEN_KEY, response.token)
      localStorage.setItem(`${TOKEN_KEY}_USERNAME`, response.username)
      localStorage.setItem(`${TOKEN_KEY}_IDENTITY`, response.identityType)
    },
    async logout() {
      try {
        await requestLogout()
      } finally {
        this.token = ''
        this.username = ''
        this.identityType = ''
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(`${TOKEN_KEY}_USERNAME`)
        localStorage.removeItem(`${TOKEN_KEY}_IDENTITY`)
      }
    }
  }
})
