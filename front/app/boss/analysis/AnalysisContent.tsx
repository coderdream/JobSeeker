"use client"

import { useCallback, useEffect, useMemo, useRef, useState } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select } from "@/components/ui/select"
import { Label } from "@/components/ui/label"
import PageHeader from "@/app/components/PageHeader"
import { BiRefresh, BiDownload, BiBarChart, BiLineChart, BiPieChart, BiBriefcase } from "react-icons/bi"
import { useAuthedRequest } from "@/components/auth/useAuthedRequest"

type NameValue = { name: string; value: number }
type BucketValue = { bucket: string; value: number }

type StatsResponse = {
  kpi: {
    total: number
    delivered: number
    pending: number
    filtered: number
    failed: number
    avgMonthlyK?: number | null
  }
  charts: {
    byStatus: NameValue[]
    byCity: NameValue[]
    byIndustry: NameValue[]
    byCompany: NameValue[]
    byExperience: NameValue[]
    byDegree: NameValue[]
    salaryBuckets: BucketValue[]
    dailyTrend: NameValue[]
    hrActivity: NameValue[]
  }
}

type BossJob = {
  id: number
  companyName?: string
  jobName?: string
  salary?: string
  location?: string
  experience?: string
  degree?: string
  hrName?: string
  hrPosition?: string
  hrActiveStatus?: string
  deliveryStatus?: string
  jobUrl?: string
  recruitmentStatus?: string
  companyAddress?: string
  industry?: string
  introduce?: string
  financingStage?: string
  companyScale?: string
  jobDescription?: string
  createdAt?: string
}

type PagedResult = {
  items: BossJob[]
  total: number
  page: number
  size: number
}

type ChartDatasetConfig = {
  label: string
  data: number[]
  backgroundColor: string | string[]
  borderColor?: string | string[]
  fill?: boolean
  pointBackgroundColor?: string
  pointBorderColor?: string
}
type ChartConfig = {
  type: "pie" | "bar" | "line"
  data: {
    labels: string[]
    datasets: ChartDatasetConfig[]
  }
  options: {
    responsive: boolean
    maintainAspectRatio: boolean
    plugins: {
      legend: { display: boolean }
      title: { display: boolean; text?: string }
    }
    scales?: {
      x: { ticks: { autoSkip: boolean } }
      y: { beginAtZero: boolean }
    }
  }
}
type ChartInstance = { destroy: () => void }
type ChartConstructor = new (ctx: CanvasRenderingContext2D, config: ChartConfig) => ChartInstance
type ChartWindow = Window & { Chart?: ChartConstructor }

// 通用分类颜色（用于柱状/饼状图每个分类不同颜色）
const CATEGORY_COLORS = [
  "#3b82f6",
  "#10b981",
  "#f59e0b",
  "#ef4444",
  "#6366f1",
  "#22c55e",
  "#fb7185",
  "#a78bfa",
  "#f97316",
  "#06b6d4",
  "#4ade80",
  "#2dd4bf",
  "#f472b6",
  "#64748b",
]

function ChartCanvas({
  type,
  labels,
  data,
  title,
  color = "#3b82f6",
  colors,
}: {
  type: "pie" | "bar" | "line"
  labels: string[]
  data: number[]
  title?: string
  color?: string
  colors?: string[]
}) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const chartRef = useRef<ChartInstance | null>(null)
  // 颜色统一使用纯色（不透明）
  const toSolid = (hex: string) => hex

  async function ensureChart(): Promise<ChartConstructor> {
    const currentWindow = window as ChartWindow
    if (currentWindow.Chart) return currentWindow.Chart
    return new Promise((resolve, reject) => {
      const existing = document.querySelector("script[data-chartjs-cdn='true']") as HTMLScriptElement | null
      if (existing) {
        existing.addEventListener("load", () => {
          const loadedChart = currentWindow.Chart
          if (loadedChart) resolve(loadedChart)
          else reject(new Error("Chart.js loaded without Chart constructor"))
        })
        existing.addEventListener("error", () => reject(new Error("Chart.js CDN load error")))
        return
      }
      const script = document.createElement("script")
      script.src = "https://cdn.jsdelivr.net/npm/chart.js@4.4.4/dist/chart.umd.min.js"
      script.async = true
      script.setAttribute("data-chartjs-cdn", "true")
      script.addEventListener("load", () => {
        const loadedChart = currentWindow.Chart
        if (loadedChart) resolve(loadedChart)
        else reject(new Error("Chart.js loaded without Chart constructor"))
      })
      script.addEventListener("error", () => reject(new Error("Chart.js CDN load error")))
      document.head.appendChild(script)
    })
  }

  useEffect(() => {
    const ctx = canvasRef.current?.getContext("2d")
    if (!ctx) return

    // 销毁旧图表
    if (chartRef.current) {
      chartRef.current.destroy()
      chartRef.current = null
    }

    let cancelled = false

    const pieColorsBase = [
      "#3b82f6",
      "#10b981",
      "#f59e0b",
      "#ef4444",
      "#6366f1",
      "#22c55e",
      "#fb7185",
      "#a78bfa",
      "#f97316",
      "#06b6d4",
    ]

    const backgroundColor = (() => {
      if (type === "pie") {
        const arr = (colors && colors.length ? colors : pieColorsBase).slice(0, labels.length)
        return arr
      }
      if (type === "bar" && colors && colors.length) {
        // 柱状图每个分类使用纯色
        return colors.slice(0, data.length).map((c) => toSolid(c))
      }
      // 折线图/默认均使用纯色
      return toSolid(color ?? "#3b82f6")
    })()

    const borderColor = (() => {
      if (type === "pie") {
        // 饼图无需边框或统一边框
        return undefined
      }
      if (type === "bar" && colors && colors.length) {
        return colors.slice(0, data.length)
      }
      return color
    })()

    const dataset: ChartDatasetConfig = {
      label: title || "",
      data,
      backgroundColor,
      borderColor,
    }

    // 线形图不使用区域填充，点与线均为纯色
    if (type === "line") {
      dataset.fill = false
      dataset.pointBackgroundColor = toSolid(color)
      dataset.pointBorderColor = toSolid(color)
    }

    ;(async () => {
      try {
        const Chart = await ensureChart()
        // 检查组件是否已卸载
        if (cancelled) return

        chartRef.current = new Chart(ctx, {
          type,
          data: {
            labels,
            datasets: [dataset],
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: { display: type === "pie" },
              title: { display: !!title, text: title },
            },
            scales: type !== "pie" ? { x: { ticks: { autoSkip: true } }, y: { beginAtZero: true } } : undefined,
          },
        })
      } catch (error) {
        console.error("Failed to create chart:", error)
      }
    })()

    return () => {
      cancelled = true
      if (chartRef.current) {
        chartRef.current.destroy()
        chartRef.current = null
      }
    }
  }, [type, labels, data, title, color, colors])

  return <canvas ref={canvasRef} className="w-full h-64" />
}

export default function AnalysisContent({ showHeader = false }: { showHeader?: boolean }) {
  const { authedFetch } = useAuthedRequest()
  const [stats, setStats] = useState<StatsResponse | null>(null)
  const [, setLoadingStats] = useState(true)

  const [items, setItems] = useState<BossJob[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(20)
  // 分页输入（页码与每页条数），便于自定义跳转与控制
  const [inputPage, setInputPage] = useState<number | string>(1)
  const [inputSize, setInputSize] = useState<number | string>(20)

  const [statuses, setStatuses] = useState<string[]>([]) // 默认不勾选任何状态
  const [location, setLocation] = useState<string>("")
  const [experience, setExperience] = useState<string>("")
  const [degree, setDegree] = useState<string>("")
  const [minK, setMinK] = useState<string>("")
  const [maxK, setMaxK] = useState<string>("")
  const [keyword, setKeyword] = useState<string>("")
  const [loadingList, setLoadingList] = useState(false)
  const [reloading, setReloading] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [filterHeadhunter, setFilterHeadhunter] = useState<boolean>(false)

  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [applying, setApplying] = useState(false)
  const [discardingId, setDiscardingId] = useState<number | null>(null)

  const handleSelectAll = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.checked) {
      setSelectedIds(items.map(it => it.id))
    } else {
      setSelectedIds([])
    }
  }

  const handleSelect = (id: number, checked: boolean) => {
    if (checked) {
      setSelectedIds(prev => [...prev, id])
    } else {
      setSelectedIds(prev => prev.filter(v => v !== id))
    }
  }

  const handleBatchApply = async () => {
    if (selectedIds.length === 0) {
      alert("请先勾选岗位")
      return
    }
    if (!confirm(`确定要一键投递选中的 ${selectedIds.length} 个岗位吗？`)) return
    try {
      setApplying(true)
      const res = await authedFetch('/api/boss/apply', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ jobIds: selectedIds })
      })
      const data = await res.json()
      if (data.success) {
        alert("投递任务已启动！")
        setSelectedIds([]) // 清空选中
        await loadList(page, size)
      } else {
        alert(data.message || "启动投递失败")
      }
    } catch (e) {
      console.error(e)
      alert("请求异常")
    } finally {
      setApplying(false)
    }
  }

  const handleDiscard = async (id: number) => {
    if (!confirm("确定要废弃该岗位吗？")) return
    try {
      setDiscardingId(id)
      const res = await authedFetch(`/api/boss/jobs/${id}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: '废弃' })
      })
      const data = await res.json()
      if (data.success) {
        setItems(prev => prev.map(it => it.id === id ? { ...it, deliveryStatus: '废弃' } : it))
      } else {
        alert(data.message || "废弃失败")
      }
    } catch (e) {
      console.error(e)
      alert("请求异常")
    } finally {
      setDiscardingId(null)
    }
  }

  // 查看全文弹窗
  const [showTextDialog, setShowTextDialog] = useState(false)
  const [textDialogTitle, setTextDialogTitle] = useState<string>("")
  const [textDialogContent, setTextDialogContent] = useState<string>("")
  const textAreaRef = useRef<HTMLTextAreaElement | null>(null)

  const openTextDialog = (title: string, content?: string) => {
    setTextDialogTitle(title)
    setTextDialogContent(content || "")
    setShowTextDialog(true)
  }

  const selectDialogText = () => {
    const el = textAreaRef.current
    if (el) el.select()
  }

  const copyDialogText = async () => {
    try {
      await navigator.clipboard.writeText(textDialogContent || "")
      alert("已复制到剪贴板")
    } catch {
      try {
        const ta = document.createElement("textarea")
        ta.value = textDialogContent || ""
        document.body.appendChild(ta)
        ta.select()
        document.execCommand("copy")
        document.body.removeChild(ta)
        alert("已复制到剪贴板")
      } catch {
        alert("复制失败，请手动选中复制")
      }
    }
  }

  const statusOptions = ["未投递", "已投递", "已过滤", "投递失败"]

  // 仅显示日期（YYYY-MM-DD）
  const formatDateOnly = (s?: string) => {
    if (!s) return ""
    const d = new Date(s)
    if (!isNaN(d.getTime())) {
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, "0")
      const day = String(d.getDate()).padStart(2, "0")
      return `${y}-${m}-${day}`
    }
    // 非标准时间串，兜底截取前10位
    return s.slice(0, 10)
  }

  const loadList = useCallback(async (toPage: number, toSize: number) => {
    const params = new URLSearchParams()
    if (statuses.length) params.set("statuses", statuses.join(","))
    if (location) params.set("location", location)
    if (experience) params.set("experience", experience)
    if (degree) params.set("degree", degree)
    if (minK) params.set("minK", String(Number(minK)))
    if (maxK) params.set("maxK", String(Number(maxK)))
    if (keyword) params.set("keyword", keyword)
    if (filterHeadhunter) params.set("filterHeadhunter", "true")
    params.set("page", String(toPage))
    params.set("size", String(toSize))

    try {
      setLoadingList(true)
      const res = await authedFetch(`/api/boss/list?${params.toString()}`)
      const data: PagedResult = await res.json()
      // 前端兜底过滤猎头（避免后端未更新导致的显示异常）
      const filteredItems = (data.items || []).filter(it => {
        if (!filterHeadhunter) return true
        const hp = (it.hrPosition || "").toLowerCase()
        return !(hp.includes("猎头") || hp.includes("獵頭"))
      })
      const nextPage = data.page || toPage
      const nextSize = data.size || toSize
      setItems(filteredItems)
      setTotal(data.total || 0)
      setPage(nextPage)
      setSize(nextSize)
      setInputPage(nextPage)
      setInputSize(nextSize)
    } catch (e) {
      console.error("fetch list failed", e)
    } finally {
      setLoadingList(false)
    }
  }, [authedFetch, degree, experience, filterHeadhunter, keyword, location, maxK, minK, statuses])

  // 统计图加载：与列表共享相同筛选条件
  const loadStats = useCallback(async () => {
    const params = new URLSearchParams()
    if (statuses.length) params.set("statuses", statuses.join(","))
    if (location) params.set("location", location)
    if (experience) params.set("experience", experience)
    if (degree) params.set("degree", degree)
    if (minK) params.set("minK", String(Number(minK)))
    if (maxK) params.set("maxK", String(Number(maxK)))
    if (keyword) params.set("keyword", keyword)
    if (filterHeadhunter) params.set("filterHeadhunter", "true")

    try {
      setLoadingStats(true)
      const res = await authedFetch(`/api/boss/stats?${params.toString()}`)
      const data: StatsResponse = await res.json()
      setStats(data)
    } catch (e) {
      console.error("fetch stats failed", e)
    } finally {
      setLoadingStats(false)
    }
  }, [authedFetch, degree, experience, filterHeadhunter, keyword, location, maxK, minK, statuses])

  useEffect(() => {
    queueMicrotask(() => void loadList(1, size))
  }, [loadList, size])

  useEffect(() => {
    queueMicrotask(() => void loadStats())
  }, [loadStats])

  const onReload = async () => {
    try {
      setReloading(true)
      const res = await authedFetch('/api/boss/reload')
      const data = await res.json()
      console.log("reload", data)
      await loadList(1, size)
      await loadStats()
    } catch (e) {
      console.error("reload failed", e)
    } finally {
      setReloading(false)
    }
  }

  const exportCSV = async () => {
    try {
      setExporting(true)
      // 组装当前筛选条件
      const baseParams = new URLSearchParams()
      if (statuses.length) baseParams.set("statuses", statuses.join(","))
      if (location) baseParams.set("location", location)
      if (experience) baseParams.set("experience", experience)
      if (degree) baseParams.set("degree", degree)
      if (minK) baseParams.set("minK", String(Number(minK)))
      if (maxK) baseParams.set("maxK", String(Number(maxK)))
      if (keyword) baseParams.set("keyword", keyword)
      if (filterHeadhunter) baseParams.set("filterHeadhunter", "true")

      // 分页抓取，直到获取全部数据
      const pageSize = 1000
      let currentPage = 1
      let all: BossJob[] = []
      let totalCount = 0

      while (true) {
        const params = new URLSearchParams(baseParams)
        params.set("page", String(currentPage))
        params.set("size", String(pageSize))
        const res = await authedFetch(`/api/boss/list?${params.toString()}`)
        const data: PagedResult = await res.json()
        let chunk = data.items || []
        // 导出也做兜底过滤，确保CSV不含猎头岗位
        if (filterHeadhunter) {
          chunk = chunk.filter(it => {
            const hp = (it.hrPosition || "").toLowerCase()
            return !(hp.includes("猎头") || hp.includes("獵頭"))
          })
        }
        if (currentPage === 1) totalCount = data.total || chunk.length
        all = all.concat(chunk)
        if (all.length >= totalCount || chunk.length === 0) break
        currentPage += 1
      }

      const header = [
        "公司名称",
        "岗位名称",
        "薪资",
        "工作地点",
        "经验",
        "学历",
        "HR",
        "投递状态",
        "链接",
        "创建时间",
      ]
      const rows = all.map((it) => [
        it.companyName || "",
        it.jobName || "",
        it.salary || "",
        it.location || "",
        it.experience || "",
        it.degree || "",
        it.hrName || "",
        it.deliveryStatus || "",
        it.jobUrl || "",
        it.createdAt || "",
      ])
      const csv = [header, ...rows]
        .map((r) => r.map((v) => (String(v).includes(",") ? `"${String(v).replace(/"/g, '""')}"` : String(v))).join(","))
        .join("\n")
      const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" })
      const url = URL.createObjectURL(blob)
      const a = document.createElement("a")
      a.href = url
      a.download = `boss_jobs_${new Date().toISOString().slice(0, 10)}.csv`
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      console.error("export CSV failed", e)
      alert("导出失败，请稍后重试")
    } finally {
      setExporting(false)
    }
  }

  // 彩色标签样式（用于状态类字段）
  const badgeClass = (kind: "delivery" | "hr" | "recruitment", value?: string) => {
    const base = "px-2 py-1 rounded-full text-xs font-medium whitespace-nowrap"
    const v = (value || "").trim()
    if (kind === "delivery") {
      if (v.includes("已投递")) return `${base} bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300`
      if (v.includes("已过滤")) return `${base} bg-pink-100 text-pink-700 dark:bg-pink-900/30 dark:text-pink-300`
      if (v.includes("失败")) return `${base} bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300`
      return `${base} bg-slate-100 text-slate-700 dark:bg-slate-800/50 dark:text-slate-300`
    }
    if (kind === "hr") {
      if (/刚|在线|今日/.test(v)) return `${base} bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300`
      if (/小时|近/.test(v)) return `${base} bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300`
      if (/天|周|月|很久/.test(v)) return `${base} bg-slate-100 text-slate-700 dark:bg-slate-800/50 dark:text-slate-300`
      return `${base} bg-slate-100 text-slate-700 dark:bg-slate-800/50 dark:text-slate-300`
    }
    // recruitment
    if (/暂停|关闭|下线|结束/.test(v)) return `${base} bg-gray-200 text-gray-800 dark:bg-gray-700/60 dark:text-gray-200`
    if (/急/.test(v)) return `${base} bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300`
    if (/招|招聘|中/.test(v)) return `${base} bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300`
    return `${base} bg-gray-100 text-gray-700 dark:bg-gray-700/50 dark:text-gray-200`
  }

  const kpiCards = useMemo(() => {
    const k = stats?.kpi
    return [
      { title: "总岗位数", value: k?.total ?? 0 },
      { title: "已投递", value: k?.delivered ?? 0 },
      { title: "未投递", value: k?.pending ?? 0 },
      { title: "已过滤", value: k?.filtered ?? 0 },
      { title: "投递失败", value: k?.failed ?? 0 },
      { title: "平均月薪(K)", value: k?.avgMonthlyK ?? 0 },
    ]
  }, [stats])

  return (
    <div className="min-w-0 max-w-full space-y-5 overflow-hidden">
      {showHeader && (
        <PageHeader
          title="Boss 投递分析"
          subtitle="基于 boss_data 表的统计图与列表分析"
          icon={<BiBarChart size={28} />}
        />
      )}

      {/* KPI 卡片 */}
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-3 2xl:grid-cols-6">
        {kpiCards.map((c, idx) => (
          <Card key={idx}>
            <CardHeader className="p-4">
              <CardTitle className="text-sm">{c.title}</CardTitle>
              <CardDescription className="text-lg font-semibold">{c.value}</CardDescription>
            </CardHeader>
          </Card>
        ))}
      </div>

      {/* 操作栏 */}
      <Card>
        <CardHeader className="space-y-0">
          <div className="flex min-w-0 flex-wrap items-start justify-between gap-3">
            <div className="min-w-0">
              <CardTitle className="text-base">筛选与操作</CardTitle>
              <CardDescription>按状态、地区、经验、学历与薪资区间过滤列表</CardDescription>
            </div>
            <div className="flex max-w-full flex-wrap gap-2 rounded-md border border-border bg-muted/40 p-2">
              {statusOptions.map((s) => (
                <label
                  key={s}
                  className={`group inline-flex items-center gap-2 rounded-md border px-3 py-1.5 text-sm transition-colors ${
                    statuses.includes(s)
                      ? "border-primary/40 bg-primary/10 text-primary"
                      : "border-border bg-background text-muted-foreground hover:bg-accent hover:text-foreground"
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={statuses.includes(s)}
                    onChange={(e) => {
                      setStatuses((prev) => {
                        if (e.target.checked) return Array.from(new Set([...prev, s]))
                        return prev.filter((x) => x !== s)
                      })
                    }}
                    className="sr-only peer"
                  />
                  <span className="inline-flex h-4 w-4 items-center justify-center rounded border border-input transition-colors peer-checked:border-primary peer-checked:bg-primary"></span>
                  {s}
                </label>
              ))}
              <label
                className={`group inline-flex items-center gap-2 rounded-md border px-3 py-1.5 text-sm transition-colors ${
                  filterHeadhunter
                    ? "border-primary/40 bg-primary/10 text-primary"
                    : "border-border bg-background text-muted-foreground hover:bg-accent hover:text-foreground"
                }`}
              >
                <input
                  type="checkbox"
                  checked={filterHeadhunter}
                  onChange={(e) => setFilterHeadhunter(e.target.checked)}
                  className="sr-only peer"
                />
                <span className="inline-flex h-4 w-4 items-center justify-center rounded border border-input transition-colors peer-checked:border-primary peer-checked:bg-primary"></span>
                过滤猎头岗位
              </label>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-6">
            <div>
              <Label>城市</Label>
              <Input value={location} onChange={(e) => setLocation(e.target.value)} placeholder="如：深圳" />
            </div>
            <div>
              <Label>经验</Label>
              <Input value={experience} onChange={(e) => setExperience(e.target.value)} placeholder="如：3-5年" />
            </div>
            <div>
              <Label>学历</Label>
              <Input value={degree} onChange={(e) => setDegree(e.target.value)} placeholder="如：本科" />
            </div>
            <div>
              <Label>最低月薪(K)</Label>
              <Input type="number" value={minK} onChange={(e) => setMinK(e.target.value)} placeholder="10" />
            </div>
            <div>
              <Label>最高月薪(K)</Label>
              <Input type="number" value={maxK} onChange={(e) => setMaxK(e.target.value)} placeholder="30" />
            </div>
            <div>
              <Label>关键词</Label>
              <Input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="公司/岗位/HR" />
            </div>
          </div>

          <div className="mt-4 flex flex-wrap gap-2">
            <Button
              onClick={async () => {
                await loadList(1, size)
                await loadStats()
              }}
              disabled={loadingList}
            >
              <BiBarChart className="mr-2" /> 应用筛选
            </Button>
            <Button variant="success" onClick={exportCSV} disabled={exporting}>
              <BiDownload className="mr-2" /> {exporting ? "导出中..." : "导出CSV"}
            </Button>
            <Button variant="outline" onClick={onReload} disabled={reloading}>
              <BiRefresh className="mr-2" /> 刷新数据
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* 图表区：6个图表（已移除每日趋势与HR活跃度） */}
      <div className="grid min-w-0 grid-cols-1 gap-4 2xl:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2"><BiPieChart /> 投递状态分布</CardTitle>
            <CardDescription>已投递/未投递/已过滤/失败等占比</CardDescription>
          </CardHeader>
          <CardContent>
            {stats ? (
              <ChartCanvas
                type="pie"
                labels={stats.charts.byStatus.map((x) => x.name)}
                data={stats.charts.byStatus.map((x) => x.value)}
              />
            ) : (
              <div className="h-64 flex items-center justify-center border-2 border-dashed rounded-lg text-muted-foreground">
                加载中...
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2"><BiBarChart /> 行业TOP10</CardTitle>
            <CardDescription>岗位按行业聚合</CardDescription>
          </CardHeader>
          <CardContent>
            {stats ? (
              <ChartCanvas
                type="bar"
                labels={stats.charts.byIndustry.map((x) => x.name)}
                data={stats.charts.byIndustry.map((x) => x.value)}
                colors={CATEGORY_COLORS}
              />
            ) : (
              <div className="h-64 flex items-center justify-center border-2 border-dashed rounded-lg text-muted-foreground">加载中...</div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2"><BiBarChart /> 公司岗位数TOP10</CardTitle>
            <CardDescription>按公司名称聚合</CardDescription>
          </CardHeader>
          <CardContent>
            {stats ? (
              <ChartCanvas type="bar" labels={stats.charts.byCompany.map((x) => x.name)} data={stats.charts.byCompany.map((x) => x.value)} colors={CATEGORY_COLORS} />
            ) : (
              <div className="h-64 flex items-center justify-center border-2 border-dashed rounded-lg text-muted-foreground">加载中...</div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2"><BiBarChart /> 经验分布</CardTitle>
            <CardDescription>不同经验要求的岗位数</CardDescription>
          </CardHeader>
          <CardContent>
            {stats ? (
              <ChartCanvas type="bar" labels={stats.charts.byExperience.map((x) => x.name)} data={stats.charts.byExperience.map((x) => x.value)} colors={CATEGORY_COLORS} />
            ) : (
              <div className="h-64 flex items-center justify-center border-2 border-dashed rounded-lg text-muted-foreground">加载中...</div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2"><BiBarChart /> 学历分布</CardTitle>
            <CardDescription>不同学历要求的岗位数</CardDescription>
          </CardHeader>
          <CardContent>
            {stats ? (
              <ChartCanvas type="bar" labels={stats.charts.byDegree.map((x) => x.name)} data={stats.charts.byDegree.map((x) => x.value)} colors={CATEGORY_COLORS} />
            ) : (
              <div className="h-64 flex items-center justify-center border-2 border-dashed rounded-lg text-muted-foreground">加载中...</div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2"><BiLineChart /> 薪资区间分布</CardTitle>
            <CardDescription>基于中位数K的桶聚合</CardDescription>
          </CardHeader>
          <CardContent>
            {stats ? (
              <ChartCanvas type="line" labels={stats.charts.salaryBuckets.map((x) => x.bucket)} data={stats.charts.salaryBuckets.map((x) => x.value)} color="#ef4444" />
            ) : (
              <div className="h-64 flex items-center justify-center border-2 border-dashed rounded-lg text-muted-foreground">加载中...</div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* 列表 */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-base flex items-center gap-2"><BiBriefcase /> 岗位数据</CardTitle>
              <CardDescription>支持筛选、导出与刷新</CardDescription>
            </div>
            <div className="flex items-center gap-2">
              {selectedIds.length > 0 && (
                <Button onClick={handleBatchApply} disabled={applying}>
                  {applying ? "启动中..." : `一键投递 (${selectedIds.length})`}
                </Button>
              )}
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="max-w-full overflow-x-auto rounded-lg border border-border shadow-sm">
            <table className="w-full table-fixed min-w-[600px] bg-white dark:bg-blacksection">
              <thead>
                <tr className="border-b border-border bg-muted/70">
                  <th className="w-12 px-4 py-3.5 text-center text-sm font-semibold text-gray-700 dark:text-gray-200 border-r border-gray-200 dark:border-gray-700">
                    <input type="checkbox" onChange={handleSelectAll} checked={items.length > 0 && selectedIds.length === items.length} />
                  </th>
                  <th className="px-4 py-3.5 text-left text-sm font-semibold text-gray-700 dark:text-gray-200 border-r border-gray-200 dark:border-gray-700">职位信息</th>
                  <th className="w-24 px-4 py-3.5 text-center text-sm font-semibold text-gray-700 dark:text-gray-200 border-r border-gray-200 dark:border-gray-700">投递状态</th>
                  <th className="w-24 px-4 py-3.5 text-center text-sm font-semibold text-gray-700 dark:text-gray-200 border-r border-gray-200 dark:border-gray-700">操作</th>
                </tr>
              </thead>
              <tbody>
                {items.length === 0 ? (
                  <tr>
                    <td colSpan={21} className="px-4 py-12 text-center text-muted-foreground bg-gray-50 dark:bg-gray-900/20">
                      <div className="flex flex-col items-center gap-3">
                        <BiBriefcase className="text-4xl text-gray-300 dark:text-gray-600" />
                        <p className="text-sm">暂无数据</p>
                      </div>
                    </td>
                  </tr>
                ) : (
                  items.map((it, idx) => (
                    <tr
                      key={it.id}
                      className={`group transition-colors border-b border-gray-200 dark:border-gray-700 last:border-b-0 ${
                        idx % 2 === 0
                          ? 'bg-white dark:bg-blacksection hover:bg-blue-50/50 dark:hover:bg-blue-950/20'
                          : 'bg-gray-50/50 dark:bg-gray-900/20 hover:bg-blue-50/50 dark:hover:bg-blue-950/20'
                      }`}
                    >
                      <td className="px-4 py-3 text-center align-top border-r border-gray-200 dark:border-gray-700">
                        <input type="checkbox" checked={selectedIds.includes(it.id)} onChange={e => handleSelect(it.id, e.target.checked)} />
                      </td>
                      <td className="px-4 py-3 text-sm leading-6 align-top border-r border-gray-200 dark:border-gray-700">
                        <div className="font-semibold text-primary">{it.jobName || '-'} <span className="text-red-500 mx-2">{it.salary || '-'}</span></div>
                        <div className="text-muted-foreground mt-1 text-xs">
                          {it.location || '-'} | {it.experience || '-'} | {it.degree || '-'}
                        </div>
                        <div className="text-muted-foreground mt-1 text-xs">
                          {it.companyName || '-'} | {it.companyScale || '-'} | {it.financingStage || '-'} | {it.industry || '-'}
                        </div>
                        <div className="text-muted-foreground mt-1 text-xs">
                          HR: {it.hrName || '-'} ({it.hrPosition || '-'}) | 活跃度: <span className="text-orange-500">{it.hrActiveStatus || '-'}</span>
                        </div>
                        <div className="flex gap-2 mt-2">
                           {it.jobUrl && <a href={it.jobUrl} target="_blank" rel="noreferrer" className="text-blue-500 hover:underline text-xs">职位详情</a>}
                           {it.companyAddress && <span className="text-gray-500 text-xs">地址: {it.companyAddress}</span>}
                        </div>
                      </td>
                      <td className="px-4 py-3 text-center whitespace-nowrap align-middle border-r border-gray-200 dark:border-gray-700">
                        <span className={badgeClass("delivery", it.deliveryStatus)} title={it.deliveryStatus}>{it.deliveryStatus || "-"}</span>
                      </td>
                      <td className="px-4 py-3 text-center align-middle border-l border-gray-200 dark:border-gray-700">
                        <div className="flex flex-col gap-2 items-center">
                          <Button variant="destructive" size="sm" className="w-20" onClick={() => handleDiscard(it.id)} disabled={discardingId === it.id}>
                            {discardingId === it.id ? "废弃中" : "废弃"}
                          </Button>
                          <Button variant="outline" size="sm" className="w-20 text-blue-600 border-blue-600 hover:bg-blue-50" onClick={() => {}}>
                            一键投递
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          <div className="mt-4 flex flex-wrap items-center gap-3">
            <Button variant="outline" onClick={() => loadList(Math.max(1, page - 1), size)} disabled={loadingList || page <= 1}>上一页</Button>
            <div className="text-sm">第 {page} 页 / 共 {Math.max(1, Math.ceil(total / size))} 页</div>
            <Button variant="outline" onClick={() => loadList(page + 1, size)} disabled={loadingList || page >= Math.ceil(total / size)}>下一页</Button>
            {/* 自定义页码与每页条数 */}
            <div className="flex items-center gap-2 sm:ml-4">
              <Label className="text-sm">页码</Label>
              <Input
                type="number"
                value={inputPage}
                onChange={(e) => setInputPage(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    const toPage = Math.max(1, Number(inputPage) || 1)
                    loadList(toPage, size)
                  }
                }}
                className="h-8 w-20"
              />
              <Label className="text-sm">每页</Label>
              <Select
                value={String(inputSize)}
                onChange={(e) => {
                  const v = Number(e.target.value)
                  setInputSize(v)
                  loadList(1, Math.max(1, v))
                }}
                className="h-8 w-28"
              >
                <option value="20">20</option>
                <option value="50">50</option>
                <option value="100">100</option>
                <option value="200">200</option>
              </Select>
              <span className="text-sm text-muted-foreground">条</span>
            </div>
            <div className="ml-auto text-sm text-muted-foreground">共 {total} 条</div>
          </div>
      </CardContent>
      </Card>

      {/* 查看全文弹框 */}
      {showTextDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30" role="dialog" aria-modal="true">
          <div className="w-[92%] max-w-3xl rounded-lg border border-border bg-background shadow-lg animate-in fade-in zoom-in-95">
            <Card className="border-0">
              <CardHeader className="pb-2">
                <CardTitle className="text-lg flex items-center gap-2">{textDialogTitle}</CardTitle>
              </CardHeader>
              <CardContent className="pt-0">
                <textarea
                  ref={textAreaRef}
                  readOnly
                  value={textDialogContent || ''}
                  className="w-full h-[50vh] text-sm leading-6 rounded-md border p-2 bg-muted/30 dark:bg-neutral-800"
                />
                <div className="flex justify-end gap-2 mt-4">
                  <Button variant="outline" onClick={selectDialogText} className="px-3">全选</Button>
                  <Button variant="success" onClick={copyDialogText} className="px-3">复制</Button>
                  <Button onClick={() => setShowTextDialog(false)} className="px-3">关闭</Button>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      )}
    </div>
  )
}
