import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import {
  getAccessTokenExpiry,
  getCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
  refreshAccessToken,
  register as registerRequest,
} from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [expiresAt, setExpiresAt] = useState(getAccessTokenExpiry())

  useEffect(() => {
    const onTokenUpdated = (event) => setExpiresAt(event.detail.expiresAt)
    const onSessionEnded = () => {
      setUser(null)
      setExpiresAt(0)
    }
    window.addEventListener('aija:token-updated', onTokenUpdated)
    window.addEventListener('aija:session-ended', onSessionEnded)

    getCurrentUser()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false))

    return () => {
      window.removeEventListener('aija:token-updated', onTokenUpdated)
      window.removeEventListener('aija:session-ended', onSessionEnded)
    }
  }, [])

  useEffect(() => {
    if (!user || !expiresAt) return undefined
    const renewIn = Math.max(0, expiresAt - Date.now() - 60_000)
    const timer = window.setTimeout(() => {
      refreshAccessToken().catch(() => {})
    }, renewIn)
    return () => window.clearTimeout(timer)
  }, [user, expiresAt])

  const signIn = useCallback(async (email, password) => {
    const session = await loginRequest(email, password)
    setUser(session.user)
    return session.user
  }, [])

  const signUp = useCallback(async (name, email, password) => {
    const session = await registerRequest(name, email, password)
    setUser(session.user)
    return session.user
  }, [])

  const signOut = useCallback(async () => {
    await logoutRequest()
  }, [])

  const updateUser = useCallback((nextUser) => {
    setUser((current) => current ? { ...current, ...nextUser } : nextUser)
  }, [])

  const value = useMemo(() => ({ user, loading, signIn, signUp, signOut, updateUser }),
    [user, loading, signIn, signUp, signOut, updateUser])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider.')
  return context
}
