import { aqiCategoryLabel, aqiFill, aqiInk } from '../lib/format'
import type { CategoriaIca } from '../lib/types'

const SIZE = 220
const STROKE = 16
const RADIUS = (SIZE - STROKE) / 2 - 6
const SWEEP = 0.75 // 270° arc, open at the bottom like an Apple Watch gauge
const CIRCUMFERENCE = 2 * Math.PI * RADIUS
const ARC = CIRCUMFERENCE * SWEEP

/** AQI gauge. The arc length and colour transition together when the station changes. */
export function AqiGauge({ value, category, label = 'ICA' }: { value: number | null; category: CategoriaIca | null; label?: string }) {
  const progress = value === null ? 0 : Math.min(value, 300) / 300
  const fill = aqiFill(category)

  return (
    <div style={{ position: 'relative', width: SIZE, height: SIZE }} role="img"
      aria-label={value === null ? 'Índice de calidad del aire no disponible' : `Índice de calidad del aire ${value}, ${aqiCategoryLabel[category!]}`}>
      <svg width={SIZE} height={SIZE} viewBox={`0 0 ${SIZE} ${SIZE}`} style={{ transform: 'rotate(135deg)' }}>
        <circle cx={SIZE / 2} cy={SIZE / 2} r={RADIUS} fill="none" stroke="var(--surface-sunken)" strokeWidth={STROKE}
          strokeLinecap="round" strokeDasharray={`${ARC} ${CIRCUMFERENCE}`} />
        <circle cx={SIZE / 2} cy={SIZE / 2} r={RADIUS} fill="none" stroke={fill} strokeWidth={STROKE}
          strokeLinecap="round" strokeDasharray={`${ARC} ${CIRCUMFERENCE}`}
          strokeDashoffset={ARC * (1 - progress)}
          style={{ transition: 'stroke-dashoffset 900ms cubic-bezier(0.23, 1, 0.32, 1), stroke 400ms ease' }} />
      </svg>
      <div style={{ position: 'absolute', inset: 0, display: 'grid', placeItems: 'center', textAlign: 'center' }}>
        <div>
          <div className="stat-label">{label}</div>
          <div className="tabular" style={{
            fontFamily: 'var(--font-display)', fontSize: 60, fontWeight: 700, letterSpacing: '-0.04em', lineHeight: 1,
            color: aqiInk(category), transition: 'color 400ms ease', marginTop: 2,
          }}>
            {value ?? '—'}
          </div>
          <div style={{ fontSize: 15, fontWeight: 600, color: aqiInk(category), marginTop: 4, maxWidth: 140, transition: 'color 400ms ease' }}>
            {category ? aqiCategoryLabel[category] : 'Sin datos'}
          </div>
        </div>
      </div>
    </div>
  )
}

const SCALE: { category: CategoriaIca; to: number }[] = [
  { category: 'BUENA', to: 50 },
  { category: 'ACEPTABLE', to: 100 },
  { category: 'DANINA_SENSIBLES', to: 150 },
  { category: 'DANINA', to: 200 },
  { category: 'MUY_DANINA', to: 300 },
  { category: 'PELIGROSA', to: 500 },
]

/** Horizontal AQI scale with a marker at the current value. */
export function AqiScale({ value }: { value: number | null }) {
  const position = value === null ? null : Math.min(value, 300) / 300
  return (
    <div style={{ width: '100%' }}>
      <div style={{ position: 'relative', display: 'flex', gap: 2, height: 8 }}>
        {SCALE.slice(0, 5).map((s) => (
          <div key={s.category} style={{ flex: 1, background: aqiFill(s.category), borderRadius: 4 }} />
        ))}
        {position !== null && (
          <div style={{
            position: 'absolute', top: -4, left: `calc(${position * 100}% - 8px)`, width: 16, height: 16, borderRadius: '50%',
            background: 'var(--bg-elevated)', border: '3px solid var(--text)', boxShadow: 'var(--shadow-sm)',
            transition: 'left 900ms cubic-bezier(0.23, 1, 0.32, 1)',
          }} />
        )}
      </div>
      <div className="row between small muted tabular" style={{ marginTop: 6 }}>
        <span>0</span><span>50</span><span>100</span><span>150</span><span>200</span><span>300+</span>
      </div>
    </div>
  )
}
