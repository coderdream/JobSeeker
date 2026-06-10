export type ApiRequestOptions = RequestInit & {
  token?: string | null
}

export type ApiError = Error & {
  status?: number
}

function isApiAuthStatus(status?: number) {
  return status === 401 || status === 403
}

export function isApiAuthError(error: unknown) {
  const status = (error as ApiError).status
  return isApiAuthStatus(status)
}

function createApiError(message: string, status?: number) {
  const error = new Error(message) as ApiError
  error.status = status
  return error
}

async function readErrorMessage(response: Response) {
  try {
    const payload = (await response.clone().json()) as { message?: string }
    return payload.message || `请求失败：${response.status}`
  } catch {
    return `请求失败：${response.status}`
  }
}

export function getApiBaseUrl() {
  return process.env.NEXT_PUBLIC_API_BASE_URL || process.env.API_BASE_URL || "http://localhost:8889"
}

export async function apiFetch(path: string, options: ApiRequestOptions = {}) {
  const headers = new Headers(options.headers || {})

  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json")
  }

  if (options.token) {
    headers.set("Authorization", `Bearer ${options.token}`)
  }

  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    ...options,
    headers,
  })

  if (isApiAuthStatus(response.status)) {
    throw createApiError("UNAUTHORIZED", response.status)
  }

  return response
}

export async function apiFetchJson<T>(path: string, options: ApiRequestOptions = {}) {
  const response = await apiFetch(path, options)
  if (!response.ok) {
    throw createApiError(await readErrorMessage(response), response.status)
  }
  return response.json() as Promise<T>
}
