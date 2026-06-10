export type SSEListener = { name: string; handler: (event: MessageEvent) => void }

export type BackoffConfig = {
  initialDelayMs?: number
  maxDelayMs?: number
  factor?: number
  jitter?: boolean
}

export type SSEBackoffOptions = {
  token?: string | null
  listeners: SSEListener[]
  onOpen?: () => void
  onError?: (error: unknown, attempt: number, delayMs: number) => void
  backoff?: BackoffConfig
}

export type SSEBackoffClient = {
  close: () => void
}

type SSEEventState = {
  event: string
  data: string[]
}

type SSERequestError = Error & {
  status?: number
}

/**
 * Create an SSE client with exponential backoff using fetch so auth headers can
 * be sent with the stream request.
 */
export function createSSEWithBackoff(url: string, options: SSEBackoffOptions): SSEBackoffClient {
  const cfg: Required<BackoffConfig> = {
    initialDelayMs: options.backoff?.initialDelayMs ?? 1000,
    maxDelayMs: options.backoff?.maxDelayMs ?? 30000,
    factor: options.backoff?.factor ?? 1.7,
    jitter: options.backoff?.jitter ?? true,
  }

  let attempt = 0
  let closed = false
  let reconnectTimer: number | null = null
  let abortController: AbortController | null = null

  const computeDelay = (n: number) => {
    const base = Math.min(cfg.initialDelayMs * Math.pow(cfg.factor, Math.max(0, n - 1)), cfg.maxDelayMs)
    if (!cfg.jitter) return base
    const jitter = base * 0.3 * Math.random()
    return Math.floor(base - jitter)
  }

  const cleanupReconnect = () => {
    if (reconnectTimer != null) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  const dispatchEvent = (eventName: string, data: string) => {
    const listener = options.listeners.find((item) => item.name === eventName)
    if (!listener) return
    listener.handler(new MessageEvent(eventName, { data }))
  }

  const flushEvent = (state: SSEEventState) => {
    if (state.data.length === 0) {
      state.event = "message"
      return
    }

    dispatchEvent(state.event, state.data.join("\n"))
    state.event = "message"
    state.data = []
  }

  const processLine = (line: string, state: SSEEventState) => {
    if (line === "") {
      flushEvent(state)
      return
    }

    if (line.startsWith(":")) {
      return
    }

    const separatorIndex = line.indexOf(":")
    const field = separatorIndex === -1 ? line : line.slice(0, separatorIndex)
    let value = separatorIndex === -1 ? "" : line.slice(separatorIndex + 1)
    if (value.startsWith(" ")) {
      value = value.slice(1)
    }

    if (field === "event") {
      state.event = value || "message"
      return
    }

    if (field === "data") {
      state.data.push(value)
    }
  }

  const createRequestError = (status: number) => {
    const error = new Error(`SSE_REQUEST_FAILED_${status}`) as SSERequestError
    error.status = status
    return error
  }

  const scheduleReconnect = (error: unknown) => {
    if (closed) return

    const nextAttempt = attempt + 1
    const delay = computeDelay(nextAttempt)
    options.onError?.(error, nextAttempt, delay)
    attempt = nextAttempt
    cleanupReconnect()

    reconnectTimer = window.setTimeout(() => {
      void connect()
    }, delay)
  }

  const connect = async () => {
    if (closed) return

    abortController?.abort()
    abortController = new AbortController()

    try {
      const response = await fetch(url, {
        method: "GET",
        headers: options.token
          ? {
              Accept: "text/event-stream",
              Authorization: `Bearer ${options.token}`,
            }
          : {
              Accept: "text/event-stream",
            },
        cache: "no-store",
        signal: abortController.signal,
      })

      if (!response.ok) {
        throw createRequestError(response.status)
      }

      if (!response.body) {
        throw new Error("SSE response body is empty")
      }

      attempt = 0
      options.onOpen?.()

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      const state: SSEEventState = { event: "message", data: [] }
      let buffer = ""

      while (!closed) {
        const { value, done } = await reader.read()
        if (done) {
          break
        }

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split(/\r?\n/)
        buffer = lines.pop() ?? ""

        for (const line of lines) {
          processLine(line, state)
        }
      }

      buffer += decoder.decode()
      if (buffer) {
        processLine(buffer, state)
      }
      flushEvent(state)

      if (!closed) {
        scheduleReconnect(new Error("SSE stream closed"))
      }
    } catch (error) {
      if ((error as Error).name === "AbortError") {
        return
      }
      scheduleReconnect(error)
    }
  }

  const handleBeforeUnload = () => {
    closed = true
    abortController?.abort()
    abortController = null
    cleanupReconnect()
  }

  if (typeof window !== "undefined") {
    window.addEventListener("beforeunload", handleBeforeUnload)
    void connect()
  }

  return {
    close() {
      if (typeof window !== "undefined") {
        window.removeEventListener("beforeunload", handleBeforeUnload)
      }
      handleBeforeUnload()
    },
  }
}
