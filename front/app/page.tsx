"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/components/auth/AuthProvider"

export default function HomeRedirect() {
  const router = useRouter()
  const { ready, isAuthenticated } = useAuth()

  useEffect(() => {
    if (!ready) return
    router.replace(isAuthenticated ? "/env-config" : "/login")
  }, [isAuthenticated, ready, router])

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-6">
      <div className="rounded-lg border border-border bg-card px-6 py-4 text-sm text-muted-foreground shadow-sm">
        正在进入工作台...
      </div>
    </main>
  )
}
