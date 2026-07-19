'use client'

import { useState, useEffect, useCallback } from 'react'
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
}

export default function JobList() {
  const { authedFetch } = useAuthedRequest()
  const [items, setItems] = useState<BossJob[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(20)
  
  // default to NOT showing Delivered or Discarded
  const [showDelivered, setShowDelivered] = useState(false)
  const [showDiscarded, setShowDiscarded] = useState(false)
  
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set())

  const loadList = useCallback(async (toPage: number, toSize: number) => {
    const params = new URLSearchParams()
    
    const statuses = []
    if (showDelivered) statuses.push('已投递')
    if (showDiscarded) statuses.push('废弃')
    
    const allStatuses = ["未投递", "已投递", "已过滤", "投递失败", "废弃"]
    const requestedStatuses = allStatuses.filter(s => {
      if (s === '已投递') return showDelivered;
      if (s === '废弃') return showDiscarded;
      return true;
    })
    params.set("statuses", requestedStatuses.join(","))

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
  }, [authedFetch, showDelivered, showDiscarded])

  useEffect(() => {
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
      setSelectedIds(new Set(items.map(it => it.id)))
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

  return (
    <Card className="mt-4">
      <CardHeader>
        <CardTitle className="text-base flex justify-between items-center">
          职位列表
          <div className="flex gap-4 text-sm font-normal">
            <label className="flex items-center gap-1 cursor-pointer">
              <input type="checkbox" checked={showDelivered} onChange={e => { setShowDelivered(e.target.checked); setPage(1); }} /> 
              显示已投递
            </label>
            <label className="flex items-center gap-1 cursor-pointer">
              <input type="checkbox" checked={showDiscarded} onChange={e => { setShowDiscarded(e.target.checked); setPage(1); }} /> 
              显示已废弃
            </label>
          </div>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left border-collapse">
            <thead>
              <tr className="border-b">
                <th className="p-2 whitespace-nowrap w-8">
                  <input 
                    type="checkbox" 
                    checked={items.length > 0 && selectedIds.size === items.length}
                    ref={input => {
                      if (input) {
                        input.indeterminate = selectedIds.size > 0 && selectedIds.size < items.length
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
              {items.map(job => (
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
              {items.length === 0 && (
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
