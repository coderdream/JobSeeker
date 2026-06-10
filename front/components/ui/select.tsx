import * as React from "react"
import { createPortal } from "react-dom"
import { cn } from "@/lib/utils"

type OptionItem = { value: string; label: React.ReactNode }
type SelectOptionElement = React.ReactElement<React.OptionHTMLAttributes<HTMLOptionElement>, "option">
type DropdownPosition = {
  top: number
  left: number
  width: number
  maxWidth: number
  maxHeight: number
  placement: "top" | "bottom"
}

export interface SelectProps {
  value?: string
  onChange?: (e: { target: { value: string } }) => void
  placeholder?: string
  className?: string
  id?: string
  disabled?: boolean
  children?: React.ReactNode
}

const Select = React.forwardRef<HTMLDivElement, SelectProps>(
  ({ className, children, value, onChange, placeholder, disabled, id, ...props }, ref) => {
    const [open, setOpen] = React.useState(false)
    const wrapperRef = React.useRef<HTMLDivElement>(null)
    const buttonRef = React.useRef<HTMLButtonElement>(null)
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
    const canUsePortal = typeof document !== 'undefined'

    const options = React.useMemo<OptionItem[]>(() => {
      return React.Children.toArray(children)
        .filter((c): c is SelectOptionElement => React.isValidElement(c) && c.type === 'option')
        .map((c) => ({ value: String(c.props.value ?? c.props.children), label: c.props.children }))
    }, [children])

    const selected = options.find((o) => String(value ?? '') === String(o.value))

    const emitChange = (val: string) => onChange?.({ target: { value: val } })

    // 计算下拉框位置
    const updatePosition = React.useCallback(() => {
      if (buttonRef.current) {
        const rect = buttonRef.current.getBoundingClientRect()
        const viewportPadding = 8
        const gap = 8
        const maxPanelHeight = 224
        const topPanelMaxHeight = 144
        const minUsefulHeight = 96
        const maxWidth = Math.max(1, window.innerWidth - viewportPadding * 2)
        const minPanelWidth = Math.min(220, maxWidth)
        const width = Math.min(Math.max(rect.width, minPanelWidth), maxWidth)
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
      }
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

    // 打开时计算位置
    React.useEffect(() => {
      if (open) {
        schedulePositionUpdate()
        // 监听滚动和窗口大小变化，更新位置
        const handleUpdate = () => schedulePositionUpdate()
        window.addEventListener('scroll', handleUpdate, true)
        window.addEventListener('resize', handleUpdate)
        return () => {
          if (frameRef.current != null) {
            window.cancelAnimationFrame(frameRef.current)
            frameRef.current = null
          }
          window.removeEventListener('scroll', handleUpdate, true)
          window.removeEventListener('resize', handleUpdate)
        }
      }
    }, [open, schedulePositionUpdate])

    // 点击外部关闭下拉框
    React.useEffect(() => {
      const handleClickOutside = (event: MouseEvent) => {
        const target = event.target as Node
        // 检查点击是否在按钮或下拉框内
        const clickedButton = wrapperRef.current?.contains(target)
        const clickedDropdown = dropdownRef.current?.contains(target)

        if (!clickedButton && !clickedDropdown) {
          setOpen(false)
        }
      }

      const handleEscape = (event: KeyboardEvent) => {
        if (event.key === 'Escape') {
          setOpen(false)
        }
      }

      if (open) {
        // 使用 setTimeout 确保 DOM 已更新
        setTimeout(() => {
          document.addEventListener('mousedown', handleClickOutside)
          document.addEventListener('keydown', handleEscape)
        }, 0)
      }

      return () => {
        document.removeEventListener('mousedown', handleClickOutside)
        document.removeEventListener('keydown', handleEscape)
      }
    }, [open])

    return (
      <div ref={ref} {...props}>
        <div ref={wrapperRef} className="relative">
          <button
            ref={buttonRef}
            id={id as string}
            type="button"
            disabled={disabled}
            onClick={() => {
              const nextOpen = !open
              setOpen(nextOpen)
              if (nextOpen) schedulePositionUpdate()
            }}
            className={cn(
              "flex h-9 w-full items-center rounded-md border border-input bg-background px-3 py-2 pr-8 text-left text-sm shadow-sm",
              "transition-colors hover:border-primary/40",
              disabled ? "cursor-not-allowed opacity-50" : "focus:outline-none focus:ring-2 focus:ring-ring/30 focus:border-ring",
              "bg-[url('data:image/svg+xml;utf8,<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"%23a1a1aa\" stroke-width=\"2\"><path d=\"M6 9l6 6 6-6\"/></svg>')] bg-no-repeat bg-[length:16px_16px] bg-[position:right_12px_center]",
              className
            )}
          >
            <span className="min-w-0 truncate text-sm">{selected ? selected.label : (placeholder ?? '')}</span>
          </button>

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
              <ul className="py-1">
                {options.map((o) => {
                  const active = String(value ?? '') === String(o.value)
                  return (
                    <li
                      key={String(o.value)}
                      className={cn(
                        "group flex cursor-pointer items-center justify-between gap-3 px-3 py-2 text-sm transition-colors",
                        active ? "bg-primary/10 text-primary" : "hover:bg-accent"
                      )}
                      onClick={() => {
                        emitChange(String(o.value))
                        setOpen(false)
                      }}
                    >
                      <span className="flex min-w-0 items-start gap-3">
                        <span className={cn("mt-0.5 inline-flex h-4 w-4 shrink-0 items-center justify-center rounded border border-input", active && "border-primary bg-primary")}></span>
                        <span className="min-w-0 whitespace-normal break-words text-sm leading-5">{o.label}</span>
                      </span>
                    </li>
                  )
                })}
              </ul>
            </div>,
            document.body
          )}
        </div>
      </div>
    )
  }
)
Select.displayName = "Select"

export { Select }
