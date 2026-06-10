import type { Metadata } from "next"
import "./globals.css"
import AppShell from "./components/AppShell"

export const metadata: Metadata = {
  title: "Get Jobs - 招聘自动化工作台",
  description: "招聘自动化工作台，用于管理平台配置、投递任务、分析数据和系统配置",
  icons: {
    icon: "data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2290%22>GJ</text></svg>",
  },
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="zh-CN" suppressHydrationWarning>
      <body suppressHydrationWarning className="dark:bg-blacksection">
        <AppShell>{children}</AppShell>
      </body>
    </html>
  )
}
