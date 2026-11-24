import { useMemo, useState } from 'react'
import {
  CartesianGrid, ComposedChart, Line, ResponsiveContainer, Scatter, ScatterChart, Tooltip, XAxis, YAxis, ZAxis,
} from 'recharts'
import { axisProps, ChartTooltip } from '../components/ChartTooltip'
import { StationSelect, useEffectiveStation } from '../components/StationSelect'
import { ErrorState, PageHeader, Panel, Segmented, Skeleton } from '../components/ui'
import { api } from '../lib/api'
import { fmtWeekdayHour, num } from '../lib/format'
import type { Visualizacion } from '../lib/types'
import { useResource } from '../lib/useResource'

type SeriesKey = 'pm25' | 'pm10' | 'humedad' | 'temperatura'

const SERIES: { key: SeriesKey; label: string; color: string; unit: string; axis: 'left' | 'right' }[] = [
  { key: 'pm25', label: 'PM2.5', color: 'var(--series-1)', unit: 'µg/m³', axis: 'left' },
  { key: 'pm10', label: 'PM10', color: 'var(--series-2)', unit: 'µg/m³', axis: 'left' },
  { key: 'humedad', label: 'Humedad', color: 'var(--series-3)', unit: '%', axis: 'right' },
  { key: 'temperatura', label: 'Temperatura', color: 'var(--series-4)', unit: '°C', axis: 'right' },
]

const DAYS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']

export default function VisualizationPage() {
  const station = useEffectiveStation()
  const [days, setDays] = useState(3)
  const [mode, setMode] = useState<'multi' | 'correlation'>('multi')
  const [visible, setVisible] = useState<Record<SeriesKey, boolean>>({ pm25: true, pm10: true, humedad: true, temperatura: false })
  const res = useResource<Visualizacion>(station ? `v-${station.id}-${days}` : null,
    () => api.get(`/visualizacion?estacionId=${station!.id}&dias=${days}`))
  const v = res.data

  // Sequential scale clipped at the 95th percentile so a single extreme hour does not wash out the pattern.
  const scale = useMemo(() => {
    const values = (v?.mapaCalor.celdas.flat() ?? []).filter((x): x is number => x !== null).sort((a, b) => a - b)
    if (!values.length) return { min: 0, max: 1 }
    return { min: values[0], max: values[Math.floor((values.length - 1) * 0.95)] }
  }, [v])

  const regression = useMemo(() => {
    if (!v || v.correlacion.puntos.length < 2) return []
    const xs = v.correlacion.puntos.map((p) => p.temperatura)
    const min = Math.min(...xs)
    const max = Math.max(...xs)
    const { pendiente, intercepto } = v.correlacion
    return [{ temperatura: min, pm25: pendiente * min + intercepto }, { temperatura: max, pm25: pendiente * max + intercepto }]
  }, [v])

  return (
    <>
      <PageHeader
        title="Visualización gráfica"
        subtitle="Curvas multivariable con doble eje, correlación entre temperatura y PM2.5, y matriz de calor semanal."
        actions={
          <>
            <StationSelect />
            <Segmented<'multi' | 'correlation'> label="Modo" value={mode} onChange={setMode}
              options={[{ value: 'multi', label: 'Multivariable' }, { value: 'correlation', label: 'Correlación' }]} />
          </>
        }
      />

      {res.error && !v ? <Panel><ErrorState message={res.error.message} onRetry={res.reload} /></Panel> : mode === 'multi' ? (
        <Panel
          title="Series temporales"
          caption="Eje izquierdo: material particulado (µg/m³). Eje derecho: humedad (%) y temperatura (°C). Toque una etiqueta para mostrar u ocultar la serie."
          actions={<Segmented<number> label="Rango" value={days} onChange={setDays}
            options={[{ value: 1, label: '24 h' }, { value: 3, label: '3 días' }, { value: 7, label: '7 días' }]} />}
        >
          <div className="legend" style={{ marginBottom: 12 }}>
            {SERIES.map((s) => (
              <button key={s.key} type="button" className="legend-chip" aria-pressed={visible[s.key]}
                onClick={() => setVisible((prev) => ({ ...prev, [s.key]: !prev[s.key] }))}>
                <span className="dot" style={{ background: s.color }} />{s.label}
              </button>
            ))}
          </div>
          {!v ? <Skeleton height={360} /> : (
            <div style={{ height: 360, opacity: res.loading ? 0.6 : 1, transition: 'opacity 200ms ease' }}>
              <ResponsiveContainer>
                <ComposedChart data={v.serie} margin={{ top: 8, right: 0, bottom: 0, left: -14 }}>
                  <CartesianGrid vertical={false} stroke="var(--grid)" />
                  <XAxis dataKey="fecha" tickFormatter={fmtWeekdayHour} minTickGap={50} {...axisProps} />
                  <YAxis yAxisId="left" width={48} {...axisProps} />
                  <YAxis yAxisId="right" orientation="right" width={40} {...axisProps} />
                  <Tooltip content={<ChartTooltip labelFormatter={(l) => fmtWeekdayHour(String(l))}
                    units={Object.fromEntries(SERIES.map((s) => [s.key, s.unit]))} />} />
                  {SERIES.filter((s) => visible[s.key]).map((s) => (
                    <Line key={s.key} yAxisId={s.axis} dataKey={s.key} name={s.label} stroke={s.color} type="monotone"
                      strokeWidth={s.key === 'pm25' ? 2.4 : 1.8} dot={false} activeDot={{ r: 4.5, strokeWidth: 2, stroke: 'var(--bg-elevated)' }}
                      animationDuration={600} />
                  ))}
                </ComposedChart>
              </ResponsiveContainer>
            </div>
          )}
        </Panel>
      ) : (
        <Panel
          title="Correlación temperatura vs. PM2.5"
          caption="Últimas 4 semanas. La línea muestra la tendencia por mínimos cuadrados."
          actions={v && (
            <span className="badge badge-blue tabular">r de Pearson = {num(v.correlacion.pearsonR, 2)}</span>
          )}
        >
          {!v ? <Skeleton height={360} /> : (
            <>
              <div style={{ height: 360 }}>
                <ResponsiveContainer>
                  <ScatterChart margin={{ top: 8, right: 8, bottom: 8, left: -14 }}>
                    <CartesianGrid stroke="var(--grid)" />
                    <XAxis type="number" dataKey="temperatura" name="Temperatura" unit=" °C" domain={['auto', 'auto']} {...axisProps} />
                    <YAxis type="number" dataKey="pm25" name="PM2.5" width={48} {...axisProps} />
                    <ZAxis range={[22, 22]} />
                    <Tooltip cursor={{ strokeDasharray: '3 3' }} content={({ active, payload }) => active && payload?.length ? (
                      <div className="chart-tooltip">
                        <div className="chart-tooltip-row"><span>Temperatura</span><strong>{num(Number(payload[0].payload.temperatura))} °C</strong></div>
                        <div className="chart-tooltip-row"><span>PM2.5</span><strong>{num(Number(payload[0].payload.pm25))} µg/m³</strong></div>
                      </div>
                    ) : null} />
                    <Scatter data={v.correlacion.puntos} fill="var(--series-1)" fillOpacity={0.45} animationDuration={600} />
                    <Scatter data={regression} line={{ stroke: 'var(--series-4)', strokeWidth: 2.5 }} shape={() => <g />} legendType="none" />
                  </ScatterChart>
                </ResponsiveContainer>
              </div>
              <p className="small muted" style={{ marginTop: 8 }}>
                {Math.abs(v.correlacion.pearsonR) < 0.3 ? 'Correlación débil' : Math.abs(v.correlacion.pearsonR) < 0.6 ? 'Correlación moderada' : 'Correlación fuerte'}
                {v.correlacion.pearsonR < 0 ? ' e inversa: a mayor temperatura, menor concentración (mayor dispersión vertical).' : ' y directa.'}
              </p>
            </>
          )}
        </Panel>
      )}

      <Panel title="Matriz de calor semanal" caption="Concentración media de PM2.5 por día de la semana y hora (últimas 4 semanas).">
        {!v ? <Skeleton height={200} /> : (
          <div className="table-wrap">
            <div className="heatmap" style={{ minWidth: 560 }} role="table" aria-label="PM2.5 medio por día y hora">
              <span />
              {Array.from({ length: 24 }, (_, h) => (
                <span key={h} className="tabular" style={{ textAlign: 'center' }}>{h % 3 === 0 ? h : ''}</span>
              ))}
              {v.mapaCalor.celdas.map((row, d) => (
                <HeatRow key={d} label={DAYS[d]} row={row} scale={scale} />
              ))}
            </div>
            <div className="row small muted tabular" style={{ marginTop: 12, gap: 10 }}>
              <span>{num(scale.min)} µg/m³</span>
              <span className="heat-legend" aria-hidden />
              <span>≥ {num(scale.max)} µg/m³</span>
            </div>
          </div>
        )}
      </Panel>
    </>
  )
}

function HeatRow({ label, row, scale }: { label: string; row: (number | null)[]; scale: { min: number; max: number } }) {
  return (
    <>
      <span style={{ alignSelf: 'center', fontWeight: 600, color: 'var(--text-2)' }}>{label}</span>
      {row.map((value, h) => {
        const t = value === null ? 0 : Math.min(1, Math.max(0, (value - scale.min) / Math.max(1e-6, scale.max - scale.min)))
        return (
          <span key={h} className="heatmap-cell" title={value === null ? `${label} ${h}:00 · sin datos` : `${label} ${h}:00 · ${num(value)} µg/m³`}
            style={{ background: value === null ? 'var(--surface-sunken)' : `color-mix(in oklab, var(--heat-high) ${Math.round(8 + t * 92)}%, var(--heat-low))` }} />
        )
      })}
    </>
  )
}
