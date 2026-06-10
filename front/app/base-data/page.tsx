'use client'

import { useCallback, useEffect, useMemo, useRef, useState, type Key, type ReactNode } from 'react'
import { BiData, BiRefresh, BiSave, BiTrash } from 'react-icons/bi'
import PageHeader from '@/app/components/PageHeader'
import { useAuthedRequest } from '@/components/auth/useAuthedRequest'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { ConfirmDialog, FeedbackDialog } from '@/components/workbench/feedback-dialog'

type PlatformKey = 'boss' | '51job' | 'liepin' | 'zhilian'
type OptionTypeMeta = { id?: number; value: string; label: string; sortOrder?: number; enabled?: number; fallback?: boolean }

interface City {
  id?: number
  name: string
  province?: string
  cityCode: string
  sortOrder?: number
  enabled?: number
}

interface CityPlatformCode {
  id?: number
  cityId?: number
  platform: PlatformKey
  platformCityCode: string
  platformCityName?: string
  enabled?: number
}

interface PlatformOption {
  id?: number
  platform: PlatformKey
  type: string
  name: string
  code: string
  sortOrder?: number
  enabled?: number
}

interface PlatformOptionType {
  id?: number
  platform: PlatformKey
  type: string
  label: string
  sortOrder?: number
  enabled?: number
}

const platforms: { value: PlatformKey; label: string }[] = [
  { value: 'boss', label: 'Boss直聘' },
  { value: '51job', label: '51job' },
  { value: 'liepin', label: '猎聘' },
  { value: 'zhilian', label: '智联招聘' },
]

function getDefaultOptionType(types: OptionTypeMeta[]) {
  return types[0]?.value ?? 'salary'
}

function mergeOptionTypes(typeRows: PlatformOptionType[], optionRows: PlatformOption[], platform: PlatformKey): OptionTypeMeta[] {
  const merged = new Map<string, OptionTypeMeta>()
  typeRows
    .filter((row) => row.platform === platform && String(row.type ?? '').trim())
    .forEach((row) => {
      const type = String(row.type).trim()
      merged.set(type, {
        id: row.id,
        value: type,
        label: row.label || type,
        sortOrder: row.sortOrder,
        enabled: row.enabled,
      })
    })
  optionRows
    .filter((row) => row.platform === platform && String(row.type ?? '').trim())
    .forEach((row) => {
      const type = String(row.type).trim()
      if (!merged.has(type)) {
        merged.set(type, { value: type, label: `${type}（当前平台已有）`, fallback: true })
      }
    })
  return Array.from(merged.values())
}

function resolveOptionType(types: OptionTypeMeta[], type: string) {
  return types.some((item) => item.value === type) ? type : getDefaultOptionType(types)
}

function getOptionTypeLabel(types: OptionTypeMeta[], type?: string) {
  if (!type) return ''
  return types.find((item) => item.value === type)?.label ?? type
}

const emptyCity: City = { name: '', province: '', cityCode: '', sortOrder: 100, enabled: 1 }
const emptyCode: CityPlatformCode = { cityId: undefined, platform: 'boss', platformCityCode: '', platformCityName: '', enabled: 1 }
const emptyOption: PlatformOption = { platform: 'boss', type: 'salary', name: '', code: '', sortOrder: 100, enabled: 1 }
const emptyOptionType: PlatformOptionType = { platform: 'boss', type: '', label: '', sortOrder: 100, enabled: 1 }

export default function BaseDataPage() {
  const { authedFetchJson } = useAuthedRequest()
  const [cities, setCities] = useState<City[]>([])
  const [cityCodes, setCityCodes] = useState<CityPlatformCode[]>([])
  const [options, setOptions] = useState<PlatformOption[]>([])
  const [platformOptions, setPlatformOptions] = useState<PlatformOption[]>([])
  const [optionTypes, setOptionTypes] = useState<PlatformOptionType[]>([])
  const [cityForm, setCityForm] = useState<City>(emptyCity)
  const [codeForm, setCodeForm] = useState<CityPlatformCode>(emptyCode)
  const [optionForm, setOptionForm] = useState<PlatformOption>(emptyOption)
  const [optionTypeForm, setOptionTypeForm] = useState<PlatformOptionType>(emptyOptionType)
  const [platformFilter, setPlatformFilter] = useState<PlatformKey>('boss')
  const [typeFilter, setTypeFilter] = useState('salary')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')
  const [deleteTarget, setDeleteTarget] = useState<{ path: string; label: string } | null>(null)
  const loadSeqRef = useRef(0)

  const cityNameById = useMemo(() => {
    const map = new Map<number, string>()
    cities.forEach((city) => {
      if (city.id != null) map.set(city.id, city.name)
    })
    return map
  }, [cities])

  const availableOptionTypes = useMemo(
    () => mergeOptionTypes(optionTypes, platformOptions, platformFilter),
    [optionTypes, platformOptions, platformFilter],
  )

  const loadAll = useCallback(async () => {
    const sequence = loadSeqRef.current + 1
    loadSeqRef.current = sequence
    setLoading(true)
    try {
      const platform = encodeURIComponent(platformFilter)
      const type = encodeURIComponent(typeFilter)
      const [cityRows, codeRows, optionRows, platformOptionRows, optionTypeRows] = await Promise.all([
        authedFetchJson<City[]>('/api/cities'),
        authedFetchJson<CityPlatformCode[]>(`/api/city-platform-codes?platform=${platform}`),
        authedFetchJson<PlatformOption[]>(`/api/platform-options?platform=${platform}&type=${type}`),
        authedFetchJson<PlatformOption[]>(`/api/platform-options?platform=${platform}`),
        authedFetchJson<PlatformOptionType[]>(`/api/platform-option-types?platform=${platform}`),
      ])
      if (sequence !== loadSeqRef.current) return
      const nextAvailableTypes = mergeOptionTypes(optionTypeRows, platformOptionRows, platformFilter)
      const resolvedType = resolveOptionType(nextAvailableTypes, typeFilter)
      setCities(cityRows)
      setCityCodes(codeRows)
      setOptions(resolvedType === typeFilter ? optionRows : [])
      setPlatformOptions(platformOptionRows)
      setOptionTypes(optionTypeRows)
      if (resolvedType !== typeFilter) {
        setTypeFilter(resolvedType)
        setOptionForm({ ...emptyOption, platform: platformFilter, type: resolvedType })
      }
      setMessage('')
    } catch (error) {
      if (sequence !== loadSeqRef.current) return
      console.error('load base data failed', error)
      setMessage('基础数据加载失败')
    } finally {
      if (sequence === loadSeqRef.current) setLoading(false)
    }
  }, [authedFetchJson, platformFilter, typeFilter])

  useEffect(() => {
    queueMicrotask(() => void loadAll())
  }, [loadAll])

  const validateCity = () => {
    if (!cityForm.name.trim()) return '请填写城市名称'
    if (!cityForm.cityCode.trim()) return '请填写通用编码'
    return ''
  }

  const validateCode = () => {
    if (!codeForm.cityId) return '请选择全局城市'
    if (!codeForm.platformCityCode.trim()) return '请填写平台编码'
    return ''
  }

  const validateOption = () => {
    if (!optionForm.type.trim()) return '请选择字典类型'
    if (!optionForm.name.trim()) return '请填写字典名称'
    if (!optionForm.code.trim()) return '请填写字典编码'
    return ''
  }

  const validateOptionType = () => {
    if (!optionTypeForm.type.trim()) return '请填写类型编码'
    if (!optionTypeForm.label.trim()) return '请填写类型名称'
    return ''
  }

  const saveCity = async () => {
    const error = validateCity()
    if (error) {
      setMessage(error)
      return
    }
    const path = cityForm.id ? `/api/cities/${cityForm.id}` : '/api/cities'
    const method = cityForm.id ? 'PUT' : 'POST'
    const saved = await authedFetchJson<City>(path, {
      method,
      body: JSON.stringify(cityForm),
    })
    setCityForm(emptyCity)
    setMessage(`城市已保存：${saved.name}`)
    await loadAll()
  }

  const saveCode = async () => {
    const error = validateCode()
    if (error) {
      setMessage(error)
      return
    }
    const path = codeForm.id ? `/api/city-platform-codes/${codeForm.id}` : '/api/city-platform-codes'
    const method = codeForm.id ? 'PUT' : 'POST'
    const saved = await authedFetchJson<CityPlatformCode>(path, {
      method,
      body: JSON.stringify(codeForm),
    })
    setCodeForm({ ...emptyCode, platform: platformFilter })
    setMessage(`平台城市编码已保存：${saved.platformCityName || saved.platformCityCode}`)
    await loadAll()
  }

  const saveOption = async () => {
    const error = validateOption()
    if (error) {
      setMessage(error)
      return
    }
    const path = optionForm.id ? `/api/platform-options/${optionForm.id}` : '/api/platform-options'
    const method = optionForm.id ? 'PUT' : 'POST'
    const saved = await authedFetchJson<PlatformOption>(path, {
      method,
      body: JSON.stringify(optionForm),
    })
    setOptionForm({ ...emptyOption, platform: platformFilter, type: typeFilter })
    setMessage(`平台字典已保存：${saved.name}`)
    await loadAll()
  }

  const saveOptionType = async () => {
    const error = validateOptionType()
    if (error) {
      setMessage(error)
      return
    }
    const payload = { ...optionTypeForm, platform: platformFilter }
    const path = optionTypeForm.id ? `/api/platform-option-types/${optionTypeForm.id}` : '/api/platform-option-types'
    const method = optionTypeForm.id ? 'PUT' : 'POST'
    const saved = await authedFetchJson<PlatformOptionType>(path, {
      method,
      body: JSON.stringify(payload),
    })
    setOptionTypeForm({ ...emptyOptionType, platform: platformFilter })
    setTypeFilter(saved.type)
    setOptionForm({ ...emptyOption, platform: platformFilter, type: saved.type })
    setMessage(`字典类型已保存：${saved.label}`)
    await loadAll()
  }

  const remove = async (path: string) => {
    await authedFetchJson(path, { method: 'DELETE' })
    await loadAll()
  }

  const requestRemove = (path: string, label: string) => {
    setDeleteTarget({ path, label })
  }

  const handlePlatformFilterChange = useCallback((platform: PlatformKey) => {
    const nextTypes = mergeOptionTypes(optionTypes, platformOptions, platform)
    const nextType = resolveOptionType(nextTypes, typeFilter)
    setPlatformFilter(platform)
    setTypeFilter(nextType)
    setCodeForm({ ...emptyCode, platform })
    setOptionForm({ ...emptyOption, platform, type: nextType })
    setOptionTypeForm({ ...emptyOptionType, platform })
    setMessage('')
  }, [optionTypes, platformOptions, typeFilter])

  const handleTypeFilterChange = useCallback((type: string) => {
    setTypeFilter(type)
    setOptionForm({ ...emptyOption, platform: platformFilter, type })
    setMessage('')
  }, [platformFilter])

  const handleCodePlatformChange = useCallback((platform: PlatformKey) => {
    const nextTypes = mergeOptionTypes(optionTypes, platformOptions, platform)
    const nextType = resolveOptionType(nextTypes, typeFilter)
    setPlatformFilter(platform)
    setTypeFilter(nextType)
    setCodeForm((current) => ({
      ...current,
      id: current.platform === platform ? current.id : undefined,
      platform,
    }))
    setOptionForm({ ...emptyOption, platform, type: nextType })
    setOptionTypeForm({ ...emptyOptionType, platform })
    setMessage('')
  }, [optionTypes, platformOptions, typeFilter])

  const handleOptionPlatformChange = useCallback((platform: PlatformKey) => {
    const nextTypes = mergeOptionTypes(optionTypes, platformOptions, platform)
    const nextType = resolveOptionType(nextTypes, optionForm.type)
    setPlatformFilter(platform)
    setTypeFilter(nextType)
    setCodeForm({ ...emptyCode, platform })
    setOptionForm({ ...emptyOption, platform, type: nextType })
    setOptionTypeForm({ ...emptyOptionType, platform })
    setMessage('')
  }, [optionForm.type, optionTypes, platformOptions])

  const handleOptionTypeChange = useCallback((type: string) => {
    setTypeFilter(type)
    setOptionForm((current) => ({
      ...current,
      id: current.type === type ? current.id : undefined,
      type,
    }))
    setMessage('')
  }, [])

  return (
    <div className="space-y-6">
      <PageHeader
        icon={<BiData />}
        title="基础数据"
        subtitle="维护全局城市、平台城市编码和平台字典"
        actions={
          <Button onClick={loadAll} size="sm" variant="outline" disabled={loading}>
            <BiRefresh /> 刷新
          </Button>
        }
      />

      {message && (
        <div className="rounded-md border border-border bg-muted px-3 py-2 text-sm text-foreground">
          {message}
        </div>
      )}

      <Tabs defaultValue="cities" className="space-y-5">
        <TabsList>
          <TabsTrigger value="cities">城市管理</TabsTrigger>
          <TabsTrigger value="codes">平台城市编码</TabsTrigger>
          <TabsTrigger value="options">平台字典</TabsTrigger>
        </TabsList>

        <TabsContent value="cities" className="space-y-5">
          <Card>
            <CardHeader>
              <CardTitle>城市管理</CardTitle>
              <CardDescription>维护可复用的全局城市主数据</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 gap-3 md:grid-cols-5">
                <Field label="城市名称"><Input value={cityForm.name} onChange={(e) => setCityForm({ ...cityForm, name: e.target.value })} /></Field>
                <Field label="省份"><Input value={cityForm.province || ''} onChange={(e) => setCityForm({ ...cityForm, province: e.target.value })} /></Field>
                <Field label="通用编码"><Input value={cityForm.cityCode} onChange={(e) => setCityForm({ ...cityForm, cityCode: e.target.value })} /></Field>
                <Field label="排序"><Input type="number" value={cityForm.sortOrder ?? ''} onChange={(e) => setCityForm({ ...cityForm, sortOrder: Number(e.target.value || 0) })} /></Field>
                <div className="flex items-end gap-2">
                  <Button onClick={saveCity} className="w-full"><BiSave /> 保存</Button>
                  {cityForm.id && <Button variant="outline" onClick={() => setCityForm(emptyCity)}>取消</Button>}
                </div>
              </div>
              <DataTable
                emptyText="暂无城市数据"
                rows={cities.map((city) => ({
                  key: city.id || city.cityCode,
                  cells: [city.name, city.province || '-', city.cityCode, String(city.sortOrder ?? '-')],
                  onEdit: () => setCityForm(city),
                  onDelete: city.id ? () => requestRemove(`/api/cities/${city.id}`, city.name) : undefined,
                }))}
                headers={['城市', '省份', '编码', '排序']}
              />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="codes" className="space-y-5">
          <FilterBar
            platform={platformFilter}
            setPlatform={handlePlatformFilterChange}
            loading={loading}
            resultCount={cityCodes.length}
          />
          <Card>
            <CardHeader>
              <CardTitle>平台城市编码</CardTitle>
              <CardDescription>把全局城市映射到各平台自己的城市编码</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 gap-3 md:grid-cols-5">
                <Field label="全局城市">
                  <Select value={String(codeForm.cityId || '')} onChange={(e) => setCodeForm({ ...codeForm, cityId: e.target.value ? Number(e.target.value) : undefined })}>
                    <option value="">请选择城市</option>
                    {cities.map((city) => <option key={city.id} value={city.id}>{city.name}</option>)}
                  </Select>
                </Field>
                <Field label="平台">
                  <PlatformSelect value={codeForm.platform} onChange={handleCodePlatformChange} />
                </Field>
                <Field label="平台编码"><Input value={codeForm.platformCityCode} onChange={(e) => setCodeForm({ ...codeForm, platformCityCode: e.target.value })} /></Field>
                <Field label="平台显示名"><Input value={codeForm.platformCityName || ''} onChange={(e) => setCodeForm({ ...codeForm, platformCityName: e.target.value })} /></Field>
                <div className="flex items-end gap-2">
                  <Button onClick={saveCode} className="w-full"><BiSave /> 保存</Button>
                  {codeForm.id && <Button variant="outline" onClick={() => setCodeForm({ ...emptyCode, platform: platformFilter })}>取消</Button>}
                </div>
              </div>
              <DataTable
                emptyText="暂无平台城市编码"
                rows={cityCodes.map((row) => ({
                  key: row.id || `${row.platform}-${row.platformCityCode}`,
                  cells: [cityNameById.get(row.cityId || 0) || '-', row.platform, row.platformCityCode, row.platformCityName || '-'],
                  onEdit: () => setCodeForm(row),
                  onDelete: row.id ? () => requestRemove(`/api/city-platform-codes/${row.id}`, row.platformCityName || row.platformCityCode) : undefined,
                }))}
                headers={['全局城市', '平台', '平台编码', '显示名']}
              />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="options" className="space-y-5">
          <FilterBar
            platform={platformFilter}
            setPlatform={handlePlatformFilterChange}
            type={typeFilter}
            setType={handleTypeFilterChange}
            types={availableOptionTypes}
            loading={loading}
            resultCount={options.length}
          />
          <Card>
            <CardHeader>
              <CardTitle>字典类型</CardTitle>
              <CardDescription>维护当前平台可用的字典类型，平台字典选项会按类型分组</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 gap-3 md:grid-cols-5">
                <Field label="类型编码"><Input value={optionTypeForm.type} onChange={(e) => setOptionTypeForm({ ...optionTypeForm, type: e.target.value })} /></Field>
                <Field label="显示名称"><Input value={optionTypeForm.label} onChange={(e) => setOptionTypeForm({ ...optionTypeForm, label: e.target.value })} /></Field>
                <Field label="排序"><Input type="number" value={optionTypeForm.sortOrder ?? ''} onChange={(e) => setOptionTypeForm({ ...optionTypeForm, sortOrder: Number(e.target.value || 0) })} /></Field>
                <Field label="启用状态">
                  <Select value={String(optionTypeForm.enabled ?? 1)} onChange={(e) => setOptionTypeForm({ ...optionTypeForm, enabled: Number(e.target.value) })}>
                    <option value="1">启用</option>
                    <option value="0">停用</option>
                  </Select>
                </Field>
                <div className="flex items-end gap-2">
                  <Button onClick={saveOptionType} className="w-full"><BiSave /> 保存</Button>
                  {optionTypeForm.id && <Button variant="outline" onClick={() => setOptionTypeForm({ ...emptyOptionType, platform: platformFilter })}>取消</Button>}
                </div>
              </div>
              <DataTable
                emptyText="暂无字典类型"
                rows={availableOptionTypes.map((row) => ({
                  key: row.id || `${platformFilter}-${row.value}`,
                  cells: [platformFilter, row.value, row.label, String(row.sortOrder ?? '-'), row.fallback ? '已有选项' : row.enabled === 0 ? '停用' : '启用'],
                  onEdit: row.fallback ? undefined : () => setOptionTypeForm({
                    id: row.id,
                    platform: platformFilter,
                    type: row.value,
                    label: row.label,
                    sortOrder: row.sortOrder,
                    enabled: row.enabled ?? 1,
                  }),
                  onDelete: row.id ? () => requestRemove(`/api/platform-option-types/${row.id}`, row.label) : undefined,
                }))}
                headers={['平台', '类型编码', '显示名称', '排序', '状态']}
              />
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle>平台字典</CardTitle>
              <CardDescription>维护薪资、经验、学历、行业等非城市类选项</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-6">
                <Field label="平台"><PlatformSelect value={optionForm.platform} onChange={handleOptionPlatformChange} /></Field>
                <Field label="类型"><TypeSelect value={optionForm.type} onChange={handleOptionTypeChange} types={availableOptionTypes} /></Field>
                <Field label="名称"><Input value={optionForm.name} onChange={(e) => setOptionForm({ ...optionForm, name: e.target.value })} /></Field>
                <Field label="编码"><Input value={optionForm.code} onChange={(e) => setOptionForm({ ...optionForm, code: e.target.value })} /></Field>
                <Field label="排序"><Input type="number" value={optionForm.sortOrder ?? ''} onChange={(e) => setOptionForm({ ...optionForm, sortOrder: Number(e.target.value || 0) })} /></Field>
                <div className="flex items-end gap-2">
                  <Button onClick={saveOption} className="w-full"><BiSave /> 保存</Button>
                  {optionForm.id && <Button variant="outline" onClick={() => setOptionForm({ ...emptyOption, platform: platformFilter, type: typeFilter })}>取消</Button>}
                </div>
              </div>
              <DataTable
                emptyText="暂无平台字典"
                rows={options.map((row) => ({
                  key: row.id || `${row.platform}-${row.type}-${row.code}`,
                  cells: [row.platform, row.type, row.name, row.code, String(row.sortOrder ?? '-')],
                  onEdit: () => setOptionForm(row),
                  onDelete: row.id ? () => requestRemove(`/api/platform-options/${row.id}`, row.name) : undefined,
                }))}
                headers={['平台', '类型', '名称', '编码', '排序']}
              />
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title="确认删除"
        message={deleteTarget ? `将删除「${deleteTarget.label}」，此操作不可撤销。` : ''}
        confirmLabel="删除"
        destructive
        onClose={() => setDeleteTarget(null)}
        onConfirm={async () => {
          if (deleteTarget) await remove(deleteTarget.path)
        }}
      />
      <FeedbackDialog
        open={message.length > 0 && !deleteTarget}
        title="操作提示"
        message={message}
        tone={message.includes('失败') || message.includes('请') ? 'error' : 'success'}
        onClose={() => setMessage('')}
      />
    </div>
  )
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return <div className="space-y-2"><Label>{label}</Label>{children}</div>
}

function PlatformSelect({ value, onChange }: { value: PlatformKey; onChange: (platform: PlatformKey) => void }) {
  return (
    <Select value={value} onChange={(e) => onChange(e.target.value as PlatformKey)}>
      {platforms.map((platform) => <option key={platform.value} value={platform.value}>{platform.label}</option>)}
    </Select>
  )
}

function TypeSelect({ value, onChange, types }: { value: string; onChange: (type: string) => void; types: OptionTypeMeta[] }) {
  return (
    <Select value={value} onChange={(e) => onChange(e.target.value)}>
      {types.length === 0 && <option value="">暂无类型</option>}
      {types.map((type) => <option key={type.value} value={type.value}>{type.label}</option>)}
    </Select>
  )
}

function FilterBar({
  platform,
  setPlatform,
  type,
  setType,
  types = [],
  loading,
  resultCount,
}: {
  platform: PlatformKey
  setPlatform: (platform: PlatformKey) => void
  type?: string
  setType?: (type: string) => void
  types?: OptionTypeMeta[]
  loading?: boolean
  resultCount: number
}) {
  const platformLabel = platforms.find((item) => item.value === platform)?.label ?? platform
  const typeLabel = getOptionTypeLabel(types, type)
  return (
    <div className="flex flex-col gap-3 rounded-lg border border-border bg-muted/30 p-3 sm:flex-row sm:items-end sm:justify-between">
      <div className="flex flex-wrap items-end gap-3">
        <Field label="平台"><PlatformSelect value={platform} onChange={setPlatform} /></Field>
        {type && setType && <Field label="类型"><TypeSelect value={type} onChange={setType} types={types} /></Field>}
      </div>
      <div className="rounded-md border border-border bg-background px-3 py-2 text-xs text-muted-foreground">
        {loading ? '正在刷新...' : `当前 ${platformLabel}${typeLabel ? ` / ${typeLabel}` : ''}：${resultCount} 条`}
      </div>
    </div>
  )
}

function DataTable({ headers, rows, emptyText }: { headers: string[]; rows: { key: Key; cells: string[]; onEdit?: () => void; onDelete?: () => void }[]; emptyText: string }) {
  if (rows.length === 0) {
    return <div className="rounded-md border border-dashed border-border p-6 text-center text-sm text-muted-foreground">{emptyText}</div>
  }
  return (
    <div className="overflow-x-auto rounded-md border border-border">
      <table className="w-full min-w-[720px] text-sm">
        <thead className="bg-muted/60 text-muted-foreground">
          <tr>
            {headers.map((header) => <th key={header} className="px-3 py-2 text-left font-medium">{header}</th>)}
            <th className="w-32 px-3 py-2 text-right font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.key} className="border-t border-border">
              {row.cells.map((cell, index) => <td key={index} className="px-3 py-2">{cell}</td>)}
              <td className="px-3 py-2">
                <div className="flex justify-end gap-2">
                  {row.onEdit && <Button size="sm" variant="outline" onClick={row.onEdit}>编辑</Button>}
                  {row.onDelete && <Button size="sm" variant="destructive" onClick={row.onDelete}><BiTrash /></Button>}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
