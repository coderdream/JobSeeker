import type { ApiRequestOptions } from "./api-client"

export type PlatformKey = "boss" | "51job" | "liepin" | "zhilian" | "yupao"

export type PlatformStatusResponse = {
  success?: boolean
  platform?: string
  isRunning?: boolean
  isLoggedIn?: boolean
  message?: string
  timestamp?: number
}

export type PlatformActionResponse = {
  success?: boolean
  status?: string
  message?: string
  [key: string]: unknown
}

export type AuthedFetch = (path: string, options?: ApiRequestOptions) => Promise<Response>

export type PlatformStatusOptions = {
  refreshLogin?: boolean
}

export class PlatformActionError extends Error {
  payload: PlatformActionResponse
  responseStatus: number

  constructor(message: string, payload: PlatformActionResponse, responseStatus: number) {
    super(message)
    this.name = "PlatformActionError"
    this.payload = payload
    this.responseStatus = responseStatus
  }
}

export function isPlatformAlreadyRunningError(error: unknown) {
  return error instanceof PlatformActionError && error.payload.status === "running"
}

const PLATFORM_LABELS: Record<PlatformKey, string> = {
  boss: "Boss",
  "51job": "51job",
  liepin: "猎聘",
  zhilian: "智联招聘",
  yupao: "鱼泡直聘",
}

const STATUS_PATHS: Record<PlatformKey, string> = {
  boss: "/api/boss/status",
  "51job": "/api/51job/status",
  liepin: "/api/liepin/status",
  zhilian: "/api/zhilian/status",
  yupao: "/api/yupao/status",
}

const START_PATHS: Record<PlatformKey, string> = {
  boss: "/api/boss/start",
  "51job": "/api/51job/start",
  liepin: "/api/liepin/start",
  zhilian: "/api/zhilian/start",
  yupao: "/api/yupao/start",
}

const LOGIN_PATHS: Partial<Record<PlatformKey, string>> = {
  boss: "/api/boss/login",
  "51job": "/api/51job/login",
  liepin: "/api/liepin/login",
  zhilian: "/api/zhilian/login",
  yupao: "/api/yupao/login",
}

async function readJson<T>(response: Response): Promise<T> {
  return response.json().catch(() => ({})) as Promise<T>
}

async function readFailureMessage(response: Response, fallback: string) {
  const payload = await readJson<{ message?: string }>(response)
  return payload.message || fallback
}

export async function getPlatformStatus(
  authedFetch: AuthedFetch,
  platform: PlatformKey,
  options: PlatformStatusOptions = {},
) {
  const path = options.refreshLogin ? `${STATUS_PATHS[platform]}?refreshLogin=true` : STATUS_PATHS[platform]
  const response = await authedFetch(path, { method: "GET" })
  if (!response.ok) {
    throw new Error(await readFailureMessage(response, `${PLATFORM_LABELS[platform]} 状态检查失败`))
  }
  return readJson<PlatformStatusResponse>(response)
}

export async function ensurePlatformLoggedIn(authedFetch: AuthedFetch, platform: PlatformKey) {
  const status = await getPlatformStatus(authedFetch, platform)
  if (!status.isLoggedIn) {
    throw new Error(`${PLATFORM_LABELS[platform]} 未登录，请先完成平台登录`)
  }
  return status
}

export async function startPlatformTask(authedFetch: AuthedFetch, platform: PlatformKey) {
  await ensurePlatformLoggedIn(authedFetch, platform)

  const response = await authedFetch(START_PATHS[platform], { method: "POST" })
  const payload = await readJson<PlatformActionResponse>(response)

  if (!response.ok || payload.success === false) {
    throw new PlatformActionError(
      payload.message || `${PLATFORM_LABELS[platform]} 启动任务失败`,
      payload,
      response.status,
    )
  }

  return payload
}

export async function savePlatformCookie(authedFetch: AuthedFetch, platform: PlatformKey) {
  const response = await authedFetch(`/api/cookie/save?platform=${encodeURIComponent(platform)}`, { method: "POST" })
  const payload = await readJson<PlatformActionResponse>(response)

  if (!response.ok || payload.success === false) {
    throw new Error(payload.message || `${PLATFORM_LABELS[platform]} Cookie 保存失败`)
  }

  return payload
}

export async function openPlatformLogin(authedFetch: AuthedFetch, platform: PlatformKey) {
  const path = LOGIN_PATHS[platform]
  if (!path) {
    throw new Error(`${PLATFORM_LABELS[platform]} 后端暂未提供登录入口`)
  }

  const response = await authedFetch(path, { method: "POST" })
  const payload = await readJson<PlatformActionResponse>(response)

  if (!response.ok || payload.success === false) {
    throw new Error(payload.message || `${PLATFORM_LABELS[platform]} 登录入口打开失败`)
  }

  return payload
}
