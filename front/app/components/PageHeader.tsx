"use client"
import { ReactNode } from 'react'
import { motion } from 'framer-motion'

export default function PageHeader({
  icon,
  title,
  subtitle,
  iconClass = 'text-primary',
  accentBgClass = 'bg-primary/10 dark:bg-primary/20',
  actions,
}: {
  icon: ReactNode
  title: string
  subtitle?: string
  iconClass?: string
  accentBgClass?: string
  actions?: ReactNode
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: -20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25, ease: "easeOut" }}
      className="mb-6"
    >
      <div className="flex flex-col gap-4 border-b border-border pb-5 sm:flex-row sm:items-center">
        <motion.div
          initial={{ scale: 0, rotate: -180 }}
          animate={{ scale: 1, rotate: 0 }}
          transition={{ delay: 0.05, type: "spring", stiffness: 180 }}
          className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-lg ${accentBgClass}`}
        >
          <span className={`${iconClass} text-xl`}>{icon}</span>
        </motion.div>
        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.08, duration: 0.25 }}
          className="min-w-0 flex-1"
        >
          <h1 className="truncate text-2xl font-semibold tracking-normal text-foreground">
            {title}
          </h1>
          {subtitle && (
            <p className="mt-1 text-sm text-muted-foreground">
              {subtitle}
            </p>
          )}
        </motion.div>
        {actions && (
          <motion.div
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.1, duration: 0.2 }}
            className="flex flex-wrap items-center gap-2 sm:ml-auto sm:justify-end"
          >
            {actions}
          </motion.div>
        )}
      </div>
    </motion.div>
  )
}
