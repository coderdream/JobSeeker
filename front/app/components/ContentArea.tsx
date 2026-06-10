"use client"
import { usePathname } from 'next/navigation'
import { ReactNode, useMemo } from 'react'
import { motion } from 'framer-motion'
import AuthGuard from '@/components/auth/AuthGuard'

export default function ContentArea({ children }: { children: ReactNode }) {
  const pathname = usePathname()

  const accentClass = useMemo(() => {
    if (pathname.startsWith('/boss')) return 'accent-teal'
    if (pathname.startsWith('/liepin')) return 'accent-orange'
    if (pathname.startsWith('/51job')) return 'accent-amber'
    if (pathname.startsWith('/zhilian')) return 'accent-sky'
    if (pathname.startsWith('/yupao')) return 'accent-emerald'
    return ''
  }, [pathname])

  return (
    <AuthGuard>
      <main className={`min-h-screen w-full min-w-0 bg-background content-bg ${accentClass} pt-14 lg:pl-64 lg:pt-0`}>
        <motion.div
          key={pathname}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -20 }}
          transition={{ duration: 0.4, ease: "easeInOut" }}
          className="mx-auto w-full max-w-[1400px] px-4 py-5 sm:px-5 lg:px-6"
        >
          {children}
        </motion.div>
      </main>
    </AuthGuard>
  )
}
