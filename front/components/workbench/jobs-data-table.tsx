"use client"

import { ReactNode } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { cn } from "@/lib/utils"

export type JobTableColumn<T> = {
  key: string
  header: string
  className?: string
  render: (item: T) => ReactNode
}

export function JobsDataTable<T>({
  title,
  description,
  items,
  columns,
  emptyText = "暂无数据",
  getRowKey,
  footer,
}: {
  title: string
  description?: string
  items: T[]
  columns: JobTableColumn<T>[]
  emptyText?: string
  getRowKey: (item: T, index: number) => string | number
  footer?: ReactNode
}) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">{title}</CardTitle>
        {description ? <CardDescription>{description}</CardDescription> : null}
      </CardHeader>
      <CardContent>
        <div className="max-w-full overflow-x-auto rounded-lg border border-border">
          <table className="min-w-full text-sm">
            <thead className="sticky top-0 z-10 bg-muted/80 text-muted-foreground">
              <tr>
                {columns.map((column) => (
                  <th key={column.key} className={cn("whitespace-nowrap px-3 py-2 text-left font-medium", column.className)}>
                    {column.header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {items.length === 0 ? (
                <tr>
                  <td colSpan={columns.length} className="px-3 py-10 text-center text-muted-foreground">
                    {emptyText}
                  </td>
                </tr>
              ) : (
                items.map((item, index) => (
                  <tr key={getRowKey(item, index)} className="border-t border-border hover:bg-muted/40">
                    {columns.map((column) => (
                      <td key={column.key} className={cn("whitespace-nowrap px-3 py-2 align-top", column.className)}>
                        {column.render(item)}
                      </td>
                    ))}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        {footer ? <div className="mt-4">{footer}</div> : null}
      </CardContent>
    </Card>
  )
}
