'use client'

import { useState, useEffect, useCallback } from 'react'
import { createSSEWithBackoff } from '@/lib/sse'
import { BiSearch, BiSave, BiTargetLock, BiMoney, BiPlay, BiStop, BiLogOut, BiBriefcase } from 'react-icons/bi'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { Combobox } from '@/components/ui/combobox'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import AnalysisContent from '@/app/liepin/analysis/AnalysisContent'
import PageHeader from '@/app/components/PageHeader'
import { useAuthedRequest } from '@/components/auth/useAuthedRequest'
import { getApiBaseUrl } from '@/lib/api-client'
import { getPlatformStatus, isPlatformAlreadyRunningError, openPlatformLogin, startPlatformTask } from '@/lib/platform-requests'
import { FeedbackDialog } from '@/components/workbench/feedback-dialog'
import { PlatformStatusBar } from '@/components/workbench/platform-status-bar'

interface LiepinConfig {
  id?: number
  keywords?: string
  city?: string
  salaryCode?: string
  compTag?: string
  pubTime?: string
  workYearCode?: string
  eduLevel?: string
  industry?: string
  jobKind?: string
  compScale?: string
  compStage?: string
  compKind?: string
}

interface LiepinOption {
  id: number
  type: string
  name: string
  code: string
}

interface LiepinOptions {
  city: LiepinOption[]
  salary: LiepinOption[]
  compTag: LiepinOption[]
  pubTime: LiepinOption[]
  workYearCode: LiepinOption[]
  degree: LiepinOption[]
  industry: LiepinOption[]
  jobType: LiepinOption[]
  scale: LiepinOption[]
  stage: LiepinOption[]
  compKind: LiepinOption[]
}

type LiepinFilterField =
  | 'compTag'
  | 'pubTime'
  | 'workYearCode'
  | 'eduLevel'
  | 'industry'
  | 'jobKind'
  | 'compScale'
  | 'compStage'
  | 'compKind'

export default function LiepinPage() {
  const { token, authedFetch } = useAuthedRequest()
  const [config, setConfig] = useState<LiepinConfig>({
    keywords: '',
    city: '',
    salaryCode: '',
    compTag: '',
    pubTime: '',
    workYearCode: '',
    eduLevel: '',
    industry: '',
    jobKind: '',
    compScale: '',
    compStage: '',
    compKind: '',
  })
  const [options, setOptions] = useState<LiepinOptions>({
    city: [],
    salary: [],
    compTag: [],
    pubTime: [],
    workYearCode: [],
    degree: [],
    industry: [],
    jobType: [],
    scale: [],
    stage: [],
    compKind: [],
  })
  const [loading, setLoading] = useState(true)
  const [showSaveDialog, setShowSaveDialog] = useState(false)
  const [saveResult, setSaveResult] = useState<{ success: boolean; message: string } | null>(null)
  const [isCustomCity, setIsCustomCity] = useState(false) // 是否手动输入城市
  const [isCustomSalary, setIsCustomSalary] = useState(false)
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [isDelivering, setIsDelivering] = useState(false)
  const [isStartingDelivery, setIsStartingDelivery] = useState(false)
  const [checkingLogin, setCheckingLogin] = useState(true)
  const [loginPolling, setLoginPolling] = useState(false)
  const [showLogoutDialog, setShowLogoutDialog] = useState(false)
  const [showLogoutResultDialog, setShowLogoutResultDialog] = useState(false)
  const [logoutResult, setLogoutResult] = useState<{ success: boolean; message: string } | null>(null)

  useEffect(() => {
    if (!token) {
      return
    }

    // 确保在客户端环境且支持 fetch 流式读取
    if (typeof window === 'undefined' || typeof ReadableStream === 'undefined') {
      console.warn('ReadableStream 不可用，无法连接SSE')
      return
    }

    const client = createSSEWithBackoff(`${getApiBaseUrl()}/api/jobs/login-status/stream`, {
      token,
      onOpen: () => {
        console.log('[SSE] 连接已打开')
      },
      onError: (e, attempt, delay) => {
        console.warn(`[SSE] 连接错误，准备第${attempt}次重连，延迟 ${delay}ms`, e)
        setCheckingLogin(false)
      },
      listeners: [
        {
          name: 'connected',
          handler: (event) => {
            try {
              const data = JSON.parse(event.data)
              setIsLoggedIn(data.liepinLoggedIn || false)
              setCheckingLogin(false)
            } catch (error) {
              console.error('[SSE] 解析连接消息失败:', error)
            }
          },
        },
        {
          name: 'login-status',
          handler: (event) => {
            try {
              const data = JSON.parse(event.data)
              if (data.platform === 'liepin') {
                const loggedIn = Boolean(data.isLoggedIn)
                setIsLoggedIn(loggedIn)
                if (loggedIn) {
                  setCheckingLogin(false)
                  setLoginPolling(false)
                }
              }
            } catch (error) {
              console.error('[SSE] 解析登录状态消息失败:', error)
            }
          },
        },
        { name: 'ping', handler: () => {} },
      ],
    })

    return () => {
      client.close()
    }
  }, [token])

  useEffect(() => {
    if (!loginPolling || !token) {
      return
    }

    let attempts = 0
    const pollLoginStatus = async () => {
      attempts += 1
      try {
        const status = await getPlatformStatus(authedFetch, 'liepin')
        const loggedIn = Boolean(status.isLoggedIn)
        setIsLoggedIn(loggedIn)
        if (loggedIn || attempts >= 150) {
          setCheckingLogin(false)
          setLoginPolling(false)
        }
      } catch (error) {
        console.warn('[Liepin login polling] failed:', error)
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
    const status = await getPlatformStatus(authedFetch, 'liepin')
    setIsLoggedIn(Boolean(status.isLoggedIn))
    setIsDelivering(Boolean(status.isRunning))
  }, [authedFetch])

  useEffect(() => {
    if (!isDelivering || !token) {
      return
    }

    const pollStatus = async () => {
      try {
        await refreshDeliveryStatus()
      } catch (error) {
        console.warn('[Liepin status polling] failed:', error)
      }
    }

    void pollStatus()
    const interval = window.setInterval(() => {
      void pollStatus()
    }, 3000)

    return () => window.clearInterval(interval)
  }, [isDelivering, refreshDeliveryStatus, token])

  // 将数据库中的 JSON 数组字符串转换为逗号分隔的可读字符串
  const parseKeywordsFromDb = (raw?: string): string => {
    if (!raw) return ''
    const t = raw.trim()
    if (t.startsWith('[') && t.endsWith(']')) {
      try {
        const arr = JSON.parse(t)
        if (Array.isArray(arr)) return arr.filter(Boolean).join(', ')
      } catch (e) {
        console.warn('解析关键词JSON失败，使用原值:', e)
      }
    }
    // 兼容逗号或中文逗号分隔的原始字符串
    return t.replace(/，/g, ',')
  }

  // 将输入的逗号分隔字符串转换为 JSON 数组字符串保存到数据库
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
      const response = await authedFetch('/api/liepin/config')
      const data = await response.json()

      console.log('Fetched liepin data:', data)

      if (data.config) {
        const normalized = { ...data.config }
        normalized.keywords = parseKeywordsFromDb(data.config.keywords)
        setConfig(normalized)
        // 检查当前城市是否在选项列表中
        if (data.options?.city && data.config.city) {
          const cityExists = data.options.city.some((c: LiepinOption) => c.name === data.config.city || c.code === data.config.city)
          setIsCustomCity(!cityExists)
        }
      }
      if (data.options) {
        setOptions({
          city: data.options.city || [],
          salary: data.options.salary || [],
          compTag: data.options.compTag || [],
          pubTime: data.options.pubTime || [],
          workYearCode: data.options.workYearCode || [],
          degree: data.options.degree || [],
          industry: data.options.industry || [],
          jobType: data.options.jobType || [],
          scale: data.options.scale || [],
          stage: data.options.stage || [],
          compKind: data.options.compKind || [],
        })
      }
    } catch (error) {
      console.error('Failed to fetch liepin data:', error)
    } finally {
      setLoading(false)
    }
  }, [authedFetch])

  useEffect(() => {
    queueMicrotask(() => void fetchAllData())
  }, [fetchAllData])

  const handleSave = async () => {
    try {
      const payload = { ...config, keywords: serializeKeywordsForDb(config.keywords) }
      const response = await authedFetch('/api/liepin/config', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
      })

      if (response.ok) {
        // 统一保存 Cookie（Liepin）
        try {
          await authedFetch('/api/cookie/save?platform=liepin', { method: 'POST' })
        } catch (e) {
          console.warn('保存 Cookie 失败（Liepin）:', e)
        }

        fetchAllData()
        setSaveResult({ success: true, message: '保存成功，配置与Cookie已更新。' })
        setShowSaveDialog(true)
      } else {
        console.warn('保存失败：后端返回非 2xx 状态')
        setSaveResult({ success: false, message: '保存失败：后端返回异常状态。' })
        setShowSaveDialog(true)
      }
    } catch (error) {
      console.error('Failed to save config:', error)
      setSaveResult({ success: false, message: '保存失败：网络或服务异常。' })
      setShowSaveDialog(true)
    }
  }

  const handleStartDelivery = async () => {
    try {
      setIsStartingDelivery(true)
      setIsDelivering(true)
      await startPlatformTask(authedFetch, 'liepin')
      await refreshDeliveryStatus()
    } catch (error) {
      if (isPlatformAlreadyRunningError(error)) {
        setIsDelivering(true)
        await refreshDeliveryStatus()
        return
      }
      console.error('Failed to start delivery:', error)
      // 启动失败：不弹框
      setIsDelivering(false)
    } finally {
      setIsStartingDelivery(false)
    }
  }

  const handleOpenLogin = async () => {
    try {
      setCheckingLogin(true)
      await openPlatformLogin(authedFetch, 'liepin')
      setLoginPolling(true)
    } catch (error) {
      console.error('[Liepin] 打开登录入口失败:', error)
      setCheckingLogin(false)
      setLoginPolling(false)
    }
  }

  const handleStopDelivery = async () => {
    try {
      const response = await authedFetch('/api/liepin/stop', {
        method: 'POST',
      })
      const data = await response.json()

      if (data.success) {
        // 停止成功：不弹框
        setIsDelivering(false)
      } else {
        // 停止失败：不弹框
        console.warn('停止失败：', data.message)
      }
    } catch (error) {
      console.error('Failed to stop delivery:', error)
      // 停止失败：不弹框
    }
  }

  const triggerLogout = async () => {
    try {
      const response = await authedFetch('/api/liepin/logout', { method: 'POST' })
      const data = await response.json()
      if (data.success) {
        setIsLoggedIn(false)
        setIsDelivering(false)
        setLoginPolling(false)
        console.info('已退出登录，数据库Cookie已置空')
        setLogoutResult({ success: true, message: '已退出登录，Cookie已清空。' })
        setShowLogoutResultDialog(true)
      } else {
        console.warn('退出登录失败：', data.message)
        setLogoutResult({ success: false, message: `退出登录失败：${data.message || '服务返回异常。'}` })
        setShowLogoutResultDialog(true)
      }
    } catch (error) {
      console.error('Failed to logout:', error)
      setLogoutResult({ success: false, message: '退出登录失败：网络或服务异常。' })
      setShowLogoutResultDialog(true)
    }
  }

  const renderFilterCombobox = (field: LiepinFilterField, label: string, optionList: LiepinOption[]) => {
    const normalizedOptions = optionList
      .filter((option) => option && (option.code || option.name))
      .map((option) => ({ code: String(option.code ?? ''), name: String(option.name ?? '') }))
    const hasUnlimited = normalizedOptions.some((option) => option.code === '0' || option.code === '' || option.name === '不限')
    const filterOptions = hasUnlimited ? normalizedOptions : [{ code: '0', name: '不限' }, ...normalizedOptions]
    return (
      <div className="space-y-2">
        <Label htmlFor={field}>{label}</Label>
        <Combobox
          id={field}
          value={config[field] || ''}
          options={filterOptions}
          allowCustom
          placeholder="不限 / 输入平台编码"
          emptyText="暂无字典，可直接输入平台编码"
          onChange={(value) => setConfig((prev) => ({ ...prev, [field]: value }))}
        />
      </div>
    )
  }

  if (loading) {
    return <div className="flex items-center justify-center h-screen">加载中...</div>
  }

  return (
    <div className="space-y-6">
      <PageHeader
        icon={<BiSearch className="text-2xl" />}
        title="猎聘配置"
        subtitle="配置猎聘平台的求职参数"
        iconClass="text-white"
        accentBgClass="bg-orange-500"
        actions={
          <div className="flex items-center gap-2">
            {checkingLogin ? (
              <Button size="sm" disabled variant="secondary" className="px-3">
                <BiPlay className="mr-1" /> 检查登录中...
              </Button>
            ) : !isLoggedIn ? (
              <Button onClick={handleOpenLogin} size="sm" className="px-3">
                <BiPlay className="mr-1" /> 打开猎聘登录
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
            <Button onClick={handleSave} size="sm" className="px-3">
              <BiSave className="mr-1" /> 保存配置
            </Button>
          </div>
        }
      />

      <PlatformStatusBar
        platform="猎聘"
        description="支持平台字典选择，也可以在高级筛选中手动输入猎聘平台编码。"
        items={[
          { label: '登录', value: checkingLogin ? '检查中' : isLoggedIn ? '已登录' : '未登录', tone: checkingLogin ? 'info' : isLoggedIn ? 'success' : 'warning' },
          { label: '任务', value: isDelivering ? '运行中' : isStartingDelivery ? '启动中' : '空闲', tone: isDelivering ? 'success' : isStartingDelivery ? 'info' : 'neutral' },
          { label: '配置', value: '可编辑', tone: 'neutral' },
        ]}
      />

      <Tabs defaultValue="config" className="min-w-0">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="config">平台配置</TabsTrigger>
          <TabsTrigger value="analytics">投递分析</TabsTrigger>
        </TabsList>

        <TabsContent value="config" className="mt-5 min-w-0 space-y-4">
        {/* 平台说明 */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BiBriefcase className="text-primary" />
              猎聘平台说明
            </CardTitle>
            <CardDescription>登录与投递操作提示</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <p className="text-sm text-muted-foreground">请在浏览器标签页中登录 猎聘 平台，登录成功后系统会自动检测登录状态。</p>
              <p className="text-sm text-muted-foreground">登录成功后，点击“开始投递”按钮启动自动投递任务。</p>
              <p className="text-sm text-muted-foreground">点击“保存配置”按钮可手动保存当前登录相关信息到数据库。</p>
            </div>
          </CardContent>
        </Card>

        {/* 搜索配置 */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BiSearch className="text-primary" />
              搜索配置
            </CardTitle>
            <CardDescription>设置职位搜索关键词和筛选条件</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
              <div className="space-y-2">
                <Label htmlFor="keywords">搜索关键词</Label>
                <Input
                  id="keywords"
                  value={config.keywords || ''}
                  onChange={(e) => setConfig({ ...config, keywords: e.target.value })}
                  placeholder="例如：大模型, Python, Golang"
                />
                <p className="text-xs text-muted-foreground">关键词可多选，使用英文逗号分隔，例如：大模型, Python, Golang</p>
              </div>

              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <Label htmlFor="city">工作城市</Label>
                  <button
                    type="button"
                    onClick={() => {
                      setIsCustomCity(!isCustomCity)
                      if (!isCustomCity) {
                        // 切换到手动输入时，清空当前值
                        setConfig({ ...config, city: '' })
                      }
                    }}
                    className="text-xs text-primary hover:underline"
                  >
                    {isCustomCity ? '从列表选择' : '手动输入'}
                  </button>
                </div>
                {isCustomCity ? (
                  <Input
                    id="city"
                    value={config.city || ''}
                    onChange={(e) => setConfig({ ...config, city: e.target.value })}
                    placeholder="请输入城市码，例如：410"
                  />
                ) : (
                  <Select
                    id="city"
                    value={config.city || ''}
                    onChange={(e) => setConfig({ ...config, city: e.target.value })}
                  >
                    <option value="">请选择城市</option>
                    {options.city.map((city) => (
                      <option key={city.id} value={city.name}>
                        {city.name}
                      </option>
                    ))}
                  </Select>
                )}
                <p className="text-xs text-muted-foreground">
                  {isCustomCity ? '手动输入城市码（例如：410代表北京）' : '从列表选择城市，或点击"手动输入"自定义'}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* 薪资配置 */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BiMoney className="text-primary" />
              薪资筛选
            </CardTitle>
            <CardDescription>设置期望薪资范围</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <Label htmlFor="salaryCode">薪资范围</Label>
                  <button
                    type="button"
                    onClick={() => {
                      setIsCustomSalary(!isCustomSalary)
                      if (!isCustomSalary) setConfig({ ...config, salaryCode: '' })
                    }}
                    className="text-xs text-primary hover:underline"
                  >
                    {isCustomSalary ? '从列表选择' : '手动输入'}
                  </button>
                </div>
                {isCustomSalary ? (
                  <Input
                    id="salaryCode"
                    value={config.salaryCode || ''}
                    onChange={(e) => setConfig({ ...config, salaryCode: e.target.value })}
                    placeholder="例如：15$30"
                  />
                ) : (
                  <Select
                    id="salaryCode"
                    value={config.salaryCode || ''}
                    onChange={(e) => setConfig({ ...config, salaryCode: e.target.value })}
                  >
                    <option value="">请选择薪资范围</option>
                    {options.salary.map((salary) => (
                      <option key={salary.code} value={salary.code}>{salary.name}</option>
                    ))}
                  </Select>
                )}
                <p className="text-xs text-muted-foreground">
                  {options.salary.length === 0 && !isCustomSalary ? '暂无薪资字典，请到基础数据维护，或切换为手动输入' : '薪资范围来自平台字典，也可以手动输入平台编码'}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BiTargetLock className="text-primary" />
              搜索筛选
            </CardTitle>
            <CardDescription>补充猎聘搜索条件</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
              {renderFilterCombobox('compTag', '名企', options.compTag)}
              {renderFilterCombobox('pubTime', '招聘者活跃', options.pubTime)}
              {renderFilterCombobox('workYearCode', '经验', options.workYearCode)}
              {renderFilterCombobox('eduLevel', '学历', options.degree)}
              {renderFilterCombobox('industry', '行业', options.industry)}
              {renderFilterCombobox('jobKind', '职位类型', options.jobType)}
              {renderFilterCombobox('compScale', '企业规模', options.scale)}
              {renderFilterCombobox('compStage', '融资阶段', options.stage)}
              {renderFilterCombobox('compKind', '企业性质', options.compKind)}
            </div>
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
          <div className="w-[92%] max-w-sm rounded-lg border border-border bg-background shadow-lg animate-in fade-in zoom-in-95">
            <Card className="border-0">
              <CardHeader className="pb-2">
                <CardTitle className="text-lg flex items-center gap-2">
                  <BiLogOut className="text-red-500" />
                  确认退出登录
                </CardTitle>
              </CardHeader>
              <CardContent className="pt-0">
                <p className="text-sm text-muted-foreground mb-4">退出后将清除Cookie并切换为未登录状态。</p>
                <div className="flex justify-end gap-2">
                  <Button
                    variant="ghost"
                    onClick={() => setShowLogoutDialog(false)}
                    className="px-3"
                  >
                    取消
                  </Button>
                  <Button
                    onClick={async () => {
                      await triggerLogout()
                      setShowLogoutDialog(false)
                    }}
                    variant="destructive"
                    className="px-3"
                  >
                    确认退出
                  </Button>
                </div>
              </CardContent>
            </Card>
          </div>
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
