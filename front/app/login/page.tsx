"use client"

import { FormEvent, Suspense, useEffect, useMemo, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { BiCheckCircle, BiHide, BiShow, BiUser, BiUserPlus } from "react-icons/bi"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { apiFetch, apiFetchJson } from "@/lib/api-client"
import { AuthUser } from "@/lib/auth-storage"
import { useAuth } from "@/components/auth/AuthProvider"

type AuthMode = "login" | "register"

type LoginResponse = {
  success?: boolean
  message?: string
  data?: {
    id?: number
    username?: string
    accessToken?: string
    token?: string
    user?: AuthUser
  }
  id?: number
  username?: string
  accessToken?: string
  token?: string
  user?: AuthUser
}

type RegisterResponse = {
  success?: boolean
  message?: string
  id?: number
  username?: string
  nickname?: string
}

type LoginFormState = {
  username: string
  password: string
}

type RegisterFormState = {
  username: string
  nickname: string
  password: string
  confirmPassword: string
}

function normalizeLoginPayload(payload: LoginResponse) {
  const token = payload.data?.accessToken || payload.data?.token || payload.accessToken || payload.token
  const user =
    payload.data?.user ||
    payload.user ||
    (payload.data?.id && payload.data?.username
      ? {
          id: payload.data.id,
          username: payload.data.username,
        }
      : payload.id && payload.username
        ? {
            id: payload.id,
            username: payload.username,
          }
        : null)
  if (!token || !user) return null
  return { token, user }
}

function validateRegisterForm(form: RegisterFormState) {
  if (!/^[A-Za-z0-9_]{4,32}$/.test(form.username)) {
    return "用户名只能包含字母、数字和下划线，长度为 4-32 位"
  }
  if (form.nickname.trim().length < 2 || form.nickname.trim().length > 32) {
    return "昵称长度必须为 2-32 位"
  }
  if (!/^(?=.*[A-Za-z])(?=.*\d).{8,64}$/.test(form.password)) {
    return "密码必须为 8-64 位，且同时包含字母和数字"
  }
  if (form.password !== form.confirmPassword) {
    return "两次输入的密码不一致"
  }
  return null
}

async function parseErrorMessage(response: Response) {
  try {
    const payload = (await response.json()) as { message?: string }
    return payload.message || "请求失败，请稍后重试"
  } catch {
    return "请求失败，请稍后重试"
  }
}

function LoginForm() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const { login, ready, isAuthenticated } = useAuth()
  const redirectTarget = useMemo(() => searchParams.get("redirect") || "/env-config", [searchParams])
  const [mode, setMode] = useState<AuthMode>("login")
  const [loginForm, setLoginForm] = useState<LoginFormState>({ username: "codex", password: "Codex12345" })
  const [registerForm, setRegisterForm] = useState<RegisterFormState>({
    username: "",
    nickname: "",
    password: "",
    confirmPassword: "",
  })
  const [loginSubmitting, setLoginSubmitting] = useState(false)
  const [registerSubmitting, setRegisterSubmitting] = useState(false)
  const [loginError, setLoginError] = useState("")
  const [registerError, setRegisterError] = useState("")
  const [successMessage, setSuccessMessage] = useState("")
  const [showLoginPassword, setShowLoginPassword] = useState(false)
  const [showRegisterPassword, setShowRegisterPassword] = useState(false)
  const [showRegisterConfirmPassword, setShowRegisterConfirmPassword] = useState(false)

  useEffect(() => {
    if (ready && isAuthenticated) {
      router.replace("/env-config")
    }
  }, [isAuthenticated, ready, router])

  const handleLoginSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLoginSubmitting(true)
    setLoginError("")
    setSuccessMessage("")

    try {
      const result = await apiFetchJson<LoginResponse>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify(loginForm),
      })
      const session = normalizeLoginPayload(result)
      if (!session) {
        throw new Error(result.message || "登录返回数据不完整")
      }
      login(session)
      router.replace(redirectTarget)
    } catch (error) {
      const message = error instanceof Error ? error.message : "登录失败，请稍后重试"
      setLoginError(message === "UNAUTHORIZED" ? "账号或密码错误" : message)
    } finally {
      setLoginSubmitting(false)
    }
  }

  const handleRegisterSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setRegisterSubmitting(true)
    setRegisterError("")
    setSuccessMessage("")

    const validationMessage = validateRegisterForm(registerForm)
    if (validationMessage) {
      setRegisterError(validationMessage)
      setRegisterSubmitting(false)
      return
    }

    try {
      const response = await apiFetch("/api/auth/register", {
        method: "POST",
        body: JSON.stringify({
          username: registerForm.username.trim(),
          nickname: registerForm.nickname.trim(),
          password: registerForm.password,
          confirmPassword: registerForm.confirmPassword,
        }),
      })

      if (!response.ok) {
        throw new Error(await parseErrorMessage(response))
      }

      const payload = (await response.json()) as RegisterResponse
      setMode("login")
      setLoginForm((prev) => ({
        ...prev,
        username: payload.username || registerForm.username.trim(),
        password: "",
      }))
      setRegisterForm({
        username: registerForm.username.trim(),
        nickname: registerForm.nickname.trim(),
        password: "",
        confirmPassword: "",
      })
      setSuccessMessage(payload.message || "注册成功，请使用新账号登录")
    } catch (error) {
      setRegisterError(error instanceof Error ? error.message : "注册失败，请稍后重试")
    } finally {
      setRegisterSubmitting(false)
    }
  }

  return (
    <div className="grid min-h-screen bg-background lg:grid-cols-[minmax(360px,0.92fr)_minmax(420px,1fr)]">
      <section className="hidden min-h-screen flex-col justify-between border-r border-border bg-card p-10 lg:flex">
        <div>
          <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-primary text-sm font-semibold text-primary-foreground">
            GJ
          </div>
          <div className="mt-8 max-w-md space-y-4">
            <p className="text-sm font-medium text-primary">招聘自动化工作台</p>
            <h1 className="text-3xl font-semibold tracking-normal text-foreground">
              管理平台登录、投递任务和岗位分析。
            </h1>
            <p className="text-sm leading-6 text-muted-foreground">
              登录后进入你的专属工作区，所有平台配置、Cookie、任务状态和分析数据都会按当前账号隔离。
            </p>
          </div>
        </div>
        <div className="grid gap-3 text-sm text-muted-foreground">
          <div className="rounded-lg border border-border bg-background p-4">状态先行：先确认登录，再启动任务。</div>
          <div className="rounded-lg border border-border bg-background p-4">数据可追踪：投递结果进入平台分析页。</div>
        </div>
      </section>
      <main className="flex min-h-screen items-center justify-center bg-muted/40 px-5 py-8 sm:px-8">
      <Card className="w-full max-w-lg">
        <CardHeader className="space-y-4">
          <div className="inline-flex h-11 w-11 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            {mode === "login" ? <BiUser className="text-2xl" /> : <BiUserPlus className="text-2xl" />}
          </div>
          <div className="space-y-2">
            <CardTitle className="text-2xl font-semibold">Get Jobs 账号中心</CardTitle>
            <CardDescription>
              {mode === "login"
                ? "登录后进入你的专属招聘自动化工作台。"
                : "创建一个新账号，注册成功后返回登录页手动登录。"}
            </CardDescription>
          </div>
          <Tabs className="w-full" value={mode} onValueChange={(value) => {
            setMode(value as AuthMode)
            setLoginError("")
            setRegisterError("")
          }}>
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="login">登录</TabsTrigger>
              <TabsTrigger value="register">注册</TabsTrigger>
            </TabsList>
            <TabsContent className="hidden" value="login" />
            <TabsContent className="hidden" value="register" />
          </Tabs>
        </CardHeader>
        <CardContent>
          {successMessage ? (
            <div className="mb-5 flex items-start gap-3 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
              <BiCheckCircle className="mt-0.5 shrink-0 text-lg" />
              <div>{successMessage || "注册成功，请使用新账号登录"}</div>
            </div>
          ) : null}

          {mode === "login" ? (
            <form className="space-y-5" onSubmit={handleLoginSubmit}>
              <div className="space-y-2">
                <Label htmlFor="login-username">账号</Label>
                <Input
                  id="login-username"
                  autoComplete="username"
                  value={loginForm.username}
                  onChange={(event) => setLoginForm((prev) => ({ ...prev, username: event.target.value }))}
                  placeholder="请输入用户名"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="login-password">密码</Label>
                <div className="relative">
                  <Input
                    id="login-password"
                    type={showLoginPassword ? "text" : "password"}
                    autoComplete="current-password"
                    value={loginForm.password}
                    onChange={(event) => setLoginForm((prev) => ({ ...prev, password: event.target.value }))}
                    placeholder="请输入密码"
                  />
                  <button
                    type="button"
                    aria-label={showLoginPassword ? "隐藏密码" : "显示密码"}
                    onClick={() => setShowLoginPassword((value) => !value)}
                    className="absolute right-2 top-1/2 inline-flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                  >
                    {showLoginPassword ? <BiHide /> : <BiShow />}
                  </button>
                </div>
              </div>

              {loginError ? <p className="text-sm text-red-500">{loginError}</p> : null}

              <Button className="w-full" disabled={loginSubmitting} type="submit">
                {loginSubmitting ? "登录中..." : "登录"}
              </Button>
            </form>
          ) : (
            <form className="space-y-5" onSubmit={handleRegisterSubmit}>
              <div className="grid gap-5 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="register-username">用户名</Label>
                  <Input
                    id="register-username"
                    autoComplete="username"
                    value={registerForm.username}
                    onChange={(event) => setRegisterForm((prev) => ({ ...prev, username: event.target.value }))}
                    placeholder="4-32 位字母、数字或下划线"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="register-nickname">昵称</Label>
                  <Input
                    id="register-nickname"
                    autoComplete="nickname"
                    value={registerForm.nickname}
                    onChange={(event) => setRegisterForm((prev) => ({ ...prev, nickname: event.target.value }))}
                    placeholder="请输入昵称"
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="register-password">密码</Label>
                <div className="relative">
                  <Input
                    id="register-password"
                    type={showRegisterPassword ? "text" : "password"}
                    autoComplete="new-password"
                    value={registerForm.password}
                    onChange={(event) => setRegisterForm((prev) => ({ ...prev, password: event.target.value }))}
                    placeholder="8-64 位，需包含字母和数字"
                  />
                  <button
                    type="button"
                    aria-label={showRegisterPassword ? "隐藏密码" : "显示密码"}
                    onClick={() => setShowRegisterPassword((value) => !value)}
                    className="absolute right-2 top-1/2 inline-flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                  >
                    {showRegisterPassword ? <BiHide /> : <BiShow />}
                  </button>
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="register-confirm-password">确认密码</Label>
                <div className="relative">
                  <Input
                    id="register-confirm-password"
                    type={showRegisterConfirmPassword ? "text" : "password"}
                    autoComplete="new-password"
                    value={registerForm.confirmPassword}
                    onChange={(event) => setRegisterForm((prev) => ({ ...prev, confirmPassword: event.target.value }))}
                    placeholder="请再次输入密码"
                  />
                  <button
                    type="button"
                    aria-label={showRegisterConfirmPassword ? "隐藏确认密码" : "显示确认密码"}
                    onClick={() => setShowRegisterConfirmPassword((value) => !value)}
                    className="absolute right-2 top-1/2 inline-flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                  >
                    {showRegisterConfirmPassword ? <BiHide /> : <BiShow />}
                  </button>
                </div>
              </div>

              {registerError ? <p className="text-sm text-red-500">{registerError}</p> : null}

              <Button className="w-full" disabled={registerSubmitting} type="submit">
                {registerSubmitting ? "注册中..." : "创建账号"}
              </Button>
            </form>
          )}
        </CardContent>
      </Card>
      </main>
    </div>
  )
}

export default function LoginPage() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-muted/50" />}>
      <LoginForm />
    </Suspense>
  )
}
