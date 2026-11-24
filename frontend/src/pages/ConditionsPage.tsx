import { Droplets, HeartPulse, MapPin, Thermometer, Wind, Wrench } from 'lucide-react'
import { useEffect } from 'react'
import { Area, AreaChart, CartesianGrid, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { AlertBanner } from '../components/AlertBanner'
import { AqiGauge, AqiScale } from '../components/AqiGauge'
import { axisProps, ChartTooltip } from '../components/ChartTooltip'
import { StationSelect, useEffectiveStation } from '../components/StationSelect'
import { Callout, ErrorState, PageHeader, Panel, Skeleton } from '../components/ui'
import { api } from '../lib/api'
import { aqiFill, aqiInk, fmtHour, fmtWeekdayHour, num, relativeMinutes } from '../lib/format'
import { useStations } from '../lib/stations'
import type { CondicionesActuales, ResumenEstacion } from '../lib/types'
import { useResource } from '../lib/useResource'

const REFRESH_MS = 60_000

export default function ConditionsPage() {
  const station = useEffectiveStation()
  const { select } = useStations()
  const current = useResource<CondicionesActuales>(station ? `c-${station.id}` : null,
    () => api.get(`/condiciones/${station!.id}`))
  const overview = useResource<ResumenEstacion[]>('overview', () => api.get('/condiciones'))

  useEffect(() => {
    const t = setInterval(() => {
      void current.reload()
      void overview.reload()
    }, REFRESH_MS)
    return () => clearInterval(t)
  }, [current.reload, overview.reload])

  const c = current.data
  const others = (overview.data ?? []).filter((s) => s.id !== station?.id)

  return (
    <>
      <AlertBanner />
      <PageHeader
        title="Condiciones actuales"
        subtitle="Índice de Calidad del Aire (ICA) y variables ambientales más recientes de la estación seleccionada."
        actions={
          <>
            <StationSelect />
            {c?.disponible && (
              <span className="badge"><span className="live-dot" /> Actualizado {relativeMinutes(c.minutosTranscurridos)}</span>
            )}
          </>
        }
      />

      {current.error && !c ? <Panel><ErrorState message={current.error.message} onRetry={current.reload} /></Panel> : (
        <div className="grid grid-main-side">
          <div className="stack" style={{ gap: 16 }}>
            <Panel>
              {!c ? <Skeleton height={300} /> : c.disponible ? (
                <div className="conditions-hero">
                  <AqiGauge value={c.ica} category={c.categoria} />
                  <div className="stack" style={{ flex: 1, minWidth: 240, gap: 18 }}>
                    <div>
                      <p className="muted small row" style={{ gap: 6 }}><MapPin size={14} /> {c.zona}</p>
                      <h2 style={{ fontSize: 22, fontWeight: 700, letterSpacing: '-0.02em', marginTop: 2 }}>{c.estacionNombre}</h2>
                      <p className="small muted" style={{ marginTop: 2 }}>Medición de las {fmtHour(c.fechaMedicion!)}</p>
                    </div>
                    <AqiScale value={c.ica} />
                    <div className="advice" style={{ ['--advice' as string]: aqiFill(c.categoria), ['--advice-ink' as string]: aqiInk(c.categoria) }}>
                      <HeartPulse size={18} aria-hidden />
                      <p>{c.recomendacion}</p>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="stack">
                  <Callout tone="warning" title={c.motivoNoDisponible ?? 'Datos no disponibles'}>
                    No se muestran valores desactualizados. Consulte una de las estaciones cercanas con datos activos.
                  </Callout>
                  <div className="row">
                    {others.filter((s) => s.disponible).map((s) => (
                      <button key={s.id} className="btn btn-secondary" onClick={() => select(s.id)}>
                        <span className="dot" style={{ background: aqiFill(s.categoria) }} />
                        {s.nombre} · ICA {s.ica}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </Panel>

            {c?.disponible && (
              <div className="grid grid-4">
                <Metric icon={<Wind size={16} />} label="PM2.5" value={num(c.pm25)} unit="µg/m³" />
                <Metric icon={<Wind size={16} />} label="PM10" value={num(c.pm10)} unit="µg/m³" />
                <Metric icon={<Thermometer size={16} />} label="Temperatura" value={num(c.temperatura)} unit="°C" />
                <Metric icon={<Droplets size={16} />} label="Humedad" value={num(c.humedad, 0)} unit="%" />
              </div>
            )}
          </div>

          <div className="stack" style={{ gap: 16 }}>
            <Panel title="Tendencia PM2.5" caption="Últimas 24 horas">
              {!c ? <Skeleton height={180} /> : c.disponible && c.tendencia24h.length > 1 ? (
                <div style={{ height: 180 }}>
                  <ResponsiveContainer>
                    <AreaChart data={c.tendencia24h} margin={{ top: 8, right: 4, bottom: 0, left: -18 }}>
                      <defs>
                        <linearGradient id="trend-fill" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="0%" stopColor={aqiFill(c.categoria)} stopOpacity={0.3} />
                          <stop offset="100%" stopColor={aqiFill(c.categoria)} stopOpacity={0} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid vertical={false} stroke="var(--grid)" />
                      <XAxis dataKey="fecha" tickFormatter={fmtHour} minTickGap={40} {...axisProps} />
                      <YAxis width={44} {...axisProps} />
                      <ReferenceLine y={15} stroke="var(--text-3)" strokeDasharray="4 4" />
                      <Tooltip content={<ChartTooltip labelFormatter={(l) => fmtWeekdayHour(String(l))} units={{ pm25: 'µg/m³' }} />} />
                      <Area type="monotone" dataKey="pm25" name="PM2.5" stroke={aqiFill(c.categoria)} strokeWidth={2.2}
                        fill="url(#trend-fill)" isAnimationActive={false} />
                    </AreaChart>
                  </ResponsiveContainer>
                </div>
              ) : <p className="muted small">Sin lecturas en las últimas 24 horas.</p>}
              <p className="small muted" style={{ marginTop: 8 }}>Línea punteada: guía diaria OMS de 15 µg/m³.</p>
            </Panel>

            <Panel title="Otras estaciones" flush>
              <ul className="station-list">
                {others.map((s) => (
                  <li key={s.id}>
                    <button onClick={() => select(s.id)}>
                      <span className="dot" style={{ background: s.disponible ? aqiFill(s.categoria) : 'var(--gray-fill)' }} />
                      <span style={{ flex: 1, textAlign: 'left' }}>
                        <span className="strong">{s.nombre}</span>
                        <span className="small muted" style={{ display: 'block' }}>{s.zona}</span>
                      </span>
                      {s.disponible ? (
                        <span className="tabular strong" style={{ color: aqiInk(s.categoria) }}>ICA {s.ica}</span>
                      ) : (
                        <span className="small muted row" style={{ gap: 4 }}><Wrench size={13} /> Sin datos</span>
                      )}
                    </button>
                  </li>
                ))}
              </ul>
            </Panel>
          </div>
        </div>
      )}
    </>
  )
}

function Metric({ icon, label, value, unit }: { icon: React.ReactNode; label: string; value: string; unit: string }) {
  return (
    <div className="panel metric-tile">
      <p className="stat-label row" style={{ gap: 6 }}><span style={{ color: 'var(--accent)' }}>{icon}</span>{label}</p>
      <p className="stat-value">{value}<span className="stat-unit">{unit}</span></p>
    </div>
  )
}
