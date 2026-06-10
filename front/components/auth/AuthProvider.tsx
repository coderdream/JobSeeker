"use client"

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react"
import { apiFetchJson, type ApiError } from "@/lib/api-client"
import { AuthUser, clearSession, loadSession, saveSession, StoredSession } from "@/lib/auth-storage"

type MeResponse =
    | {
  success?: boolean
  data?: AuthUser
  id?: number
  username?: string
  nickname?: string
  role?: string
  email?: string | null
}
    | AuthUser

type AuthContextValue = {
  user: AuthUser | null
  token: string | null
  ready: boolean
  isAuthenticated: boolean
  login: (session: StoredSession) => void
  logout: () => Promise<void>
  refreshMe: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)
const INVALID_ME_RESPONSE = "INVALID_ME_RESPONSE"

function isAuthExpired(error: unknown) {
  const status = (error as ApiError).status
  return status === 401 || status === 403
}

function shouldClearSession(error: unknown) {
  return isAuthExpired(error) || (error instanceof Error && error.message === INVALID_ME_RESPONSE)
}

function normalizeUser(payload: MeResponse): AuthUser | null {
  const candidate = ("data" in payload && payload.data ? payload.data : payload) as Partial<AuthUser>
  if (!candidate?.id || !candidate?.username) {
    return null
  }
  return {
    id: candidate.id,
    username: candidate.username,
    nickname: candidate.nickname,
    role: candidate.role,
    email: candidate.email ?? null,
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [ready, setReady] = useState(false)
  const bootstrappedRef = useRef(false)

  const applySession = useCallback((session: StoredSession) => {
    setToken(session.token)
    setUser(session.user)
    setReady(true)
    saveSession(session)
  }, [])

  const clearAuthState = useCallback(() => {
    clearSession()
    setToken(null)
    setUser(null)
    setReady(true)
  }, [])

  const logout = useCallback(async () => {
    const currentToken = token
    clearAuthState()

    if (!currentToken) return

    try {
      await apiFetchJson("/api/auth/logout", {
        method: "POST",
        token: currentToken,
      })
    } catch {
      // Frontend logout should still complete even if backend logout is unavailable.
    }
  }, [clearAuthState, token])

  const validateToken = useCallback(async (currentToken: string) => {
    try {
      const response = await apiFetchJson<MeResponse>("/api/auth/me", { token: currentToken })
      const nextUser = normalizeUser(response)
      if (!nextUser) {
        throw new Error(INVALID_ME_RESPONSE)
      }
      applySession({ token: currentToken, user: nextUser })
    } catch (error) {
      if (shouldClearSession(error)) {
        clearAuthState()
      }
    } finally {
      setReady(true)
    }
  }, [applySession, clearAuthState])

  const refreshMe = useCallback(async () => {
    const currentToken = token ?? loadSession()?.token ?? null
    if (!currentToken) {
      clearAuthState()
      return
    }

    await validateToken(currentToken)
  }, [clearAuthState, token, validateToken])

  useEffect(() => {
    if (bootstrappedRef.current) return
    bootstrappedRef.current = true

    const timer = window.setTimeout(() => {
      const session = loadSession()
      if (!session) {
        setReady(true)
        return
      }

      setToken(session.token)
      setUser(session.user)

      void validateToken(session.token)
    }, 0)

    return () => {
      window.clearTimeout(timer)
    }
  }, [validateToken])

  const value = useMemo<AuthContextValue>(
      () => ({
        user,
        token,
        ready,
        isAuthenticated: Boolean(token && user),
        login: applySession,
        logout,
        refreshMe,
      }),
      [applySession, logout, ready, refreshMe, token, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider")
  }
  return context
}
