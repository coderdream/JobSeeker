'use client'

import { useCallback, useEffect, useState } from 'react'
import { BiBriefcase, BiLogOut, BiPlay, BiSave, BiStop } from 'react-icons/bi'
import PageHeader from '@/app/components/PageHeader'
import { useAuthedRequest } from '@/components/auth/useAuthedRequest'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  getPlatformStatus,
  isPlatformAlreadyRunningError,
  openPlatformLogin,
  savePlatformCookie,
  startPlatformTask,
} from '@/lib/platform-requests'
import { ConfirmDialog, FeedbackDialog } from '@/components/workbench/feedback-dialog'
import { PlatformStatusBar } from '@/components/workbench/platform-status-bar'
import AnalysisContent from './analysis/AnalysisContent'

type Option = { name: string; code: string }

type YupaoConfig = {
  id?: number
  keywords?: string
  cityCode?: string
  salary?: string
  jobType?: string
}

type YupaoOptions = {
  city: Option[]
  salary: Option[]
  jobType: Option[]
}

function parseKeywordsFromDb(raw?: string) {
  if (!raw) return ''
  const value = raw.trim()
  if (value.startsWith('[') && value.endsWith(']')) {
    try {
      const parsed = JSON.parse(value)
      if (Array.isArray(parsed)) return parsed.filter(Boolean).join(', ')
    } catch {
      return value
    }
  }
  return value.replace(/，/g, ',')
}

function serializeKeywordsForDb(raw?: string) {
  const value = (raw || '').replace(/，/g, ',').trim()
  if (!value) return '[]'
  return JSON.stringify(value.split(',').map((item) => item.trim()).filter(Boolean))
}

export default function YupaoPage() {
  const { token, authedFetch } = useAuthedRequest()
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [isDelivering, setIsDelivering] = useState(false)
  const [isStartingDelivery, setIsStartingDelivery] = useState(false)
  const [checkingLogin, setCheckingLogin] = useState(true)
  const [loginPolling, setLoginPolling] = useState(false)
  const [loadingConfig, setLoadingConfig] = useState(true)
  const [saveMessage, setSaveMessage] = useState('')
  const [feedback, setFeedback] = useState<{ success: boolean; title: string; message: string } | null>(null)
  const [showLogoutDialog, setShowLogoutDialog] = useState(false)
  const [config, setConfig] = useState<YupaoConfig>({ keywords: '', cityCode: '', salary: '', jobType: '' })
  const [options, setOptions] = useState<YupaoOptions>({ city: [], salary: [], jobType: [] })

  const fetchAllData = useCallback(async () => {
    try {
      const response = await authedFetch('/api/yupao/config')
      const data = await response.json()
      if (data.config) {
        setConfig({ ...data.config, keywords: parseKeywordsFromDb(data.config.keywords) })
      }
      if (data.options) {
        setOptions({
          city: data.options.city || [],
          salary: data.options.salary || [],
          jobType: data.options.jobType || [],
        })
      }
    } finally {
      setLoadingConfig(false)
    }
  }, [authedFetch])

  const refreshDeliveryStatus = useCallback(async () => {
    const status = await getPlatformStatus(authedFetch, 'yupao')
    setIsLoggedIn(Boolean(status.isLoggedIn))
    setIsDelivering(Boolean(status.isRunning))
    return status
  }, [authedFetch])

  useEffect(() => {
    if (!token) return
    void fetchAllData()
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void refreshDeliveryStatus().finally(() => setCheckingLogin(false))
  }, [fetchAllData, refreshDeliveryStatus, token])

  useEffect(() => {
    if (!loginPolling || !token) return
    let attempts = 0

    const pollLoginStatus = async () => {
      attempts += 1
      try {
        const status = await getPlatformStatus(authedFetch, 'yupao', { refreshLogin: true })
        const loggedIn = Boolean(status.isLoggedIn)
        setIsLoggedIn(loggedIn)
        if (loggedIn) {
          await savePlatformCookie(authedFetch, 'yupao').catch(() => undefined)
        }
        if (loggedIn || attempts >= 150) {
          setCheckingLogin(false)
          setLoginPolling(false)
        }
      } catch {
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

  useEffect(() => {
    if (!isDelivering || !token) return
    const pollStatus = async () => {
      await refreshDeliveryStatus().catch(() => undefined)
    }
    void pollStatus()
    const interval = window.setInterval(() => {
      void pollStatus()
    }, 3000)
    return () => window.clearInterval(interval)
  }, [isDelivering, refreshDeliveryStatus, token])

  const handleOpenLogin = async () => {
    setCheckingLogin(true)
    try {
      await openPlatformLogin(authedFetch, 'yupao')
      setLoginPolling(true)
    } catch (error) {
      console.error('[Yupao] open login failed:', error)
      setCheckingLogin(false)
      setLoginPolling(false)
    }
  }

  const handleStartDelivery = async () => {
    try {
      setIsStartingDelivery(true)
      setIsDelivering(true)
      await startPlatformTask(authedFetch, 'yupao')
      await refreshDeliveryStatus()
    } catch (error) {
      if (isPlatformAlreadyRunningError(error)) {
        setIsDelivering(true)
        await refreshDeliveryStatus()
        return
      }
      console.error('[Yupao] start failed:', error)
      setIsDelivering(false)
    } finally {
      setIsStartingDelivery(false)
    }
  }

  const handleStopDelivery = async () => {
    const response = await authedFetch('/api/yupao/stop', { method: 'POST' })
    const data = await response.json().catch(() => ({}))
    if (response.ok && data.success !== false) setIsDelivering(false)
  }

  const handleLogout = async () => {
    const response = await authedFetch('/api/yupao/logout', { method: 'POST' })
    if (response.ok) {
      setIsLoggedIn(false)
      setLoginPolling(false)
      setFeedback({ success: true, title: '退出登录成功', message: '鱼泡 Cookie 已清空，登录状态已切换为未登录。' })
    } else {
      setFeedback({ success: false, title: '退出登录失败', message: '退出登录失败，请检查后端服务。' })
    }
  }

  const handleSaveConfig = async () => {
    const payload = { ...config, keywords: serializeKeywordsForDb(config.keywords) }
    const response = await authedFetch('/api/yupao/config', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })
    if (response.ok) {
      await fetchAllData()
      setSaveMessage('已保存')
      setFeedback({ success: true, title: '保存成功', message: '鱼泡配置已保存。' })
    } else {
      setSaveMessage('保存失败')
      setFeedback({ success: false, title: '保存失败', message: '鱼泡配置保存失败，请稍后重试。' })
    }
  }

  const actionButton = checkingLogin ? (
    <Button size="sm" disabled variant="secondary">
      <BiPlay /> 检查登录中
    </Button>
  ) : !isLoggedIn ? (
    <Button onClick={handleOpenLogin} size="sm">
      <BiPlay /> 打开登录
    </Button>
  ) : isStartingDelivery ? (
    <Button size="sm" disabled>
      <BiPlay /> 启动中
    </Button>
  ) : isDelivering ? (
    <Button onClick={handleStopDelivery} size="sm" variant="destructive">
      <BiStop /> 停止任务
    </Button>
  ) : (
    <Button onClick={handleStartDelivery} size="sm">
      <BiPlay /> 开始投递
    </Button>
  )

  return (
    <div className="space-y-6">
      <PageHeader
        icon={<BiBriefcase />}
        title="鱼泡直聘"
        subtitle="试验平台 Beta：求职端自动化"
        iconClass="text-white"
        accentBgClass="bg-emerald-600"
        actions={
          <>
            {actionButton}
            <Button onClick={() => setShowLogoutDialog(true)} size="sm" variant="outline">
              <BiLogOut /> 退出
            </Button>
            <Button onClick={handleSaveConfig} size="sm">
              <BiSave /> 保存
            </Button>
          </>
        }
      />

      <PlatformStatusBar
        platform="鱼泡直聘"
        description="试验平台 Beta：当前仅开放基础搜索条件和投递状态轮询。"
        items={[
          { label: '登录', value: checkingLogin ? '检查中' : isLoggedIn ? '已登录' : '未登录', tone: checkingLogin ? 'info' : isLoggedIn ? 'success' : 'warning' },
          { label: '任务', value: isDelivering ? '运行中' : isStartingDelivery ? '启动中' : '空闲', tone: isDelivering ? 'success' : isStartingDelivery ? 'info' : 'neutral' },
          { label: '平台', value: 'Beta', tone: 'warning' },
        ]}
      />

      <Tabs defaultValue="config" className="min-w-0">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="config">配置</TabsTrigger>
          <TabsTrigger value="analytics">分析</TabsTrigger>
        </TabsList>

        <TabsContent value="config" className="mt-5 min-w-0 space-y-5">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">参数</CardTitle>
            </CardHeader>
            <CardContent>
              {loadingConfig ? (
                <p className="text-sm text-muted-foreground">加载中...</p>
              ) : (
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                  <div className="space-y-2">
                    <Label>关键词</Label>
                    <Input
                      placeholder="Java, 后端, Spring"
                      value={config.keywords || ''}
                      onChange={(event) => setConfig((current) => ({ ...current, keywords: event.target.value }))}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>城市</Label>
                    <Select
                      value={config.cityCode || ''}
                      onChange={(event) => setConfig((current) => ({ ...current, cityCode: event.target.value }))}
                      placeholder="选择城市"
                    >
                      <option value="">不限</option>
                      {options.city.map((option) => (
                        <option key={option.code} value={option.code}>{option.name}</option>
                      ))}
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label>薪资</Label>
                    <Select
                      value={config.salary || ''}
                      onChange={(event) => setConfig((current) => ({ ...current, salary: event.target.value }))}
                      placeholder="选择薪资"
                    >
                      {options.salary.map((option) => (
                        <option key={option.code} value={option.code}>{option.name}</option>
                      ))}
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label>岗位类型</Label>
                    <Select
                      value={config.jobType || ''}
                      onChange={(event) => setConfig((current) => ({ ...current, jobType: event.target.value }))}
                      placeholder="选择类型"
                    >
                      {options.jobType.map((option) => (
                        <option key={option.code} value={option.code}>{option.name}</option>
                      ))}
                    </Select>
                  </div>
                </div>
              )}
              {saveMessage ? <p className="mt-4 text-sm text-muted-foreground">{saveMessage}</p> : null}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="analytics" className="mt-5 min-w-0">
          <AnalysisContent />
        </TabsContent>
      </Tabs>
      <ConfirmDialog
        open={showLogoutDialog}
        title="确认退出登录"
        message="退出后将清除鱼泡 Cookie 并切换为未登录状态。"
        confirmLabel="确认退出"
        destructive
        onClose={() => setShowLogoutDialog(false)}
        onConfirm={handleLogout}
      />
      <FeedbackDialog
        open={Boolean(feedback)}
        title={feedback?.title || ''}
        message={feedback?.message}
        tone={feedback?.success ? 'success' : 'error'}
        onClose={() => setFeedback(null)}
      />
    </div>
  )
}
