"use client"

import { useEffect, useRef } from "react"

type ChartKind = "pie" | "bar" | "line"
type ChartDatasetConfig = {
  label: string
  data: number[]
  backgroundColor: string | string[]
  borderColor?: string | string[]
  fill?: boolean
  pointBackgroundColor?: string
  pointBorderColor?: string
}
type ChartConfig = {
  type: ChartKind
  data: {
    labels: string[]
    datasets: ChartDatasetConfig[]
  }
  options: {
    responsive: boolean
    maintainAspectRatio: boolean
    plugins: {
      legend: { display: boolean }
      title: { display: boolean; text?: string }
    }
    scales?: {
      x: { ticks: { autoSkip: boolean } }
      y: { beginAtZero: boolean }
    }
  }
}
type ChartInstance = { destroy: () => void }
type ChartConstructor = new (ctx: CanvasRenderingContext2D, config: ChartConfig) => ChartInstance
type ChartWindow = Window & { Chart?: ChartConstructor }

type ChartCanvasProps = {
  type: ChartKind
  labels: string[]
  data: number[]
  title?: string
  color?: string
  colors?: string[]
}

const PIE_COLORS = [
  "#3b82f6",
  "#10b981",
  "#f59e0b",
  "#ef4444",
  "#6366f1",
  "#22c55e",
  "#fb7185",
  "#a78bfa",
  "#f97316",
  "#06b6d4",
]

function chartWindow() {
  return window as ChartWindow
}

async function ensureChart(): Promise<ChartConstructor> {
  const currentWindow = chartWindow()
  if (currentWindow.Chart) return currentWindow.Chart

  return new Promise((resolve, reject) => {
    const existing = document.querySelector("script[data-chartjs-cdn='true']") as HTMLScriptElement | null
    if (existing) {
      existing.addEventListener("load", () => {
        const loadedChart = currentWindow.Chart
        if (loadedChart) resolve(loadedChart)
        else reject(new Error("Chart.js loaded without Chart constructor"))
      })
      existing.addEventListener("error", () => reject(new Error("Chart.js CDN load error")))
      return
    }

    const script = document.createElement("script")
    script.src = "https://cdn.jsdelivr.net/npm/chart.js@4.4.4/dist/chart.umd.min.js"
    script.async = true
    script.setAttribute("data-chartjs-cdn", "true")
    script.addEventListener("load", () => {
      const loadedChart = currentWindow.Chart
      if (loadedChart) resolve(loadedChart)
      else reject(new Error("Chart.js loaded without Chart constructor"))
    })
    script.addEventListener("error", () => reject(new Error("Chart.js CDN load error")))
    document.head.appendChild(script)
  })
}

export default function ChartCanvas({
  type,
  labels,
  data,
  title,
  color = "#3b82f6",
  colors,
}: ChartCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const chartRef = useRef<ChartInstance | null>(null)
  const toSolid = (hex: string) => hex

  useEffect(() => {
    const ctx = canvasRef.current?.getContext("2d")
    if (!ctx) return

    if (chartRef.current) {
      chartRef.current.destroy()
      chartRef.current = null
    }

    let cancelled = false
    const backgroundColor = (() => {
      if (type === "pie") return (colors && colors.length ? colors : PIE_COLORS).slice(0, labels.length)
      if (type === "bar" && colors && colors.length) return colors.slice(0, data.length).map((c) => toSolid(c))
      return toSolid(color)
    })()

    const borderColor = (() => {
      if (type === "pie") return undefined
      if (type === "bar" && colors && colors.length) return colors.slice(0, data.length)
      return color
    })()

    const dataset: ChartDatasetConfig = {
      label: title || "",
      data,
      backgroundColor,
      borderColor,
    }

    if (type === "line") {
      dataset.fill = false
      dataset.pointBackgroundColor = toSolid(color)
      dataset.pointBorderColor = toSolid(color)
    }

    void (async () => {
      try {
        const Chart = await ensureChart()
        if (cancelled) return
        chartRef.current = new Chart(ctx, {
          type,
          data: { labels, datasets: [dataset] },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: { display: type === "pie" },
              title: { display: !!title, text: title },
            },
            scales: type !== "pie" ? { x: { ticks: { autoSkip: true } }, y: { beginAtZero: true } } : undefined,
          },
        })
      } catch (error) {
        console.error("Failed to create chart:", error)
      }
    })()

    return () => {
      cancelled = true
      if (chartRef.current) {
        chartRef.current.destroy()
        chartRef.current = null
      }
    }
  }, [type, labels, data, title, color, colors])

  return <canvas ref={canvasRef} className="w-full h-64" />
}
