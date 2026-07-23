'use client'

import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { useEffect, useRef, useState } from 'react'
import {
  BiBrain,
  BiBriefcase,
  BiData,
  BiEnvelope,
  BiLogOut,
  BiMenu,
  BiMoon,
  BiSearch,
  BiSun,
  BiTask,
  BiUserCircle,
  BiX,
} from 'react-icons/bi'
import { motion } from 'framer-motion'
import { useTheme } from 'next-themes'
import { useAuth } from '@/components/auth/AuthProvider'
import { getApiBaseUrl } from '@/lib/api-client'

const navSections = [
  {
    title: '系统配置',
    items: [
      { href: '/env-config', icon: BiEnvelope, label: '环境配置' },
      { href: '/ai-config', icon: BiBrain, label: 'AI 配置' },
      { href: '/base-data', icon: BiData, label: '基础数据' },
    ],
  },
  {
    title: '招聘平台',
    items: [
      { href: '/boss', icon: BiBriefcase, label: 'Boss 直聘' },
      { href: '/liepin', icon: BiSearch, label: '猎聘' },
      { href: '/51job', icon: BiTask, label: '51job' },
      { href: '/zhilian', icon: BiUserCircle, label: '智联招聘' },
      { href: '/yupao', icon: BiBriefcase, label: '鱼泡直聘' },
    ],
  },
]

const healthText = {
  up: '系统运行正常',
  degraded: '服务降级',
  down: '服务异常',
  unknown: '未连接',
}

const healthClass = {
  up: 'bg-emerald-500',
  degraded: 'bg-amber-500',
  down: 'bg-red-500',
  unknown: 'bg-slate-400',
}

export default function Sidebar() {
  const pathname = usePathname()
  const router = useRouter()
  const { theme, setTheme } = useTheme()
  const { user, logout, isAuthenticated } = useAuth()

  const [mobileOpen, setMobileOpen] = useState(false)
  const [health, setHealth] = useState<'up' | 'degraded' | 'down' | 'unknown'>('unknown')
  const [backendVersion, setBackendVersion] = useState<string>('')
  const frontendVersion = process.env.NEXT_PUBLIC_FRONTEND_VERSION || 'vf.unknown'
  const checkingRef = useRef(false)

  useEffect(() => {
    let interval: NodeJS.Timeout | null = null

    const check = async () => {
      if (checkingRef.current) return
      checkingRef.current = true

      const baseUrl = getApiBaseUrl()
      const controller = new AbortController()
      const timeout = setTimeout(() => controller.abort(), 3000)

      try {
        let res = await fetch(`${baseUrl}/api/health`, { signal: controller.signal })
        if (res.status === 404) {
          res = await fetch(`${baseUrl}/actuator/health`, { signal: controller.signal })
        }
        if (!res.ok) throw new Error(`status ${res.status}`)
        const data = await res.json()
        const statusRaw = (data.status || data.state || '').toString().toUpperCase()
        if (statusRaw === 'UP' || statusRaw === 'HEALTHY') {
          setHealth('up')
        } else if (statusRaw === 'DEGRADED' || statusRaw === 'WARN') {
          setHealth('degraded')
        } else {
          setHealth('down')
        }
      } catch {
        setHealth('unknown')
      } finally {
        clearTimeout(timeout)
        checkingRef.current = false
      }
    }

    void check()
    interval = setInterval(() => {
      void check()
    }, 30000)

    return () => {
      if (interval) clearInterval(interval)
    }
  }, [])

  useEffect(() => {
    const fetchVersion = async () => {
      try {
        const baseUrl = getApiBaseUrl()
        const res = await fetch(`${baseUrl}/api/system/version`)
        if (res.ok) {
          const data = await res.json()
          setBackendVersion(data.backendVersion || '')
        }
      } catch (e) {
        // ignore
      }
    }
    void fetchVersion()
  }, [])

  const handleLogout = async () => {
    await logout()
    router.replace('/login')
  }

  const renderNav = () => (
    <nav className="space-y-6">
      {navSections.map((section) => (
        <div key={section.title}>
          <div className="px-3 text-xs font-semibold uppercase text-muted-foreground">
            {section.title}
          </div>
          <div className="mt-2 space-y-1">
            {section.items.map((item) => {
              const Icon = item.icon
              const isActive = pathname === item.href || pathname.startsWith(`${item.href}/`)
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  onClick={() => setMobileOpen(false)}
                  className={`group flex h-10 items-center gap-3 rounded-md px-3 text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-primary/10 text-primary'
                      : 'text-muted-foreground hover:bg-accent hover:text-foreground'
                  }`}
                >
                  <Icon className="text-lg" />
                  <span>{item.label}</span>
                  {isActive ? <span className="ml-auto h-1.5 w-1.5 rounded-full bg-primary" /> : null}
                </Link>
              )
            })}
          </div>
        </div>
      ))}
    </nav>
  )

  const sidebarBody = (
    <div className="flex h-full flex-col bg-card">
      <div className="border-b border-border p-5">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary text-sm font-semibold text-primary-foreground">
            GJ
          </div>
          <div className="min-w-0">
            <h1 className="truncate text-base font-semibold text-foreground">Get Jobs</h1>
            <p className="text-xs text-muted-foreground">招聘自动化工作台</p>
            <div className="mt-1 flex items-center gap-1.5 text-[10px] text-muted-foreground/80 font-mono">
              <span>{frontendVersion}</span>
              <span>|</span>
              <span>{backendVersion || 'vb.loading'}</span>
            </div>
          </div>
        </div>

        <div className="mt-4 flex items-center gap-2 rounded-md border border-border bg-muted/50 px-3 py-2 text-xs text-muted-foreground">
          <span className={`h-2 w-2 rounded-full ${healthClass[health]}`} />
          <span>{healthText[health]}</span>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4">{renderNav()}</div>

      <div className="border-t border-border p-4">
        <button
          onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
          className="mb-3 flex h-9 w-full items-center justify-center gap-2 rounded-md border border-border bg-background px-3 text-sm text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
          type="button"
        >
          {theme === 'dark' ? <BiSun className="text-base" /> : <BiMoon className="text-base" />}
          <span>{theme === 'dark' ? '浅色模式' : '深色模式'}</span>
        </button>

        {isAuthenticated && user ? (
          <div className="rounded-md border border-border bg-muted/40 p-3">
            <p className="truncate text-sm font-medium text-foreground">{user.nickname || user.username}</p>
            <p className="mt-0.5 truncate text-xs text-muted-foreground">{user.username}</p>
            <button
              className="mt-3 inline-flex items-center gap-2 text-sm text-muted-foreground transition-colors hover:text-foreground"
              onClick={handleLogout}
              type="button"
            >
              <BiLogOut className="text-base" />
              退出登录
            </button>
          </div>
        ) : null}
      </div>
    </div>
  )

  return (
    <>
      <motion.aside
        initial={{ x: -24, opacity: 0 }}
        animate={{ x: 0, opacity: 1 }}
        transition={{ duration: 0.25, ease: 'easeOut' }}
        className="fixed left-0 top-0 z-40 hidden h-full w-64 border-r border-border bg-card shadow-sm lg:block"
      >
        {sidebarBody}
      </motion.aside>

      <header className="fixed left-0 top-0 z-40 flex h-14 w-full items-center justify-between border-b border-border bg-background/95 px-4 shadow-sm backdrop-blur lg:hidden">
        <button
          aria-label="打开导航"
          className="inline-flex h-9 w-9 items-center justify-center rounded-md border border-border text-muted-foreground"
          onClick={() => setMobileOpen(true)}
          type="button"
        >
          <BiMenu className="text-xl" />
        </button>
        <div className="text-sm font-semibold">Get Jobs</div>
        <span className={`h-2 w-2 rounded-full ${healthClass[health]}`} />
      </header>

      {mobileOpen ? (
        <div className="fixed inset-0 z-50 lg:hidden" role="dialog" aria-modal="true">
          <button
            aria-label="关闭导航"
            className="absolute inset-0 bg-black/30"
            onClick={() => setMobileOpen(false)}
            type="button"
          />
          <motion.aside
            initial={{ x: -280 }}
            animate={{ x: 0 }}
            transition={{ duration: 0.2, ease: 'easeOut' }}
            className="relative h-full w-72 max-w-[86vw] border-r border-border bg-card shadow-xl"
          >
            <button
              aria-label="关闭导航"
              className="absolute right-3 top-3 z-10 inline-flex h-8 w-8 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-foreground"
              onClick={() => setMobileOpen(false)}
              type="button"
            >
              <BiX className="text-xl" />
            </button>
            {sidebarBody}
          </motion.aside>
        </div>
      ) : null}
    </>
  )
}
