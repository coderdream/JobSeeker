'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import { BiBarChart, BiRefresh } from 'react-icons/bi'
import { useAuthedRequest } from '@/components/auth/useAuthedRequest'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

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
    byCompany: NameValue[]
    byExperience: NameValue[]
    byDegree: NameValue[]
    salaryBuckets: BucketValue[]
    dailyTrend?: NameValue[]
  }
}

type YupaoJob = {
  id?: number
  jobId?: string
  companyName?: string
  jobTitle?: string
  salary?: string
  location?: string
  experience?: string
  degree?: string
  hrName?: string
  deliveryStatus?: string
  jobLink?: string
  createTime?: string
}

type PagedResult = {
  items: YupaoJob[]
  total: number
  page: number
  size: number
}

const statusOptions = ['未投递', '已投递', '已过滤', '投递失败']

function BarList({ items, labelKey = 'name' }: { items: Array<NameValue | BucketValue>; labelKey?: 'name' | 'bucket' }) {
  const max = Math.max(1, ...items.map((item) => item.value || 0))
  return (
    <div className="space-y-3">
      {items.map((item) => {
        const label = labelKey === 'bucket' ? (item as BucketValue).bucket : (item as NameValue).name
        const width = `${Math.max(4, Math.round(((item.value || 0) / max) * 100))}%`
        return (
          <div key={label} className="grid grid-cols-[96px_1fr_48px] items-center gap-3 text-sm">
            <span className="truncate text-muted-foreground">{label || '未知'}</span>
            <span className="h-2 rounded bg-muted">
              <span className="block h-2 rounded bg-emerald-600" style={{ width }} />
            </span>
            <span className="text-right font-medium">{item.value || 0}</span>
          </div>
        )
      })}
    </div>
  )
}

function formatDateOnly(value?: string) {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toISOString().slice(0, 10)
}

export default function AnalysisContent() {
  const { authedFetch } = useAuthedRequest()
  const [stats, setStats] = useState<StatsResponse | null>(null)
  const [items, setItems] = useState<YupaoJob[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [size] = useState(20)
  const [statuses, setStatuses] = useState<string[]>([])
  const [keyword, setKeyword] = useState('')
  const [location, setLocation] = useState('')
  const [loading, setLoading] = useState(false)

  const query = useMemo(() => {
    const params = new URLSearchParams()
    if (statuses.length) params.set('statuses', statuses.join(','))
    if (keyword) params.set('keyword', keyword)
    if (location) params.set('location', location)
    return params
  }, [keyword, location, statuses])

  const loadStats = useCallback(async () => {
    const params = new URLSearchParams(query)
    const response = await authedFetch(`/api/yupao/stats?${params.toString()}`)
    setStats(await response.json())
  }, [authedFetch, query])

  const loadList = useCallback(async (targetPage = page) => {
    const params = new URLSearchParams(query)
    params.set('page', String(targetPage))
    params.set('size', String(size))
    const response = await authedFetch(`/api/yupao/list?${params.toString()}`)
    const data: PagedResult = await response.json()
    setItems(data.items || [])
    setTotal(data.total || 0)
    setPage(data.page || targetPage)
  }, [authedFetch, page, query, size])

  const refresh = useCallback(async () => {
    setLoading(true)
    try {
      await Promise.all([loadStats(), loadList(1)])
    } finally {
      setLoading(false)
    }
  }, [loadList, loadStats])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void refresh()
  }, [refresh])

  const kpiCards = [
    { label: '总岗位', value: stats?.kpi.total ?? 0 },
    { label: '已投递', value: stats?.kpi.delivered ?? 0 },
    { label: '未投递', value: stats?.kpi.pending ?? 0 },
    { label: '投递失败', value: stats?.kpi.failed ?? 0 },
  ]

  return (
    <div className="space-y-5">
      <div className="grid grid-cols-2 gap-4 xl:grid-cols-4">
        {kpiCards.map((card) => (
          <Card key={card.label}>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm">{card.label}</CardTitle>
              <CardDescription className="text-2xl font-semibold text-foreground">{card.value}</CardDescription>
            </CardHeader>
          </Card>
        ))}
      </div>

      <Card>
        <CardHeader>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <CardTitle className="flex items-center gap-2 text-base"><BiBarChart /> 筛选</CardTitle>
            <Button onClick={refresh} disabled={loading} size="sm" variant="outline">
              <BiRefresh /> 刷新
            </Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap gap-2">
            {statusOptions.map((status) => (
              <button
                key={status}
                type="button"
                onClick={() => setStatuses((current) => (
                  current.includes(status) ? current.filter((item) => item !== status) : [...current, status]
                ))}
                className={`rounded-md border px-3 py-1.5 text-xs ${statuses.includes(status) ? 'border-emerald-600 bg-emerald-50 text-emerald-700' : 'border-border text-muted-foreground'}`}
              >
                {status}
              </button>
            ))}
          </div>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div className="space-y-2">
              <Label>关键词</Label>
              <Input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="公司、岗位或 HR" />
            </div>
            <div className="space-y-2">
              <Label>城市</Label>
              <Input value={location} onChange={(event) => setLocation(event.target.value)} placeholder="北京" />
            </div>
          </div>
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
        <Card>
          <CardHeader><CardTitle className="text-base">状态</CardTitle></CardHeader>
          <CardContent><BarList items={stats?.charts.byStatus || []} /></CardContent>
        </Card>
        <Card>
          <CardHeader><CardTitle className="text-base">城市</CardTitle></CardHeader>
          <CardContent><BarList items={stats?.charts.byCity || []} /></CardContent>
        </Card>
        <Card>
          <CardHeader><CardTitle className="text-base">公司</CardTitle></CardHeader>
          <CardContent><BarList items={stats?.charts.byCompany || []} /></CardContent>
        </Card>
        <Card>
          <CardHeader><CardTitle className="text-base">薪资</CardTitle></CardHeader>
          <CardContent><BarList items={stats?.charts.salaryBuckets || []} labelKey="bucket" /></CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">岗位列表</CardTitle>
          <CardDescription>共 {total} 条</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="border-b text-left text-muted-foreground">
                  <th className="px-3 py-2">公司</th>
                  <th className="px-3 py-2">岗位</th>
                  <th className="px-3 py-2">薪资</th>
                  <th className="px-3 py-2">城市</th>
                  <th className="px-3 py-2">状态</th>
                  <th className="px-3 py-2">时间</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item, index) => (
                  <tr key={`${item.jobId || item.id}-${index}`} className="border-b">
                    <td className="whitespace-nowrap px-3 py-2">{item.companyName || '-'}</td>
                    <td className="whitespace-nowrap px-3 py-2">
                      {item.jobLink ? (
                        <a className="text-primary hover:underline" href={item.jobLink} target="_blank" rel="noreferrer">
                          {item.jobTitle || '-'}
                        </a>
                      ) : item.jobTitle || '-'}
                    </td>
                    <td className="whitespace-nowrap px-3 py-2">{item.salary || '-'}</td>
                    <td className="whitespace-nowrap px-3 py-2">{item.location || '-'}</td>
                    <td className="whitespace-nowrap px-3 py-2">{item.deliveryStatus || '-'}</td>
                    <td className="whitespace-nowrap px-3 py-2">{formatDateOnly(item.createTime)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="mt-4 flex items-center gap-2">
            <Button size="sm" variant="outline" disabled={page <= 1} onClick={() => loadList(page - 1)}>上一页</Button>
            <span className="text-sm text-muted-foreground">第 {page} 页</span>
            <Button size="sm" variant="outline" disabled={page * size >= total} onClick={() => loadList(page + 1)}>下一页</Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
