import type { ReactNode } from "react"

export type PlatformKey = "boss" | "51job" | "liepin" | "zhilian" | "yupao"

export type PlatformTheme = {
  key: PlatformKey
  label: string
  accentClass: string
  toneClass: string
}

export type PlatformCapability = {
  label: string
  supported: boolean
  detail?: ReactNode
}

export const platformThemes: Record<PlatformKey, PlatformTheme> = {
  boss: { key: "boss", label: "Boss 直聘", accentClass: "bg-teal-500", toneClass: "text-teal-600" },
  "51job": { key: "51job", label: "51job", accentClass: "bg-amber-500", toneClass: "text-amber-600" },
  liepin: { key: "liepin", label: "猎聘", accentClass: "bg-orange-500", toneClass: "text-orange-600" },
  zhilian: { key: "zhilian", label: "智联招聘", accentClass: "bg-sky-500", toneClass: "text-sky-600" },
  yupao: { key: "yupao", label: "鱼泡直聘", accentClass: "bg-emerald-600", toneClass: "text-emerald-600" },
}
