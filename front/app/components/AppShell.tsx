"use client"

import { ReactNode } from "react"
import { ThemeProvider } from "next-themes"
import { usePathname } from "next/navigation"
import { AuthProvider } from "@/components/auth/AuthProvider"
import Sidebar from "./Sidebar"
import ContentArea from "./ContentArea"

export default function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname()
  const isLoginPage = pathname === "/login"

  return (
    <ThemeProvider attribute="class" defaultTheme="light" enableSystem={false}>
      <AuthProvider>
        {isLoginPage ? (
          children
        ) : (
          <div className="flex min-h-screen">
            <Sidebar />
            <ContentArea>{children}</ContentArea>
          </div>
        )}
      </AuthProvider>
    </ThemeProvider>
  )
}
