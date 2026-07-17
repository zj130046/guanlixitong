import axios from 'axios'
import { ElMessage } from 'element-plus'

export const TOKEN_KEY = 'AI_TICKET_ADMIN_TOKEN'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = token
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      const method = response.config.method?.toUpperCase()
      if (method !== 'GET') {
        ElMessage.error(body.message || '请求失败')
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body?.data ?? body
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络异常'
    if (error.response?.status === 401 || error.response?.status === 403) {
      localStorage.removeItem(TOKEN_KEY)
    }
    const method = error.config?.method?.toUpperCase()
    if (method !== 'GET') {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)
