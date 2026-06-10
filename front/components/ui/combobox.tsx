import * as React from "react"
import { createPortal } from "react-dom"
import { cn } from "@/lib/utils"

export type ComboboxOption = {
  code: string
  name: string
}

type DropdownPosition = {
  top: number
  left: number
  width: number
  maxWidth: number
  maxHeight: number
  placement: "top" | "bottom"
}

export interface ComboboxProps {
  id?: string
  value?: string
  options: ComboboxOption[]
  onChange: (value: string) => void
  placeholder?: string
  emptyText?: string
  allowCustom?: boolean
  disabled?: boolean
  className?: string
}

const Combobox = React.forwardRef<HTMLDivElement, ComboboxProps>(
  (
    {
      id,
      value,
      options,
      onChange,
      placeholder = "请选择或输入",
      emptyText = "暂无选项",
      allowCustom = false,
      disabled,
      className,
    },
    ref
  ) => {
    const [open, setOpen] = React.useState(false)
    const [inputValue, setInputValue] = React.useState("")
    const wrapperRef = React.useRef<HTMLDivElement>(null)
    const inputRef = React.useRef<HTMLInputElement>(null)
    const dropdownRef = React.useRef<HTMLDivElement>(null)
    const frameRef = React.useRef<number | null>(null)
    const [dropdownPosition, setDropdownPosition] = React.useState<DropdownPosition>({
      top: 0,
      left: 0,
      width: 0,
      maxWidth: 0,
      maxHeight: 224,
      placement: "bottom",
    })
    const canUsePortal = typeof document !== "undefined"

    const normalizedOptions = React.useMemo(() => {
      const seen = new Set<string>()
      return options
        .map((option) => ({ code: String(option.code ?? ""), name: String(option.name ?? "") }))
        .filter((option) => {
          const key = `${option.code}\u0000${option.name}`
          if (seen.has(key)) return false
          seen.add(key)
          return option.code !== "" || option.name !== ""
        })
    }, [options])

    const selected = normalizedOptions.find((option) => String(value ?? "") === option.code)

    React.useEffect(() => {
      if (open) return
      setInputValue(selected ? selected.name : String(value ?? ""))
    }, [open, selected, value])

    const updatePosition = React.useCallback(() => {
      if (!inputRef.current) return
      const rect = inputRef.current.getBoundingClientRect()
      const viewportPadding = 8
      const gap = 8
      const maxPanelHeight = 224
      const topPanelMaxHeight = 144
      const minUsefulHeight = 96
      const maxWidth = Math.max(1, window.innerWidth - viewportPadding * 2)
      const width = Math.min(rect.width, maxWidth)
      const left = Math.min(
        Math.max(viewportPadding, rect.left),
        Math.max(viewportPadding, window.innerWidth - width - viewportPadding)
      )
      const spaceBelow = window.innerHeight - rect.bottom - viewportPadding - gap
      const spaceAbove = rect.top - viewportPadding - gap
      const placement = spaceBelow < minUsefulHeight && spaceAbove > spaceBelow ? "top" : "bottom"
      const availableHeight = placement === "top" ? spaceAbove : spaceBelow
      const maxHeightLimit = placement === "top" ? topPanelMaxHeight : maxPanelHeight
      const maxHeight = Math.max(1, Math.min(maxHeightLimit, availableHeight))
      const measuredHeight = dropdownRef.current
        ? Math.min(dropdownRef.current.scrollHeight || maxHeight, maxHeight)
        : maxHeight
      setDropdownPosition({
        top: placement === "top" ? Math.max(viewportPadding, rect.top - gap - measuredHeight) : rect.bottom + gap,
        left,
        width,
        maxWidth,
        maxHeight,
        placement,
      })
    }, [])

    const schedulePositionUpdate = React.useCallback(() => {
      updatePosition()
      if (typeof window === "undefined") return
      if (frameRef.current != null) {
        window.cancelAnimationFrame(frameRef.current)
      }
      frameRef.current = window.requestAnimationFrame(() => {
        frameRef.current = null
        updatePosition()
      })
    }, [updatePosition])

    React.useEffect(() => {
      if (!open) return
      schedulePositionUpdate()
      const handleUpdate = () => schedulePositionUpdate()
      window.addEventListener("scroll", handleUpdate, true)
      window.addEventListener("resize", handleUpdate)
      return () => {
        if (frameRef.current != null) {
          window.cancelAnimationFrame(frameRef.current)
          frameRef.current = null
        }
        window.removeEventListener("scroll", handleUpdate, true)
        window.removeEventListener("resize", handleUpdate)
      }
    }, [open, schedulePositionUpdate])

    React.useEffect(() => {
      if (!open) return

      const handleClickOutside = (event: MouseEvent) => {
        const target = event.target as Node
        const clickedInput = wrapperRef.current?.contains(target)
        const clickedDropdown = dropdownRef.current?.contains(target)
        if (!clickedInput && !clickedDropdown) {
          setOpen(false)
        }
      }

      const handleEscape = (event: KeyboardEvent) => {
        if (event.key === "Escape") {
          setOpen(false)
        }
      }

      setTimeout(() => {
        document.addEventListener("mousedown", handleClickOutside)
        document.addEventListener("keydown", handleEscape)
      }, 0)

      return () => {
        document.removeEventListener("mousedown", handleClickOutside)
        document.removeEventListener("keydown", handleEscape)
      }
    }, [open])

    const filteredOptions = React.useMemo(() => {
      const query = inputValue.trim().toLowerCase()
      if (!query) return normalizedOptions
      return normalizedOptions.filter((option) =>
        `${option.name} ${option.code}`.toLowerCase().includes(query)
      )
    }, [inputValue, normalizedOptions])

    const commitOption = (option: ComboboxOption) => {
      onChange(option.code)
      setInputValue(option.name)
      setOpen(false)
    }

    const commitInput = () => {
      const raw = inputValue.trim()
      const exact = normalizedOptions.find((option) => option.code === raw || option.name === raw)
      if (exact) {
        commitOption(exact)
        return
      }
      if (allowCustom) {
        onChange(raw)
        setInputValue(raw)
        setOpen(false)
      }
    }

    return (
      <div ref={ref}>
        <div ref={wrapperRef} className="relative">
          <input
            ref={inputRef}
            id={id}
            role="combobox"
            aria-expanded={open}
            aria-controls={open ? `${id ?? "combobox"}-listbox` : undefined}
            disabled={disabled}
            value={inputValue}
            placeholder={placeholder}
            onFocus={() => {
              setOpen(true)
              schedulePositionUpdate()
            }}
            onChange={(event) => {
              const nextValue = event.target.value
              setInputValue(nextValue)
              setOpen(true)
              schedulePositionUpdate()
              if (allowCustom) {
                onChange(nextValue)
              }
            }}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                event.preventDefault()
                commitInput()
              }
              if (event.key === "ArrowDown") {
                setOpen(true)
                schedulePositionUpdate()
              }
            }}
            className={cn(
              "h-9 w-full rounded-md border border-input bg-background px-3 py-2 pr-8 text-sm shadow-sm",
              "transition-colors hover:border-primary/40 focus:outline-none focus:ring-2 focus:ring-ring/30 focus:border-ring",
              "disabled:cursor-not-allowed disabled:opacity-50",
              "bg-[url('data:image/svg+xml;utf8,<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"%23a1a1aa\" stroke-width=\"2\"><path d=\"M6 9l6 6 6-6\"/></svg>')] bg-no-repeat bg-[length:16px_16px] bg-[position:right_12px_center]",
              className
            )}
          />

          {open && canUsePortal && createPortal(
            <div
              ref={dropdownRef}
              className="dropdown-panel"
              data-placement={dropdownPosition.placement}
              style={{
                top: `${dropdownPosition.top}px`,
                left: `${dropdownPosition.left}px`,
                width: `${dropdownPosition.width}px`,
                maxWidth: `${dropdownPosition.maxWidth}px`,
                maxHeight: `${dropdownPosition.maxHeight}px`,
                boxSizing: "border-box",
                overflowX: "hidden",
              }}
            >
              <ul id={`${id ?? "combobox"}-listbox`} role="listbox" className="py-1">
                {filteredOptions.length === 0 ? (
                  <li className="px-3 py-2 text-sm text-muted-foreground">
                    {allowCustom && inputValue.trim()
                      ? `按 Enter 使用 ${inputValue.trim()}`
                      : emptyText}
                  </li>
                ) : (
                  filteredOptions.map((option) => {
                    const active = String(value ?? "") === option.code
                    return (
                      <li
                        key={`${option.code}-${option.name}`}
                        role="option"
                        aria-selected={active}
                        className={cn(
                          "group flex cursor-pointer items-center justify-between gap-3 px-3 py-2 text-sm transition-colors",
                          active ? "bg-primary/10 text-primary" : "hover:bg-accent"
                        )}
                        onMouseDown={(event) => event.preventDefault()}
                        onClick={() => commitOption(option)}
                      >
                        <span className="flex min-w-0 items-center gap-3">
                          <span className={cn("inline-flex h-4 w-4 items-center justify-center rounded border border-input", active && "border-primary bg-primary")}></span>
                          <span className="min-w-0 truncate text-sm">{option.name}</span>
                        </span>
                        <span className="shrink-0 text-xs text-muted-foreground">{option.code}</span>
                      </li>
                    )
                  })
                )}
              </ul>
            </div>,
            document.body
          )}
        </div>
      </div>
    )
  }
)

Combobox.displayName = "Combobox"

export { Combobox }
