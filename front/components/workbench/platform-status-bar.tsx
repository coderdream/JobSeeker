"use client"

import { ReactNode } from "react"
import { StatusPill, StatusTone } from "./status-pill"

export type PlatformStatusItem = {
  label: string
  value: string
  tone?: StatusTone
}

export function PlatformStatusBar({
  platform,
  description,
  items,
  primaryAction,
  secondaryActions,
  className = "",
}: {
  platform: string
  description?: string
  items: PlatformStatusItem[]
  primaryAction?: ReactNode
  secondaryActions?: ReactNode
  className?: string
}) {
  return (
    <section className={`rounded-lg border border-border bg-card p-3 shadow-sm ${className}`} aria-label={`${platform} 状态动作条`}>
      <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="text-sm font-semibold text-foreground">{platform}</h2>
            {items.map((item) => (
              <StatusPill key={`${item.label}-${item.value}`} label={`${item.label}: ${item.value}`} tone={item.tone} />
            ))}
          </div>
          {description ? <p className="mt-2 text-xs leading-5 text-muted-foreground">{description}</p> : null}
        </div>
        <div className="flex flex-wrap items-center gap-2 xl:justify-end">
          {primaryAction}
          {secondaryActions}
        </div>
      </div>
    </section>
  )
}
