import { motion } from 'motion/react'
import { AlertTriangle, CheckCircle2, Info, OctagonAlert } from 'lucide-react'
import { useId, type ButtonHTMLAttributes, type ReactNode } from 'react'

type ButtonVariant = 'primary' | 'secondary' | 'plain' | 'danger' | 'success' | 'default'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
  icon?: ReactNode
}

export function Button({ variant = 'default', size = 'md', loading, icon, children, className = '', disabled, ...rest }: ButtonProps) {
  const classes = ['btn', variant !== 'default' && `btn-${variant}`, size !== 'md' && `btn-${size}`, className]
    .filter(Boolean).join(' ')
  return (
    <button className={classes} disabled={disabled || loading} aria-busy={loading || undefined} {...rest}>
      {loading ? <span className="spinner" aria-hidden /> : icon}
      {children}
    </button>
  )
}

export function Panel({ title, caption, actions, children, flush, className = '' }: {
  title?: ReactNode
  caption?: ReactNode
  actions?: ReactNode
  children: ReactNode
  flush?: boolean
  className?: string
}) {
  return (
    <section className={`panel ${className}`}>
      {(title || actions) && (
        <header className="panel-header">
          <div>
            {title && <h2 className="panel-title">{title}</h2>}
            {caption && <p className="panel-caption">{caption}</p>}
          </div>
          {actions && <div className="row">{actions}</div>}
        </header>
      )}
      <div className={`panel-body ${flush ? 'flush' : ''}`}>{children}</div>
    </section>
  )
}

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: ReactNode; actions?: ReactNode }) {
  return (
    <div className="page-header">
      <div>
        <h1 className="page-title">{title}</h1>
        {subtitle && <p className="page-subtitle">{subtitle}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </div>
  )
}

export function Field({ label, error, hint, children, htmlFor }: {
  label: string
  error?: string
  hint?: string
  children: ReactNode
  htmlFor?: string
}) {
  return (
    <div className="field">
      <label className="field-label" htmlFor={htmlFor}>{label}</label>
      {children}
      {error ? <span className="field-error" role="alert">{error}</span> : hint ? <span className="field-hint">{hint}</span> : null}
    </div>
  )
}

export function Segmented<T extends string | number>({ value, options, onChange, label }: {
  value: T
  options: { value: T; label: string }[]
  onChange: (value: T) => void
  label: string
}) {
  const id = useId()
  return (
    <div className="segmented" role="group" aria-label={label}>
      {options.map((o) => (
        <button key={String(o.value)} type="button" aria-pressed={o.value === value} onClick={() => onChange(o.value)}>
          {o.value === value && (
            <motion.span
              layoutId={`seg-${id}`}
              className="segmented-thumb"
              transition={{ type: 'spring', duration: 0.32, bounce: 0.12 }}
            />
          )}
          {o.label}
        </button>
      ))}
    </div>
  )
}

export function Switch({ checked, onChange, label, disabled }: {
  checked: boolean
  onChange: (checked: boolean) => void
  label: string
  disabled?: boolean
}) {
  return (
    <button
      type="button"
      role="switch"
      className="switch"
      aria-checked={checked}
      aria-label={label}
      disabled={disabled}
      onClick={() => onChange(!checked)}
    />
  )
}

export type BadgeTone = 'neutral' | 'green' | 'orange' | 'red' | 'blue' | 'purple'

export function Badge({ tone = 'neutral', children, dot }: { tone?: BadgeTone; children: ReactNode; dot?: boolean }) {
  return (
    <span className={`badge ${tone !== 'neutral' ? `badge-${tone}` : ''}`}>
      {dot && <span className="dot" aria-hidden />}
      {children}
    </span>
  )
}

export function EmptyState({ icon, title, children, action }: { icon?: ReactNode; title: string; children?: ReactNode; action?: ReactNode }) {
  return (
    <div className="empty">
      {icon}
      <p className="empty-title">{title}</p>
      {children && <p className="small" style={{ maxWidth: '46ch' }}>{children}</p>}
      {action}
    </div>
  )
}

export function Skeleton({ height = 16, width = '100%', radius }: { height?: number | string; width?: number | string; radius?: number }) {
  return <span className="skeleton" style={{ display: "block", height, width, borderRadius: radius }} aria-hidden />
}

const calloutIcons = {
  info: Info,
  warning: AlertTriangle,
  danger: OctagonAlert,
  success: CheckCircle2,
}

export function Callout({ tone = 'info', title, children }: { tone?: keyof typeof calloutIcons; title?: string; children?: ReactNode }) {
  const Icon = calloutIcons[tone]
  return (
    <div className={`callout callout-${tone}`} role={tone === 'danger' || tone === 'warning' ? 'alert' : 'status'}>
      <Icon size={17} aria-hidden />
      <div>
        {title && <p className="callout-title">{title}</p>}
        {children && <p>{children}</p>}
      </div>
    </div>
  )
}

export function Stat({ label, value, unit, foot, color }: {
  label: string
  value: ReactNode
  unit?: string
  foot?: ReactNode
  color?: string
}) {
  return (
    <div>
      <p className="stat-label">{label}</p>
      <p className="stat-value" style={color ? { color } : undefined}>
        {value}
        {unit && <span className="stat-unit">{unit}</span>}
      </p>
      {foot && <p className="stat-foot">{foot}</p>}
    </div>
  )
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <EmptyState icon={<OctagonAlert size={28} />} title="No fue posible cargar la información" action={
      onRetry && <Button variant="secondary" size="sm" onClick={onRetry}>Reintentar</Button>
    }>
      {message}
    </EmptyState>
  )
}
