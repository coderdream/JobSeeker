"use client"

import { ReactNode, useEffect, useId, useRef } from "react"
import { BiCheckCircle, BiErrorCircle, BiInfoCircle, BiX } from "react-icons/bi"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { cn } from "@/lib/utils"

export type FeedbackTone = "success" | "error" | "info"

const toneIcon = {
  success: BiCheckCircle,
  error: BiErrorCircle,
  info: BiInfoCircle,
}

const toneClass: Record<FeedbackTone, string> = {
  success: "text-emerald-600 dark:text-emerald-300",
  error: "text-red-600 dark:text-red-300",
  info: "text-primary",
}

export function FeedbackDialog({
  open,
  title,
  message,
  tone = "info",
  confirmLabel = "知道了",
  showDefaultAction = true,
  onClose,
  children,
}: {
  open: boolean
  title: string
  message?: ReactNode
  tone?: FeedbackTone
  confirmLabel?: string
  showDefaultAction?: boolean
  onClose: () => void
  children?: ReactNode
}) {
  const titleId = useId()
  const closeButtonRef = useRef<HTMLButtonElement>(null)
  const Icon = toneIcon[tone]

  useEffect(() => {
    if (!open) return
    const active = document.activeElement instanceof HTMLElement ? document.activeElement : null
    closeButtonRef.current?.focus()
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose()
    }
    document.addEventListener("keydown", onKeyDown)
    return () => {
      document.removeEventListener("keydown", onKeyDown)
      active?.focus()
    }
  }, [onClose, open])

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/35 px-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <Card className="w-full max-w-sm animate-in fade-in zoom-in-95 border-border bg-background shadow-lg">
        <CardHeader className="flex-row items-start justify-between gap-4 pb-2">
          <CardTitle id={titleId} className="flex items-center gap-2 text-lg">
            <Icon className={cn("h-5 w-5", toneClass[tone])} />
            {title}
          </CardTitle>
          <button
            ref={closeButtonRef}
            type="button"
            aria-label="关闭"
            onClick={onClose}
            className="inline-flex h-8 w-8 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          >
            <BiX className="h-5 w-5" />
          </button>
        </CardHeader>
        <CardContent className="space-y-4">
          {message ? <div className="text-sm leading-6 text-muted-foreground">{message}</div> : null}
          {children}
          {showDefaultAction ? (
            <div className="flex justify-end">
              <Button onClick={onClose} className="px-3">
                {confirmLabel}
              </Button>
            </div>
          ) : null}
        </CardContent>
      </Card>
    </div>
  )
}

export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = "确认",
  cancelLabel = "取消",
  onConfirm,
  onClose,
  destructive = false,
}: {
  open: boolean
  title: string
  message?: ReactNode
  confirmLabel?: string
  cancelLabel?: string
  onConfirm: () => void | Promise<void>
  onClose: () => void
  destructive?: boolean
}) {
  return (
    <FeedbackDialog
      open={open}
      title={title}
      message={message}
      tone={destructive ? "error" : "info"}
      onClose={onClose}
      showDefaultAction={false}
    >
      <div className="flex justify-end gap-2">
        <Button variant="ghost" onClick={onClose} className="px-3">
          {cancelLabel}
        </Button>
        <Button
          variant={destructive ? "destructive" : "default"}
          onClick={async () => {
            await onConfirm()
            onClose()
          }}
          className="px-3"
        >
          {confirmLabel}
        </Button>
      </div>
    </FeedbackDialog>
  )
}
