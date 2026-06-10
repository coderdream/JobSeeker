export const ACCESS_TOKEN_KEY = "jobs_access_token"
export const USER_KEY = "jobs_user"

export type AuthUser = {
  id: number
  username: string
  nickname?: string
  role?: string
  email?: string | null
}

export type StoredSession = {
  token: string
  user: AuthUser
}

function hasWindow() {
  return typeof window !== "undefined"
}

export function loadSession(): StoredSession | null {
  if (!hasWindow()) return null

  const token = window.localStorage.getItem(ACCESS_TOKEN_KEY)
  const rawUser = window.localStorage.getItem(USER_KEY)
  if (!token || !rawUser) return null

  try {
    return {
      token,
      user: JSON.parse(rawUser) as AuthUser,
    }
  } catch {
    clearSession()
    return null
  }
}

export function saveSession(session: StoredSession) {
  if (!hasWindow()) return
  window.localStorage.setItem(ACCESS_TOKEN_KEY, session.token)
  window.localStorage.setItem(USER_KEY, JSON.stringify(session.user))
}

export function clearSession() {
  if (!hasWindow()) return
  window.localStorage.removeItem(ACCESS_TOKEN_KEY)
  window.localStorage.removeItem(USER_KEY)
}
