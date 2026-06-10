'use client'

import { useCallback, useState, useEffect } from 'react'
import { BiSave, BiKey, BiLinkExternal, BiCodeAlt, BiInfoCircle } from 'react-icons/bi'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import PageHeader from '@/app/components/PageHeader'
import { useAuthedRequest } from '@/components/auth/useAuthedRequest'
import { isApiAuthError } from '@/lib/api-client'
import { FeedbackDialog } from '@/components/workbench/feedback-dialog'

export default function EnvConfig() {
  const { authedFetch } = useAuthedRequest()
  const [envConfig, setEnvConfig] = useState({
    hookUrl: '',
    baseUrl: '',
    apiKey: '',
    model: '',
    botIsSend: 0,
  })

  const [showApiKey, setShowApiKey] = useState(false)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [showSaveDialog, setShowSaveDialog] = useState(false)
  const [saveResult, setSaveResult] = useState<{ success: boolean; message: string } | null>(null)

  // 从数据库加载配置
  const fetchConfig = useCallback(async () => {
    try {
      setLoading(true)
      const response = await authedFetch('/api/config', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      })

      if (!response.ok) {
        throw new Error('获取配置失败')
      }

      const result = await response.json()

      if (result.success && result.data) {
        setEnvConfig({
          hookUrl: result.data.HOOK_URL || '',
          baseUrl: result.data.BASE_URL || '',
          apiKey: result.data.API_KEY || '',
          model: result.data.MODEL || '',
          botIsSend: (() => {
            const raw = result.data.BOT_IS_SEND
            const val = String(raw ?? '').trim().toLowerCase()
            return val === '1' || val === 'true' ? 1 : 0
          })(),
        })
      }
    } catch (error) {
      if (isApiAuthError(error)) return
      console.error('获取配置失败:', error)
      setSaveResult({ success: false, message: '获取配置失败，请检查后端服务是否正常运行。' })
      setShowSaveDialog(true)
    } finally {
      setLoading(false)
    }
  }, [authedFetch])

  useEffect(() => {
    queueMicrotask(() => {
      void fetchConfig()
    })
  }, [fetchConfig])

  const handleSave = async (silent: boolean = false) => {
    try {
      setSaving(true)

      const configMap = {
        HOOK_URL: envConfig.hookUrl,
        BASE_URL: envConfig.baseUrl,
        API_KEY: envConfig.apiKey,
        MODEL: envConfig.model,
        BOT_IS_SEND: String(envConfig.botIsSend ?? 0),
      }

      const response = await authedFetch('/api/config', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(configMap),
      })

      if (!response.ok) {
        throw new Error('保存配置失败')
      }

      const result = await response.json()

      if (result.success) {
        if (!silent) {
          setSaveResult({ success: true, message: '保存成功' })
          setShowSaveDialog(true)
        }
      } else {
        throw new Error(result.message || '保存配置失败')
      }
    } catch (error) {
      if (isApiAuthError(error)) return
      console.error('保存配置失败:', error)
      if (!silent) {
        setSaveResult({ success: false, message: '保存配置失败：网络或服务异常。' })
        setShowSaveDialog(true)
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        icon={<BiCodeAlt className="text-2xl" />}
        title="环境变量配置"
        subtitle="后端配置存储与通知参数管理"
        actions={
          <Button
            onClick={() => handleSave(false)}
            size="sm"
            className="px-3"
            disabled={saving}
          >
            <BiSave className="mr-1" /> {saving ? '保存中...' : '保存配置'}
          </Button>
        }
      />

      {loading && (
        <Card className="border-blue-500/20 bg-blue-500/5">
          <CardContent className="pt-6">
            <p className="text-center text-sm text-muted-foreground">加载配置中...</p>
          </CardContent>
        </Card>
      )}

      <div className="space-y-6">
        {/* 企业微信 Webhook */}
        <Card className="animate-in fade-in slide-in-from-bottom-5 duration-700">
          <CardHeader className="flex items-start gap-4">
            <div className="min-w-0 space-y-2">
              <CardTitle className="flex items-center gap-2">
                <BiLinkExternal className="text-primary" />
                企业微信 Webhook
              </CardTitle>
              <CardDescription>配置企业微信群机器人，用于接收通知消息</CardDescription>
            </div>
            <div>
              <button
                type="button"
                aria-label="企业微信发送开关"
                onClick={() => setEnvConfig({ ...envConfig, botIsSend: envConfig.botIsSend ? 0 : 1 })}
                className={`relative inline-flex h-7 w-14 rounded-full border border-border transition-colors focus:outline-none focus:ring-2 focus:ring-ring/30 ${envConfig.botIsSend ? 'bg-primary hover:bg-primary/90' : 'bg-muted hover:bg-muted/80'}`}
              >
                <span
                  className={`absolute top-1 left-1 h-5 w-5 rounded-full bg-white shadow transition-transform ${envConfig.botIsSend ? 'translate-x-7' : 'translate-x-0'}`}
                />
              </button>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              <Label htmlFor="hookUrl">Webhook URL</Label>
              <Input
                id="hookUrl"
                type="text"
                value={envConfig.hookUrl}
                onChange={(e) => setEnvConfig({ ...envConfig, hookUrl: e.target.value })}
                placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=your_key"
              />
              <p className="text-xs text-muted-foreground">
                企业微信群机器人webhook地址，用于接收通知消息
              </p>
            </div>
          </CardContent>
        </Card>

        {/* API 配置 */}
        <Card className="animate-in fade-in slide-in-from-bottom-6 duration-700">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BiCodeAlt className="text-primary" />
              API 配置
            </CardTitle>
            <CardDescription>配置 API 服务器地址和使用的 AI 模型</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label htmlFor="baseUrl">API Base URL</Label>
                <Input
                  id="baseUrl"
                  type="text"
                  value={envConfig.baseUrl}
                  onChange={(e) => setEnvConfig({ ...envConfig, baseUrl: e.target.value })}
                  placeholder="https://api.ruyun.fun"
                />
                <p className="text-xs text-muted-foreground">API服务器地址</p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="model">AI模型</Label>
                <Input
                  id="model"
                  type="text"
                  value={envConfig.model}
                  onChange={(e) => setEnvConfig({ ...envConfig, model: e.target.value })}
                  placeholder="gpt-5-nano-2025-08-07"
                />
                <p className="text-xs text-muted-foreground">使用的AI模型名称</p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* API 密钥 */}
        <Card className="animate-in fade-in slide-in-from-bottom-7 duration-700">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BiKey className="text-primary" />
              API 密钥
            </CardTitle>
            <CardDescription>配置 API 访问密钥，请妥善保管</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              <Label htmlFor="apiKey">API Key</Label>
              <div className="relative">
                <Input
                  id="apiKey"
                  type={showApiKey ? 'text' : 'password'}
                  value={envConfig.apiKey}
                  onChange={(e) => setEnvConfig({ ...envConfig, apiKey: e.target.value })}
                  placeholder="sk-xxxxxxxxxxxxxxxxx"
                />
                <Button
                  onClick={() => setShowApiKey(!showApiKey)}
                  variant="ghost"
                  size="sm"
                  className="absolute right-1 top-1/2 -translate-y-1/2 h-7"
                  type="button"
                >
                  {showApiKey ? '隐藏' : '显示'}
                </Button>
              </div>
              <p className="text-xs text-muted-foreground">
                🔐 API密钥将被安全存储，请妥善保管
              </p>
            </div>
          </CardContent>
        </Card>

        {/* 安全提示 */}
        <Card className="border-primary/20 bg-primary/5 animate-in fade-in slide-in-from-bottom-8 duration-700">
          <CardContent className="pt-6">
            <div className="flex gap-3">
              <BiInfoCircle className="h-5 w-5 text-primary flex-shrink-0 mt-0.5" />
              <div>
                <p className="text-sm text-foreground">
                  <strong className="font-semibold">提示：</strong> 这些值会保存到后端配置存储中，并在投递和通知流程中使用。
                  API Key 默认脱敏展示，请只在确认环境安全时切换为明文。
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* 操作按钮已移至页头右上角 */}
      </div>
      <FeedbackDialog
        open={showSaveDialog && Boolean(saveResult)}
        title={saveResult?.success ? '保存成功' : '操作失败'}
        message={saveResult?.message}
        tone={saveResult?.success ? 'success' : 'error'}
        onClose={() => setShowSaveDialog(false)}
      />
    </div>
  )
}
