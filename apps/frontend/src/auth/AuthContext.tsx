import { useState, type ReactNode } from 'react'
import { api } from '../lib/api'
import { clearToken, getToken, setToken } from '../lib/token'
import { AuthContext } from './context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(() => getToken() !== null)

  async function login(username: string, password: string) {
    const response = await api.post<{ token: string }>('/auth/login', { username, password })
    setToken(response.data.token)
    setIsAuthenticated(true)
  }

  function logout() {
    clearToken()
    setIsAuthenticated(false)
  }

  return (
    <AuthContext.Provider value={{ isAuthenticated, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
