import { num } from '../lib/format'

interface Entry {
  name?: string | number
  value?: number | string | (number | string)[] | null
  color?: string
  dataKey?: string | number | ((obj: unknown) => unknown)
}

/** Shared tooltip for Recharts: title plus one row per visible series. */
export function ChartTooltip({ active, payload, label, labelFormatter, units = {}, hide = [] }: {
  active?: boolean
  payload?: Entry[]
  label?: string | number
  labelFormatter?: (label: string | number) => string
  units?: Record<string, string>
  hide?: string[]
}) {
  if (!active || !payload?.length) return null
  const rows = payload.filter((p) => p.value !== null && p.value !== undefined && !hide.includes(String(p.dataKey)))
  if (!rows.length) return null
  return (
    <div className="chart-tooltip">
      {label !== undefined && <div className="chart-tooltip-title">{labelFormatter ? labelFormatter(label) : label}</div>}
      {rows.map((p) => (
        <div className="chart-tooltip-row" key={String(p.dataKey)}>
          <span><span className="dot" style={{ background: p.color }} />{p.name}</span>
          <strong>
            {Array.isArray(p.value) ? `${num(Number(p.value[0]))} – ${num(Number(p.value[1]))}` : num(Number(p.value))}
            {units[String(p.dataKey)] ? ` ${units[String(p.dataKey)]}` : ''}
          </strong>
        </div>
      ))}
    </div>
  )
}

export const axisProps = {
  stroke: 'var(--grid)',
  tickLine: false,
  axisLine: false,
  tick: { fill: 'var(--text-3)', fontSize: 11 },
} as const
