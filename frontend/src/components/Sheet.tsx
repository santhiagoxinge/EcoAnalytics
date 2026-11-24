import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { useEffect, useRef, type ReactNode } from 'react'

/**
 * Modal sheet. Enters with a short scale+fade from center (modals are not anchored to a trigger)
 * and leaves faster than it arrives.
 */
export function Sheet({ open, onClose, title, children, footer, width }: {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
  footer?: ReactNode
  width?: number
}) {
  const reduce = useReducedMotion()
  const panelRef = useRef<HTMLDivElement>(null)
  const restoreFocus = useRef<HTMLElement | null>(null)
  const closeRef = useRef(onClose)
  closeRef.current = onClose

  useEffect(() => {
    if (!open) return
    restoreFocus.current = document.activeElement as HTMLElement | null
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') closeRef.current()
    }
    document.addEventListener('keydown', onKey)
    const t = setTimeout(() => {
      panelRef.current?.querySelector<HTMLElement>('input, select, textarea, button')?.focus()
    }, 30)
    return () => {
      document.removeEventListener('keydown', onKey)
      clearTimeout(t)
      restoreFocus.current?.focus?.()
    }
  }, [open])

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="sheet-backdrop"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1, transition: { duration: 0.2 } }}
          exit={{ opacity: 0, transition: { duration: 0.15 } }}
          onMouseDown={(e) => {
            if (e.target === e.currentTarget) onClose()
          }}
        >
          <motion.div
            ref={panelRef}
            className="sheet"
            role="dialog"
            aria-modal="true"
            aria-label={title}
            style={width ? { width: `min(${width}px, 100%)` } : undefined}
            initial={reduce ? { opacity: 0 } : { opacity: 0, transform: 'scale(0.96) translateY(8px)' }}
            animate={{ opacity: 1, transform: 'scale(1) translateY(0px)', transition: { duration: 0.24, ease: [0.23, 1, 0.32, 1] } }}
            exit={reduce ? { opacity: 0 } : { opacity: 0, transform: 'scale(0.98) translateY(4px)', transition: { duration: 0.14, ease: [0.23, 1, 0.32, 1] } }}
          >
            <header className="sheet-header">
              <h2 className="sheet-title">{title}</h2>
            </header>
            <div className="sheet-body">{children}</div>
            {footer && <footer className="sheet-footer">{footer}</footer>}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
