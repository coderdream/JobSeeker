'use client'

import { useState, useEffect, useCallback } from 'react'
import { Filter, Plus, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useAuthedRequest } from '@/components/auth/useAuthedRequest'

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
  deliveryStatus?: string
  jobUrl?: string
  securityId?: string
}

type FilterField = 'keyword' | 'location' | 'experience' | 'degree' | 'salary'
type FilterOperator = 'contains' | 'equals' | 'notContains' | 'startsWith' | 'endsWith'
type JobFilter = { id: number; field: FilterField; operator: FilterOperator; value: string }
type SavedFilter = { id: number; filterName: string; filterConditions: string }

const filterFields: { value: FilterField; label: string }[] = [
  { value: 'keyword', label: '关键词（岗位/公司/HR）' },
  { value: 'location', label: '地点' },
  { value: 'experience', label: '经验' },
  { value: 'degree', label: '学历' },
  { value: 'salary', label: '薪资（K）' },
]
const filterOperators: { value: FilterOperator; label: string }[] = [
  { value: 'contains', label: '包含' },
  { value: 'equals', label: '等于' },
  { value: 'notContains', label: '不包含' },
  { value: 'startsWith', label: '开头是' },
  { value: 'endsWith', label: '结尾是' },
]

export default function JobList() {
  const { authedFetch } = useAuthedRequest()
  const [items, setItems] = useState<BossJob[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(20)
  
  // default to NOT showing Delivered or Discarded
  const [showDelivered, setShowDelivered] = useState(false)
  const [showDiscarded, setShowDiscarded] = useState(false)
  const [onlyMissingSecurityId, setOnlyMissingSecurityId] = useState(false)
  const [filterOpen, setFilterOpen] = useState(false)
  const [filters, setFilters] = useState<JobFilter[]>([])
  const [appliedFilters, setAppliedFilters] = useState<JobFilter[]>([])
  const [filterName, setFilterName] = useState('')
  const [savedFilters, setSavedFilters] = useState<SavedFilter[]>([])
  
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set())

  const loadSavedFilters = useCallback(async () => {
    try {
      const res = await authedFetch('/api/boss/filter-conditions')
      if (res.ok) setSavedFilters(await res.json())
    } catch (e) { console.error('load saved filters failed', e) }
  }, [authedFetch])

  useEffect(() => {
    // Synchronize the saved-filter list with the authenticated API.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadSavedFilters()
  }, [loadSavedFilters])

  const loadList = useCallback(async (toPage: number, toSize: number) => {
    const params = new URLSearchParams()
    
    const allStatuses = ["未投递", "已投递", "已过滤", "投递失败", "废弃"]
    const requestedStatuses = allStatuses.filter(s => {
      if (s === '已投递') return showDelivered;
      if (s === '废弃') return showDiscarded;
      return true;
    })
    params.set("statuses", requestedStatuses.join(","))

    // The list API has exact-match parameters for structured fields. Other
    // operators fall back to its cross-field keyword search until the API
    // exposes field-level operators.
    const textFilters = appliedFilters.filter(f => f.field === 'keyword' && f.value.trim())
    const location = appliedFilters.find(f => f.field === 'location' && f.operator === 'equals' && f.value.trim())
    const experience = appliedFilters.find(f => f.field === 'experience' && f.operator === 'equals' && f.value.trim())
    const degree = appliedFilters.find(f => f.field === 'degree' && f.operator === 'equals' && f.value.trim())
    const salary = appliedFilters.find(f => f.field === 'salary' && f.value.trim())
    if (textFilters.length) params.set('keyword', textFilters.map(f => f.value.trim()).join(' '))
    if (location) params.set('location', location.value.trim())
    if (experience) params.set('experience', experience.value.trim())
    if (degree) params.set('degree', degree.value.trim())
    if (salary) {
      const value = Number(salary.value)
      if (Number.isFinite(value)) {
        if (salary.operator === 'equals') { params.set('minK', String(value)); params.set('maxK', String(value)) }
        if (salary.operator === 'contains' || salary.operator === 'startsWith') params.set('minK', String(value))
        if (salary.operator === 'endsWith') params.set('maxK', String(value))
      }
    }
    params.set("page", String(toPage))
    params.set("size", String(toSize))

    try {
      const res = await authedFetch(`/api/boss/list?${params.toString()}`)
      const data = await res.json()
      setItems(data.items || [])
      setTotal(data.total || 0)
      setPage(data.page || toPage)
      setSize(data.size || toSize)
      setSelectedIds(new Set()) // clear selection on reload
    } catch (e) {
      console.error("fetch list failed", e)
    }
  }, [authedFetch, showDelivered, showDiscarded, appliedFilters])

  useEffect(() => {
    // The list is an external data source synchronized with the current query state.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadList(page, size)
  }, [loadList, page, size])

  const handleDiscard = async (id: number) => {
    if (!confirm("确定要废弃该岗位吗？")) return
    try {
      const res = await authedFetch(`/api/boss/jobs/${id}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: '废弃' })
      })
      const data = await res.json()
      if (data.success) {
        setItems(prev => prev.map(it => it.id === id ? { ...it, deliveryStatus: '废弃' } : it))
      }
    } catch (e) {
      console.error(e)
    }
  }

  const handleBatchDiscard = async () => {
    if (selectedIds.size === 0) return alert("请先选择岗位")
    if (!confirm(`确定要废弃选中的 ${selectedIds.size} 个岗位吗？`)) return
    
    let successCount = 0
    for (const id of selectedIds) {
      try {
        const res = await authedFetch(`/api/boss/jobs/${id}/status`, {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ status: '废弃' })
        })
        const data = await res.json()
        if (data.success) {
          setItems(prev => prev.map(it => it.id === id ? { ...it, deliveryStatus: '废弃' } : it))
          successCount++
        }
      } catch (e) {
        console.error(e)
      }
    }
    alert(`批量废弃完成，成功 ${successCount} 个`)
    setSelectedIds(new Set())
  }

  const handleApply = async (ids: number[]) => {
    if (ids.length === 0) return alert("请先选择岗位")
    try {
      const res = await authedFetch('/api/boss/apply', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ jobIds: ids })
      })
      const data = await res.json()
      if (data.success) {
        alert("投递任务已启动！")
        loadList(page, size)
      } else {
        alert(data.message || "启动投递失败")
      }
    } catch (e) {
      console.error(e)
    }
  }
  
  const handleViewDetail = async (ids: number[]) => {
    if (ids.length === 0) return alert("请先选择岗位")
    try {
      const res = await authedFetch('/api/boss/fetch-details', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ jobIds: ids })
      })
      const data = await res.json()
      if (data.success) {
        alert("已启动批量获取详情任务，请查看日志！")
      } else {
        alert(data.message || "启动获取详情失败")
      }
    } catch (e) {
      console.error(e)
    }
  }

  const toggleSelectAll = (checked: boolean) => {
    if (checked) {
                      setSelectedIds(new Set(visibleItems.map(it => it.id)))
    } else {
      setSelectedIds(new Set())
    }
  }

  const toggleSelect = (id: number, checked: boolean) => {
    const newSet = new Set(selectedIds)
    if (checked) newSet.add(id)
    else newSet.delete(id)
    setSelectedIds(newSet)
  }

  const applyFilters = () => {
    setAppliedFilters(filters.filter(f => f.value.trim()))
    setPage(1)
  }

  const saveFilter = async () => {
    const conditions = filters.filter(f => f.value.trim())
    if (!filterName.trim()) return alert('请输入筛选条件名')
    if (conditions.length === 0) return alert('请先添加筛选条件')
    try {
      const res = await authedFetch('/api/boss/filter-conditions', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ filterName: filterName.trim(), filterConditions: JSON.stringify(conditions) })
      })
      if (!res.ok) throw new Error('save failed')
      setFilterName('')
      await loadSavedFilters()
      alert('筛选条件已保存')
    } catch (e) { console.error(e); alert('保存筛选条件失败') }
  }

  const applySavedFilter = (saved: SavedFilter) => {
    try {
      const conditions = JSON.parse(saved.filterConditions) as JobFilter[]
      if (!Array.isArray(conditions)) throw new Error('invalid conditions')
      setFilters(conditions)
      setAppliedFilters(conditions.filter(f => f.value.trim()))
      setFilterName(saved.filterName)
      setFilterOpen(true)
      setPage(1)
    } catch (e) { console.error(e); alert('筛选条件格式无效') }
  }

  const clearFilters = () => {
    setFilters([])
    setAppliedFilters([])
    setPage(1)
  }

  const toggleFilterPanel = () => {
    setFilterOpen(open => {
      if (!open && filters.length === 0) setFilters([{ id: Date.now(), field: 'keyword', operator: 'contains', value: '' }])
      return !open
    })
  }

  const matchesFilter = (job: BossJob, filter: JobFilter) => {
    const raw = filter.field === 'keyword'
      ? [job.jobName, job.companyName, job.hrName].filter(Boolean).join(' ')
      : filter.field === 'salary' ? job.salary : job[filter.field]
    const actual = String(raw ?? '').trim().toLowerCase()
    const expected = filter.value.trim().toLowerCase()
    if (!expected || filter.field === 'salary') return true
    if (filter.operator === 'equals') return actual === expected
    if (filter.operator === 'notContains') return !actual.includes(expected)
    if (filter.operator === 'startsWith') return actual.startsWith(expected)
    if (filter.operator === 'endsWith') return actual.endsWith(expected)
    return actual.includes(expected)
  }

  const visibleItems = items.filter(job => {
    if (onlyMissingSecurityId && job.securityId?.trim()) return false
    return appliedFilters.every(filter => matchesFilter(job, filter))
  })

  return (
    <Card className="mt-4">
      <CardHeader>
        <CardTitle className="text-base flex justify-between items-center">
          <span className="flex items-center gap-2">职位列表 <Button size="sm" variant={filterOpen ? 'default' : 'outline'} onClick={toggleFilterPanel}><Filter /> 筛选</Button></span>
          <div className="flex gap-4 text-sm font-normal">
            <label className="flex items-center gap-1 cursor-pointer">
              <input type="checkbox" checked={showDelivered} onChange={e => { setShowDelivered(e.target.checked); setPage(1); }} /> 
              显示已投递
            </label>
            <label className="flex items-center gap-1 cursor-pointer">
              <input type="checkbox" checked={showDiscarded} onChange={e => { setShowDiscarded(e.target.checked); setPage(1); }} /> 
              显示已废弃
            </label>
            <label className="flex items-center gap-1 cursor-pointer">
              <input type="checkbox" checked={onlyMissingSecurityId} onChange={e => { setOnlyMissingSecurityId(e.target.checked); setSelectedIds(new Set()) }} />
              仅显示缺少 securityId
            </label>
          </div>
        </CardTitle>
      </CardHeader>
      <CardContent>
        {filterOpen && (
          <div className="mb-4 rounded-md border bg-slate-50 p-3 dark:bg-slate-900">
            <div className="mb-2 flex items-center justify-between text-sm font-semibold"><span>筛选条件</span><span className="text-xs font-normal text-muted-foreground">多个条件同时满足</span></div>
            <div className="space-y-2">
              {filters.map((filter, index) => (
                <div key={filter.id} className="flex flex-wrap items-center gap-2">
                  <select value={filter.field} onChange={e => setFilters(prev => prev.map(f => f.id === filter.id ? { ...f, field: e.target.value as FilterField } : f))} className="h-9 min-w-[170px] rounded-md border bg-background px-2 text-sm">{filterFields.map(f => <option key={f.value} value={f.value}>{f.label}</option>)}</select>
                  <select value={filter.operator} onChange={e => setFilters(prev => prev.map(f => f.id === filter.id ? { ...f, operator: e.target.value as FilterOperator } : f))} className="h-9 min-w-[100px] rounded-md border bg-background px-2 text-sm">{filterOperators.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}</select>
                  <input value={filter.value} onChange={e => setFilters(prev => prev.map(f => f.id === filter.id ? { ...f, value: e.target.value } : f))} placeholder={filter.field === 'salary' ? '例如 15' : '输入筛选值'} className="h-9 min-w-[180px] flex-1 rounded-md border bg-background px-3 text-sm" />
                  <Button size="icon" variant="ghost" title="删除条件" onClick={() => setFilters(prev => prev.filter(f => f.id !== filter.id))}><X /></Button>
                  {index < filters.length - 1 && <span className="basis-full pl-2 text-xs text-muted-foreground">并且</span>}
                </div>
              ))}
            </div>
            <div className="mt-3 flex gap-2">
              <Button size="sm" variant="outline" onClick={() => setFilters(prev => [...prev, { id: Date.now(), field: 'keyword', operator: 'contains', value: '' }])}><Plus /> 添加条件</Button>
              <Button size="sm" onClick={applyFilters}>应用筛选</Button>
              <Button size="sm" variant="ghost" onClick={clearFilters}>清空</Button>
              <input value={filterName} onChange={e => setFilterName(e.target.value)} placeholder="筛选条件名" className="h-9 min-w-[160px] rounded-md border bg-background px-3 text-sm" />
              <Button size="sm" variant="outline" onClick={saveFilter}>保存为筛选条件</Button>
            </div>
          </div>
        )}
        <div className="mb-4 flex items-center gap-2 text-sm">
          <span className="font-semibold">筛选列表</span>
          {savedFilters.length === 0 ? <span className="text-muted-foreground">暂无保存的筛选条件</span> : savedFilters.map(saved => (
            <Button key={saved.id} size="sm" variant={saved.filterName === filterName ? 'default' : 'outline'} onClick={() => applySavedFilter(saved)}>{saved.filterName}</Button>
          ))}
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left border-collapse">
            <thead>
              <tr className="border-b">
                <th className="p-2 whitespace-nowrap w-8">
                  <input 
                    type="checkbox" 
                    checked={visibleItems.length > 0 && selectedIds.size === visibleItems.length}
                    ref={input => {
                      if (input) {
                        input.indeterminate = selectedIds.size > 0 && selectedIds.size < visibleItems.length
                      }
                    }}
                    onChange={e => toggleSelectAll(e.target.checked)}
                  />
                </th>
                <th className="p-2 whitespace-nowrap">岗位名称</th>
                <th className="p-2 whitespace-nowrap">薪资</th>
                <th className="p-2 whitespace-nowrap">地点</th>
                <th className="p-2 whitespace-nowrap">公司名称</th>
                <th className="p-2 whitespace-nowrap">经验</th>
                <th className="p-2 whitespace-nowrap">学历</th>
                <th className="p-2 whitespace-nowrap">HR</th>
                <th className="p-2 whitespace-nowrap">投递状态</th>
                <th className="p-2 text-right whitespace-nowrap">
                  <div className="flex justify-end gap-2">
                    <Button size="sm" variant="outline" onClick={handleBatchDiscard} disabled={selectedIds.size === 0}>一键废弃</Button>
                    <Button size="sm" variant="outline" onClick={() => handleViewDetail(Array.from(selectedIds))} disabled={selectedIds.size === 0}>一键详情</Button>
                    <Button size="sm" variant="default" onClick={() => handleApply(Array.from(selectedIds))} disabled={selectedIds.size === 0}>一键投递</Button>
                  </div>
                </th>
              </tr>
            </thead>
            <tbody>
              {visibleItems.map(job => (
                <tr key={job.id} className="border-b hover:bg-slate-50 dark:hover:bg-slate-800">
                  <td className="p-2">
                    <input 
                      type="checkbox" 
                      checked={selectedIds.has(job.id)}
                      onChange={e => toggleSelect(job.id, e.target.checked)}
                    />
                  </td>
                  <td className="p-2 max-w-[200px] truncate" title={job.jobName}>
                    {job.jobUrl ? <a href={job.jobUrl} target="_blank" className="text-blue-500 hover:underline">{job.jobName}</a> : job.jobName}
                  </td>
                  <td className="p-2 text-orange-600 whitespace-nowrap">{job.salary}</td>
                  <td className="p-2 max-w-[150px] truncate" title={job.location}>{job.location}</td>
                  <td className="p-2 max-w-[200px] truncate" title={job.companyName}>{job.companyName}</td>
                  <td className="p-2 whitespace-nowrap">{job.experience}</td>
                  <td className="p-2 whitespace-nowrap">{job.degree}</td>
                  <td className="p-2 max-w-[150px] truncate" title={`${job.hrName} - ${job.hrPosition}`}>
                    {job.hrName} {job.hrPosition ? `(${job.hrPosition})` : ''}
                  </td>
                  <td className="p-2 whitespace-nowrap">
                    <span className={`px-2 py-1 rounded-full text-xs ${job.deliveryStatus === '已投递' ? 'bg-green-100 text-green-700' : job.deliveryStatus === '废弃' ? 'bg-gray-100 text-gray-700' : 'bg-blue-100 text-blue-700'}`}>
                      {job.deliveryStatus || '未投递'}
                    </span>
                  </td>
                  <td className="p-2 text-right space-x-2 whitespace-nowrap">
                    <Button size="sm" variant="outline" onClick={() => handleDiscard(job.id)}>废弃</Button>
                    <Button size="sm" variant="outline" onClick={() => handleViewDetail([job.id])}>详情</Button>
                    <Button size="sm" variant="default" onClick={() => handleApply([job.id])}>投递</Button>
                  </td>
                </tr>
              ))}
              {visibleItems.length === 0 && (
                <tr>
                  <td colSpan={10} className="p-4 text-center text-gray-500">暂无数据</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <div className="mt-4 flex justify-between items-center text-sm">
          <div>共 {total} 条，已选 {selectedIds.size} 条</div>
          <div className="flex gap-2 items-center">
            <select value={size} onChange={e => { setSize(Number(e.target.value)); setPage(1); }} className="border rounded p-1">
              {[5, 20, 50, 100, 1000].map(s => <option key={s} value={s}>{s} 条/页</option>)}
            </select>
            <div className="flex gap-1">
              <Button size="sm" variant="outline" disabled={page <= 1} onClick={() => setPage(p => p - 1)}>上一页</Button>
              <span className="px-3 py-1 bg-gray-100 rounded border flex items-center justify-center min-w-[32px]">{page}</span>
              <Button size="sm" variant="outline" disabled={page * size >= total || total === 0} onClick={() => setPage(p => p + 1)}>下一页</Button>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
