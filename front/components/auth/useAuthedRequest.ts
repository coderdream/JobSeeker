"use client"

import { useCallback } from "react"
import { apiFetch, apiFetchJson, ApiRequestOptions, isApiAuthError } from "@/lib/api-client"
import { loadSession } from "@/lib/auth-storage"
import { useAuth } from "./AuthProvider"

const AUTH_TOKEN_MISSING = "AUTH_TOKEN_MISSING"

function resolveRequestToken(token: string | null) {
  return token ?? loadSession()?.token ?? null
}

function createMissingTokenError() {
  return new Error(AUTH_TOKEN_MISSING)
}

export function useAuthedRequest() {
  const { token, logout } = useAuth()
  const currentToken = resolveRequestToken(token)

  const authedFetch = useCallback(
    async (path: string, options: ApiRequestOptions = {}) => {
      try {
        const requestToken = resolveRequestToken(token)
        if (!requestToken) {
          throw createMissingTokenError()
        }

        return await apiFetch(path, {
          ...options,
          token: requestToken,
        })
      } catch (error) {
        if (isApiAuthError(error)) {
          await logout()
        }
        throw error
      }
    },
    [logout, token],
  )

  const authedFetchJson = useCallback(
    async <T,>(path: string, options: ApiRequestOptions = {}) => {
      try {
        const requestToken = resolveRequestToken(token)
        if (!requestToken) {
          throw createMissingTokenError()
        }

        return await apiFetchJson<T>(path, {
          ...options,
          token: requestToken,
        })
      } catch (error) {
        if (isApiAuthError(error)) {
          await logout()
        }
        throw error
      }
    },
    [logout, token],
  )

  return {
    token: currentToken,
    authedFetch,
    authedFetchJson,
  }
}
