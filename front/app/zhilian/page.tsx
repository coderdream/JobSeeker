'use client'

import { useState, useEffect, useCallback } from 'react'
import { createSSEWithBackoff } from '@/lib/sse'
import { BiLogOut, BiSave, BiBriefcase, BiPlay, BiStop } from 'react-icons/bi'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import AnalysisContent from '@/app/zhilian/analysis/AnalysisContent'
import PageHeader from '@/app/components/PageHeader'
import { useAuthedRequest } from '@/components/auth/useAuthedRequest'
import { getApiBaseUrl } from '@/lib/api-client'
import { getPlatformStatus, isPlatformAlreadyRunningError, openPlatformLogin, savePlatformCookie, startPlatformTask } from '@/lib/platform-requests'
import { FeedbackDialog } from '@/components/workbench/feedback-dialog'
import { PlatformStatusBar } from '@/components/workbench/platform-status-bar'

interface ZhilianConfig {
  id?: number
  keywords?: string
  cityCode?: string
  salary?: string
}

interface Option { name: string; code: string }
interface ZhilianOptions { city: Option[]; salary: Option[] }

export default function ZhilianPage() {
  const { token, authedFetch } = useAuthedRequest()
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [isDelivering, setIsDelivering] = useState(false)
  const [isStartingDelivery, setIsStartingDelivery] = useState(false)
  const [checkingLogin, setCheckingLogin] = useState(true)
  const [loginPolling, setLoginPolling] = useState(false)
  const [showLogoutDialog, setShowLogoutDialog] = useState(false)
  const [showSaveDialog, setShowSaveDialog] = useState(false)
  const [saveResult, setSaveResult] = useState<{ success: boolean; message: string } | null>(null)
  const [showLogoutResultDialog, setShowLogoutResultDialog] = useState(false)
  const [logoutResult, setLogoutResult] = useState<{ success: boolean; message: string } | null>(null)
  const [backendAvailable, setBackendAvailable] = useState(true)
  const [cookieSavedAfterLogin, setCookieSavedAfterLogin] = useState(false)

  const [config, setConfig] = useState<ZhilianConfig>({ keywords: '', cityCode: '', salary: '' })
  const [options, setOptions] = useState<ZhilianOptions>({ city: [], salary: [] })
  const [loadingConfig, setLoadingConfig] = useState(true)
  const [isCustomSalary, setIsCustomSalary] = useState(false)

  useEffect(() => {
    if (!token) {
      return
    }

    if (typeof window === 'undefined' || typeof ReadableStream === 'undefined') {
      console.warn('[智联招聘] ReadableStream 不可用，无法连接SSE')
      return
    }

    const client = createSSEWithBackoff(`${getApiBaseUrl()}/api/jobs/login-status/stream`, {
      token,
      onOpen: () => console.log('[智联招聘 SSE] 连接已打开'),
      onError: (e, attempt, delay) => {
        console.warn(`[智联招聘 SSE] 连接错误，第${attempt}次重连，延迟 ${delay}ms`, e)
        setCheckingLogin(false)
      },
      listeners: [
        {
          name: 'connected',
          handler: (event) => {
            try {
              const data = JSON.parse(event.data)
              console.log('[智联招聘 SSE] connected事件数据:', data)
              console.log('[智联招聘 SSE] zhilianLoggedIn状态:', data.zhilianLoggedIn)
              const loggedIn = Boolean(data.zhilianLoggedIn)
              setIsLoggedIn((current) => current || loggedIn)
              if (loggedIn && !cookieSavedAfterLogin) {
                savePlatformCookie(authedFetch, 'zhilian')
                  .then(() => setCookieSavedAfterLogin(true))
                  .catch((error) => console.warn('[Zhilian] save cookie after login failed:', error))
              }
              if (loggedIn) {
                setCheckingLogin(false)
                setLoginPolling(false)
              } else if (!loginPolling) {
                setCheckingLogin(false)
              }
            } catch (error) {
              console.error('[智联招聘 SSE] 解析连接消息失败:', error)
            }
          },
        },
        {
          name: 'login-status',
          handler: (event) => {
            try {
              const data = JSON.parse(event.data)
              console.log('[智联招聘 SSE] login-status事件数据:', data)
              if (data.platform === 'zhilian') {
                console.log('[智联招聘 SSE] 智联登录状态变更:', data.isLoggedIn)
                const loggedIn = Boolean(data.isLoggedIn)
                setIsLoggedIn(loggedIn)
                if (loggedIn && !cookieSavedAfterLogin) {
                  savePlatformCookie(authedFetch, 'zhilian')
                    .then(() => setCookieSavedAfterLogin(true))
                    .catch((error) => console.warn('[Zhilian] save cookie after login failed:', error))
                }
                if (loggedIn) {
                  setCheckingLogin(false)
                  setLoginPolling(false)
                } else if (!loginPolling) {
                  setCheckingLogin(false)
                }
              }
            } catch (error) {
              console.error('[智联招聘 SSE] 解析登录状态消息失败:', error)
            }
          },
        },
        { name: 'ping', handler: () => {} },
      ],
    })

    return () => client.close()
  }, [authedFetch, cookieSavedAfterLogin, loginPolling, token])

  useEffect(() => {
    if (!loginPolling || !token) {
      return
    }

    let attempts = 0
    const pollLoginStatus = async () => {
      attempts += 1
      try {
        const status = await getPlatformStatus(authedFetch, 'zhilian', { refreshLogin: true })
        const loggedIn = Boolean(status.isLoggedIn)
        setIsLoggedIn(loggedIn)
        if (loggedIn || attempts >= 150) {
          setCheckingLogin(false)
          setLoginPolling(false)
        }
      } catch (error) {
        console.warn('[Zhilian login polling] failed:', error)
        if (attempts >= 150) {
          setCheckingLogin(false)
          setLoginPolling(false)
        }
      }
    }

    void pollLoginStatus()
    const interval = window.setInterval(() => {
      void pollLoginStatus()
    }, 2000)

    return () => window.clearInterval(interval)
  }, [authedFetch, loginPolling, token])

  const refreshDeliveryStatus = useCallback(async () => {
    const status = await getPlatformStatus(authedFetch, 'zhilian')
    setIsLoggedIn(Boolean(status.isLoggedIn))
    setIsDelivering(Boolean(status.isRunning))
    return status
  }, [authedFetch])

  useEffect(() => {
    if (!token) {
      return
    }

    let cancelled = false
    const refreshLoginState = async () => {
      if (!loginPolling) {
        setCheckingLogin(true)
      }
      try {
        const status = await refreshDeliveryStatus()
        if (Boolean(status.isLoggedIn)) {
          setLoginPolling(false)
        }
      } catch (error) {
        console.warn('[Zhilian status refresh] failed:', error)
      } finally {
        if (!cancelled && !loginPolling) {
          setCheckingLogin(false)
        }
      }
    }

    void refreshLoginState()
    const handleFocus = () => {
      void refreshLoginState()
    }
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        void refreshLoginState()
      }
    }

    window.addEventListener('focus', handleFocus)
    document.addEventListener('visibilitychange', handleVisibilityChange)

    return () => {
      cancelled = true
      window.removeEventListener('focus', handleFocus)
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
  }, [loginPolling, refreshDeliveryStatus, token])

  useEffect(() => {
    if (!isDelivering || !token) {
      return
    }

    const pollStatus = async () => {
      try {
        await refreshDeliveryStatus()
      } catch (error) {
        console.warn('[Zhilian status polling] failed:', error)
      }
    }

    void pollStatus()
    const interval = window.setInterval(() => {
      void pollStatus()
    }, 3000)

    return () => window.clearInterval(interval)
  }, [isDelivering, refreshDeliveryStatus, token])

  // 与猎聘一致的关键词解析/序列化
  const parseKeywordsFromDb = (raw?: string): string => {
    if (!raw) return ''
    const t = raw.trim()
    if (t.startsWith('[') && t.endsWith(']')) {
      try {
        const arr = JSON.parse(t)
        if (Array.isArray(arr)) return arr.filter(Boolean).join(', ')
      } catch (e) {
        console.warn('[智联] 解析关键词JSON失败，使用原值:', e)
      }
    }
    return t.replace(/，/g, ',')
  }

  const serializeKeywordsForDb = (display?: string): string => {
    const raw = (display || '').trim()
    if (!raw) return '[]'
    const norm = raw.replace(/，/g, ',')
    const tokens = norm
      .split(',')
      .map((s) => s.trim())
      .filter((s) => s.length > 0)
    return JSON.stringify(tokens)
  }

  const fetchAllData = useCallback(async () => {
    try {
      const res = await authedFetch('/api/zhilian/config')
      const data = await res.json()
      if (data.config) {
        const normalized = { ...data.config }
        normalized.keywords = parseKeywordsFromDb(data.config.keywords)
        setConfig(normalized)
      }
      if (data.options) setOptions({ city: data.options.city || [], salary: data.options.salary || [] })
    } catch (e) {
      console.error('[智联] 获取配置失败:', e)
    } finally {
      setLoadingConfig(false)
    }
  }, [authedFetch])

  useEffect(() => { queueMicrotask(() => void fetchAllData()) }, [fetchAllData])

  // 探测后端可用性（与 51job 保持一致风格）
  useEffect(() => {
    (async () => {
      try {
        const res = await authedFetch('/api/zhilian/config', { method: 'GET' })
        const ok = !!res && res.ok
        setBackendAvailable(ok)
        if (ok) {
          await fetchAllData()
        } else {
          setLoadingConfig(false)
        }
      } catch {
        setBackendAvailable(false)
        setLoadingConfig(false)
      }
    })()
  }, [authedFetch, fetchAllData])

  const handleOpenLogin = async () => {
    try {
      setCheckingLogin(true)
      await openPlatformLogin(authedFetch, 'zhilian')
      setLoginPolling(true)
    } catch (error) {
      console.error('[Zhilian] 打开登录入口失败:', error)
      setCheckingLogin(false)
      setLoginPolling(false)
    }
  }

  const handleStartDelivery = async () => {
    try {
      setIsStartingDelivery(true)
      setIsDelivering(true)
      await startPlatformTask(authedFetch, 'zhilian')
      await refreshDeliveryStatus()
    } catch (error) {
      if (isPlatformAlreadyRunningError(error)) {
        setIsDelivering(true)
        await refreshDeliveryStatus()
        return
      }
      console.error('[Zhilian] 启动投递失败:', error)
      setIsDelivering(false)
    } finally {
      setIsStartingDelivery(false)
    }
  }

  const handleStopDelivery = async () => {
    try {
      const response = await authedFetch('/api/zhilian/stop', { method: 'POST' })
      const data = await response.json()
      if (data.success) setIsDelivering(false)
    } catch {}
  }

  const triggerLogout = async () => {
    try {
      const response = await authedFetch('/api/zhilian/logout', { method: 'POST' })
      const data = await response.json()
      setIsLoggedIn(false)
      setLoginPolling(false)
      setLogoutResult({ success: data.success, message: data.success ? '已退出登录，Cookie已清空。' : data.message })
      setShowLogoutResultDialog(true)
    } catch {
      setLogoutResult({ success: false, message: '退出登录失败：网络或服务异常。' })
      setShowLogoutResultDialog(true)
    }
  }

  const handleSaveConfig = async () => {
    try {
      const payload = { ...config, keywords: serializeKeywordsForDb(config.keywords) }
      const response = await authedFetch('/api/zhilian/config', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
      if (response.ok) {
        try { await authedFetch('/api/cookie/save?platform=zhilian', { method: 'POST' }) } catch {}
        await fetchAllData()
        setSaveResult({ success: true, message: '保存成功，配置已更新。' })
      } else {
        setSaveResult({ success: false, message: '保存失败：后端返回异常状态。' })
      }
      setShowSaveDialog(true)
    } catch (error) {
      console.error('[智联] 保存配置失败:', error)
      setSaveResult({ success: false, message: '保存失败：网络或服务异常。' })
      setShowSaveDialog(true)
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        icon={<BiBriefcase className="text-2xl" />}
        title="智联招聘配置"
        subtitle="配置智联招聘平台的求职参数"
        iconClass="text-white"
        accentBgClass="bg-sky-500"
        actions={
          <div className="flex items-center gap-2">
            {checkingLogin ? (
              <Button size="sm" disabled variant="secondary" className="px-3">
                <BiPlay className="mr-1" /> 检查登录中...
              </Button>
            ) : !isLoggedIn ? (
              <Button onClick={handleOpenLogin} size="sm" className="px-3">
                <BiPlay className="mr-1" /> 打开智联登录
              </Button>
            ) : isStartingDelivery ? (
              <Button size="sm" disabled className="px-3">
                <BiPlay className="mr-1" /> 启动中...
              </Button>
            ) : isDelivering ? (
              <Button onClick={handleStopDelivery} size="sm" variant="destructive" className="px-3">
                <BiStop className="mr-1" /> 停止投递
              </Button>
            ) : (
              <Button onClick={handleStartDelivery} size="sm" className="px-3">
                <BiPlay className="mr-1" /> 开始投递
              </Button>
            )}
            <Button onClick={() => setShowLogoutDialog(true)} size="sm" variant="outline" className="px-3 text-destructive hover:text-destructive">
              <BiLogOut className="mr-1" /> 退出登录
            </Button>
            <Button onClick={handleSaveConfig} size="sm" className="px-3">
              <BiSave className="mr-1" /> 保存配置
            </Button>
          </div>
        }
      />

      <PlatformStatusBar
        platform="智联招聘"
        description="当前支持关键词、城市和薪资配置；更多平台筛选项保持后端接口兼容后逐步补齐。"
        items={[
          { label: '登录', value: checkingLogin ? '检查中' : isLoggedIn ? '已登录' : '未登录', tone: checkingLogin ? 'info' : isLoggedIn ? 'success' : 'warning' },
          { label: '任务', value: isDelivering ? '运行中' : isStartingDelivery ? '启动中' : '空闲', tone: isDelivering ? 'success' : isStartingDelivery ? 'info' : 'neutral' },
          { label: '后端', value: backendAvailable ? '已连接' : '未连接', tone: backendAvailable ? 'success' : 'warning' },
        ]}
      />

      <Tabs defaultValue="config" className="min-w-0">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="config">平台配置</TabsTrigger>
          <TabsTrigger value="analytics">投递分析</TabsTrigger>
        </TabsList>

        <TabsContent value="config" className="mt-5 min-w-0 space-y-4">
          <Card className="border-sky-500/20 bg-sky-500/5">
            <CardContent className="pt-5">
              <p className="text-sm text-muted-foreground">
                当前支持字段：关键词、城市、薪资范围。暂未开放的筛选项会保留在平台字典和后端契约中，避免误导为配置缺失。
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <BiBriefcase className="text-primary" />
                智联招聘平台说明
              </CardTitle>
              <CardDescription>登录与投递操作提示</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <p className="text-sm text-muted-foreground">请在浏览器标签页中登录智联招聘平台，登录成功后系统会自动检测登录状态。</p>
                <p className="text-sm text-muted-foreground">登录成功后，点击&quot;开始投递&quot;按钮启动自动投递任务。</p>
                <p className="text-sm text-muted-foreground">点击&quot;保存配置&quot;按钮可手动保存当前登录相关信息到数据库。</p>
              </div>
            </CardContent>
          </Card>

          {/* 配置表单 */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <BiBriefcase className="text-primary" />
                配置参数
              </CardTitle>
              <CardDescription>设置关键词、目标城市和薪资范围</CardDescription>
            </CardHeader>
            <CardContent>
              {loadingConfig ? (
                <p className="text-sm text-muted-foreground">配置加载中...</p>
              ) : (
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                  <div className="space-y-2">
                    <Label>搜索关键词（逗号分隔）</Label>
                    <Input
                      placeholder="如：Java, 后端, Spring"
                      value={config.keywords || ''}
                      onChange={(e) => setConfig((c) => ({ ...c, keywords: e.target.value }))}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>城市</Label>
                    <Select
                      value={config.cityCode || ''}
                      onChange={(e) => setConfig((c) => ({ ...c, cityCode: e.target.value }))}
                      placeholder="请选择城市"
                    >
                      {options.city.map((o) => (
                        <option key={o.code} value={o.code}>{o.name}</option>
                      ))}
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <Label>薪资范围</Label>
                      <button
                        type="button"
                        onClick={() => {
                          setIsCustomSalary(!isCustomSalary)
                          if (!isCustomSalary) setConfig((c) => ({ ...c, salary: '' }))
                        }}
                        className="text-xs text-primary hover:underline"
                      >
                        {isCustomSalary ? '从列表选择' : '手动输入'}
                      </button>
                    </div>
                    {isCustomSalary ? (
                      <Input
                        placeholder="如：12000, 20000 或 不限"
                        value={config.salary || ''}
                        onChange={(e) => setConfig((c) => ({ ...c, salary: e.target.value }))}
                      />
                    ) : (
                      <Select
                        value={config.salary || ''}
                        onChange={(e) => setConfig((c) => ({ ...c, salary: e.target.value }))}
                        placeholder="请选择薪资范围"
                      >
                        <option value="">请选择薪资范围</option>
                        {options.salary.map((o) => (
                          <option key={o.code} value={o.code}>{o.name}</option>
                        ))}
                      </Select>
                    )}
                    <p className="text-xs text-muted-foreground">
                      {options.salary.length === 0 && !isCustomSalary ? '暂无薪资字典，请到基础数据维护，或切换为手动输入' : '薪资范围来自平台字典，也可以手动输入最低/最高工资'}
                    </p>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="analytics" className="mt-5 min-w-0 space-y-5">
          <AnalysisContent />
        </TabsContent>
      </Tabs>

      {/* 退出确认弹框 */}
      {showLogoutDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <Card className="w-[92%] max-w-sm border-0">
            <CardHeader className="pb-2">
              <CardTitle className="text-lg flex items-center gap-2">
                <BiLogOut className="text-red-500" /> 确认退出登录
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground mb-4">退出后将清除Cookie并切换为未登录状态。</p>
              <div className="flex justify-end gap-2">
                <Button variant="ghost" onClick={() => setShowLogoutDialog(false)} className="px-3">取消</Button>
                <Button onClick={async () => { await triggerLogout(); setShowLogoutDialog(false) }} variant="destructive" className="px-3">确认退出</Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      <FeedbackDialog
        open={showLogoutResultDialog && Boolean(logoutResult)}
        title={logoutResult?.success ? '退出登录成功' : '退出登录失败'}
        message={logoutResult?.message}
        tone={logoutResult?.success ? 'success' : 'error'}
        onClose={() => setShowLogoutResultDialog(false)}
      />

      <FeedbackDialog
        open={showSaveDialog && Boolean(saveResult)}
        title={saveResult?.success ? '保存成功' : '保存失败'}
        message={saveResult?.message}
        tone={saveResult?.success ? 'success' : 'error'}
        onClose={() => setShowSaveDialog(false)}
      />
    </div>
  )
}
