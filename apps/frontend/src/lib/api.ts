import axios from 'axios'
import { clearToken, getToken } from './token'

export const api = axios.create({
  baseURL: '/api',
})

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // A 401 from the login endpoint itself just means "wrong credentials" —
    // that's an expected error the login form already handles. Only treat
    // a 401 from anywhere else as "the session/token is no longer valid".
    const isLoginRequest = error.config?.url === '/auth/login'
    if (error.response?.status === 401 && !isLoginRequest) {
      clearToken()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  },
)
