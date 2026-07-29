import React, { createContext, useContext, useState, useCallback } from 'react'
import api from '../lib/api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('verichain_user')
    return stored ? JSON.parse(stored) : null
  })

  const persistSession = useCallback((authResponse) => {
    const sessionUser = {
      userId: authResponse.userId,
      name: authResponse.name,
      email: authResponse.email,
      role: authResponse.role,
    }
    localStorage.setItem('verichain_token', authResponse.token)
    localStorage.setItem('verichain_user', JSON.stringify(sessionUser))
    setUser(sessionUser)
    return sessionUser
  }, [])

  const login = useCallback(async (email, password) => {
    const { data } = await api.post('/api/auth/login', { email, password })
    return persistSession(data)
  }, [persistSession])

  const registerStudent = useCallback(async (payload) => {
    const { data } = await api.post('/api/auth/register/student', payload)
    return persistSession(data)
  }, [persistSession])

  const registerIssuer = useCallback(async (payload) => {
    const { data } = await api.post('/api/auth/register/issuer', payload)
    return persistSession(data)
  }, [persistSession])

  const logout = useCallback(() => {
    localStorage.removeItem('verichain_token')
    localStorage.removeItem('verichain_user')
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, login, registerStudent, registerIssuer, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
