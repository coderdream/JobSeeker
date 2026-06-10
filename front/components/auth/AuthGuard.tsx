"use client"

import { useEffect } from "react"
import { usePathname, useRouter } from "next/navigation"
import { useAuth } from "./AuthProvider"

export default function AuthGuard({ children }: { children: React.ReactNode }) {
  const { ready, isAuthenticated } = useAuth()
  const router = useRouter()
  const pathname = usePathname()

  useEffect(() => {
    if (!ready) return
    if (!isAuthenticated) {
      const target = pathname && pathname !== "/" ? `?redirect=${encodeURIComponent(pathname)}` : ""
      router.replace(`/login${target}`)
    }
  }, [isAuthenticated, pathname, ready, router])

  if (!ready) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-background dark:bg-blacksection">
        <div className="rounded-lg border border-border bg-card px-6 py-4 text-sm text-muted-foreground shadow-sm">
          正在校验登录状态...
        </div>
      </main>
    )
  }

  if (!isAuthenticated) {
    return null
  }

  return <>{children}</>
}
