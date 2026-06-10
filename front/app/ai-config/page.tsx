'use client'

import { useCallback, useState, useEffect } from 'react'
import { BiSave, BiBrain, BiInfoCircle } from 'react-icons/bi'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import PageHeader from '@/app/components/PageHeader'
import { useAuthedRequest } from '@/components/auth/useAuthedRequest'
import { isApiAuthError } from '@/lib/api-client'
import { FeedbackDialog } from '@/components/workbench/feedback-dialog'

export default function AiConfigPage() {
  const { authedFetch } = useAuthedRequest()
  const [aiConfig, setAiConfig] = useState({
    introduce: '',
    prompt: '',
  })

  const [loading, setLoading] = useState(false)
  // 是否启用AI（映射 boss_config.enable_ai）
  const [enableAi, setEnableAi] = useState<number>(0)
  const [feedback, setFeedback] = useState<{ success: boolean; title: string; message: string } | null>(null)

  const fetchAiConfig = useCallback(async () => {
    try {
      const response = await authedFetch('/api/ai/config', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const result = await response.json()
      if (result.success && result.data) {
        setAiConfig({
          introduce: result.data.introduce || '',
          prompt: result.data.prompt || '',
        })
      }
    } catch (error) {
      if (isApiAuthError(error)) return
      console.error('加载AI配置失败:', error)
      // 如果加载失败，使用默认值，不影响用户使用
      console.log('使用默认配置')
    }
  }, [authedFetch])

  // 加载 boss_config 的 enable_ai 字段
  const fetchEnableAi = useCallback(async () => {
    try {
      const response = await authedFetch('/api/boss/config', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const result = await response.json()
      const raw = result?.config?.enableAi
      const val = String(raw ?? '').trim().toLowerCase()
      setEnableAi(val === '1' || val === 'true' || val === 'on' ? 1 : Number(raw) === 1 ? 1 : 0)
    } catch (e) {
      if (isApiAuthError(e)) return
      console.error('加载enable_ai失败:', e)
    }
  }, [authedFetch])

  // 加载AI配置
  useEffect(() => {
    queueMicrotask(() => {
      void fetchAiConfig()
      void fetchEnableAi()
    })
  }, [fetchAiConfig, fetchEnableAi])

  // 切换 AI 开关并保存到 boss_config
  const toggleEnableAi = async () => {
    try {
      const next = enableAi ? 0 : 1
      setEnableAi(next)
      const response = await authedFetch('/api/boss/config', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ enableAi: next }),
      })
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      // 可选：校验返回体
      // const updated = await response.json()
    } catch (e) {
      if (isApiAuthError(e)) return
      console.error('更新enable_ai失败:', e)
      // 回滚
      setEnableAi((prev) => (prev ? 0 : 1))
      setFeedback({ success: false, title: '切换失败', message: 'Boss 自动投递 AI 开关切换失败，请检查后端服务连接。' })
    }
  }

  const handleSave = async () => {
    setLoading(true)
    try {
      // 保存AI配置
      const response = await authedFetch('/api/ai/config', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(aiConfig),
      })

      const result = await response.json()

      if (result.success) {
        setFeedback({ success: true, title: '保存成功', message: 'AI 技能介绍和提示词已保存。' })
      } else {
        setFeedback({ success: false, title: '保存失败', message: result.message || 'AI 配置保存失败。' })
      }
    } catch (error) {
      if (isApiAuthError(error)) return
      console.error('保存AI配置失败:', error)
      setFeedback({ success: false, title: '保存失败', message: '保存失败，请检查服务器连接。' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        icon={<BiBrain className="text-2xl" />}
        title="AI配置"
        subtitle="配置AI相关的技能介绍和提示词"
        iconClass="text-white"
        accentBgClass="bg-purple-500"
        actions={
          <Button
            onClick={handleSave}
            size="sm"
            className="px-3"
            type="button"
            disabled={loading}
          >
            <BiSave className="mr-1" /> 保存配置
          </Button>
        }
      />

      <div className="space-y-6">
        {/* AI配置 */}
        <Card className="animate-in fade-in slide-in-from-bottom-5 duration-700">
          <CardHeader className="flex items-start gap-4">
            <div className="min-w-0 space-y-2">
              <CardTitle className="flex items-center gap-2">
                <BiBrain className="text-primary" />
                AI配置
              </CardTitle>
              <CardDescription>配置全局 AI 技能资料；右侧开关仅控制 Boss 自动投递 AI 生成</CardDescription>
            </div>
            <div>
              <button
                type="button"
                aria-label="Boss 自动投递 AI 开关"
                onClick={toggleEnableAi}
                className={`relative inline-flex h-7 w-14 rounded-full border border-border transition-colors focus:outline-none focus:ring-2 focus:ring-ring/30 ${enableAi ? 'bg-primary hover:bg-primary/90' : 'bg-muted hover:bg-muted/80'}`}
              >
                <span
                  className={`absolute top-1 left-1 h-5 w-5 rounded-full bg-white shadow transition-transform ${enableAi ? 'translate-x-7' : 'translate-x-0'}`}
                />
              </button>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-6">
              <div className="rounded-md border border-border bg-muted/40 px-3 py-2 text-sm text-muted-foreground">
                Boss 自动投递 AI 开关：{enableAi ? '已开启' : '已关闭'}
              </div>
              <div className="space-y-2">
                <Label htmlFor="introduce">技能介绍</Label>
                <Textarea
                  id="introduce"
                  value={aiConfig.introduce}
                  onChange={(e) => setAiConfig({ ...aiConfig, introduce: e.target.value })}
                  placeholder="请输入您的技能介绍，例如：我熟练使用Java、Python等语言进行开发..."
                  className="min-h-[150px] resize-y"
                />
                <p className="text-xs text-muted-foreground">
                  详细描述您的技能、经验和专业背景，AI将使用这些信息生成个性化的求职文本
                </p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="prompt">AI提示词</Label>
                <Textarea
                  id="prompt"
                  value={aiConfig.prompt}
                  onChange={(e) => setAiConfig({ ...aiConfig, prompt: e.target.value })}
                  placeholder="请输入AI提示词模板，例如：我目前在找工作，%s，我期望的岗位方向是【%s】..."
                  className="min-h-[150px] resize-y"
                />
                <p className="text-xs text-muted-foreground">
                  AI使用的提示词模板，支持使用 %s 作为占位符，用于动态插入内容
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* ��用说明 */}
        <Card className="border-primary/20 bg-primary/5 animate-in fade-in slide-in-from-bottom-6 duration-700">
          <CardContent className="pt-6">
            <div className="flex gap-3">
              <BiInfoCircle className="h-5 w-5 text-primary flex-shrink-0 mt-0.5" />
              <div>
                <p className="text-sm text-foreground mb-2">
                  <strong className="font-semibold">使用说明：</strong>
                </p>
                <ul className="text-sm text-muted-foreground space-y-2">
                  <li className="flex items-start gap-2">
                    <span className="text-primary mt-0.5">•</span>
                    <span><strong>技能介绍：</strong>用于AI了解您的专业技能、工作经验和技术背景，是生成个性化内容的基础</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-primary mt-0.5">•</span>
                    <span><strong>AI提示词：</strong>定义AI生成内容的模板和风格，支持使用 <code className="bg-muted px-1 py-0.5 rounded text-xs">%s</code> 作为占位符</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-primary mt-0.5">•</span>
                    <span><strong>效果：</strong>配置保存后，AI将在自动投递时使用这些信息生成匹配度高的求职沟通内容</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-primary mt-0.5">•</span>
                    <span><strong>提示：</strong>建议定期更新技能介绍以反映最新的技能和经验，提高匹配成功率</span>
                  </li>
                </ul>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* 操作按钮（已迁移到右上角 PageHeader.actions，保持与环境配置一致） */}
      </div>
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
