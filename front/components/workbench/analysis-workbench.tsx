"use client"

import { ReactNode } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"

export type ChartSpec = {
  title: string
  description?: string
  content: ReactNode
}

export type AnalysisFilterState = Record<string, string | string[] | number | boolean | undefined>

export type PagedJobResult<T> = {
  items: T[]
  total: number
  page: number
  size: number
}

export function KpiGrid({ cards }: { cards: { label: string; value: ReactNode }[] }) {
  return (
    <div className="grid grid-cols-2 gap-3 lg:grid-cols-4 2xl:grid-cols-6">
      {cards.map((card) => (
        <Card key={card.label}>
          <CardHeader className="p-4">
            <CardTitle className="text-sm text-muted-foreground">{card.label}</CardTitle>
            <CardDescription className="text-2xl font-semibold text-foreground">{card.value}</CardDescription>
          </CardHeader>
        </Card>
      ))}
    </div>
  )
}

export function ChartCard({ title, description, content }: ChartSpec) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">{title}</CardTitle>
        {description ? <CardDescription>{description}</CardDescription> : null}
      </CardHeader>
      <CardContent>{content}</CardContent>
    </Card>
  )
}

export function AnalysisWorkbench({
  kpis,
  filters,
  charts,
  table,
}: {
  kpis: { label: string; value: ReactNode }[]
  filters?: ReactNode
  charts?: ChartSpec[]
  table: ReactNode
}) {
  return (
    <div className="min-w-0 max-w-full space-y-5 overflow-hidden">
      <KpiGrid cards={kpis} />
      {filters}
      {charts ? (
        <div className="grid min-w-0 grid-cols-1 gap-4 2xl:grid-cols-2">
          {charts.map((chart) => (
            <ChartCard key={chart.title} {...chart} />
          ))}
        </div>
      ) : null}
      {table}
    </div>
  )
}
