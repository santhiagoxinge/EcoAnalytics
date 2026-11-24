import { BrainCircuit } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Area, CartesianGrid, ComposedChart, Line, ReferenceDot, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { axisProps, ChartTooltip } from '../components/ChartTooltip'
import { StationSelect, useEffectiveStation } from '../components/StationSelect'
import { Badge, Callout, ErrorState, PageHeader, Panel, Segmented, Skeleton, Stat } from '../components/ui'
import { api } from '../lib/api'
import { aqiCategoryLabel, aqiFill, aqiInk, fmtDateTime, fmtHour, fmtWeekdayHour, num, pm25Category } from '../lib/format'
import type { VistaPronostico } from '../lib/types'
import { useResource } from '../lib/useResource'

export default function ForecastPage() {
  const station = useEffectiveStation(true)
  const [hours, setHours] = useState<24 | 48>(24)
  const res = useResource<VistaPronostico>(station ? `f-${station.id}-${hours}` : null,
    () => api.get(`/pronostico?estacionId=${station!.id}&horas=${hours}`))
  const f = res.data

  const chartData = useMemo(() => f?.pronostico.puntos.map((p) => ({
    fecha: p.fecha, pm25: p.pm25, band: [p.inferior, p.superior] as [number, number],
  })) ?? [], [f])

  return (
    <>
      <PageHeader
        title="Pronóstico de calidad del aire"
        subtitle="Proyección de PM2.5 generada por el modelo de Machine Learning en producción, con su intervalo de confianza del 95 %."
        actions={
          <>
            <StationSelect onlyReporting />
            <Segmented<24 | 48> label="Horizonte" value={hours} onChange={setHours}
              options={[{ value: 24, label: 'Próximas 24 h' }, { value: 48, label: 'Próximas 48 h' }]} />
          </>
        }
      />

      {res.error && !f ? <Panel><ErrorState message={res.error.message} onRetry={res.reload} /></Panel> : !f ? (
        <div className="stack"><div className="grid grid-3"><Skeleton height={150} radius={14} /><Skeleton height={150} radius={14} /><Skeleton height={150} radius={14} /></div><Skeleton height={340} radius={14} /></div>
      ) : (
        <>
          <div className="grid grid-3">
            <section className="panel kpi">
              <p className="stat-label">ICA proyectado · {f.horizonteHoras} h</p>
              <p className="stat-value" style={{ fontSize: 40, color: aqiInk(f.categoriaProyectada) }}>{f.icaProyectado}</p>
              <p className="strong" style={{ color: aqiInk(f.categoriaProyectada) }}>{aqiCategoryLabel[f.categoriaProyectada]}</p>
              <p className="small muted" style={{ marginTop: 8 }}>{f.recomendacion}</p>
            </section>
            <section className="panel kpi">
              <Stat label="Pico máximo previsto" value={num(f.pico.pm25)} unit="µg/m³" color={aqiInk(f.pico.ica.categoria)}
                foot={<>{fmtWeekdayHour(f.pico.fecha)} · IC 95 %: {num(f.pico.inferior)} – {num(f.pico.superior)}</>} />
              <div style={{ marginTop: 10 }}>
                <Badge tone={f.pico.pm25 > f.limiteOms ? 'orange' : 'green'} dot>
                  {f.pico.pm25 > f.limiteOms ? `Supera la guía OMS (${f.limiteOms} µg/m³)` : 'Dentro de la guía OMS'}
                </Badge>
              </div>
            </section>
            <section className="panel kpi">
              <Stat label="Horas sobre la guía OMS" value={f.horasSobreOms} unit={`de ${f.horizonteHoras}`}
                foot={`Límite diario de referencia: ${f.limiteOms} µg/m³`} />
              <p className="small muted row" style={{ gap: 6, marginTop: 10 }}>
                <BrainCircuit size={14} /> Modelo {f.pronostico.versionModelo} · RMSE {num(f.pronostico.rmseModelo, 2)}
              </p>
            </section>
          </div>

          <Panel
            title={`Curva proyectada · ${f.pronostico.estacionNombre}`}
            caption={`Generado ${fmtDateTime(f.pronostico.fechaGeneracion)}. La banda se ensancha con el horizonte: la incertidumbre crece hora a hora.`}
            actions={<div className="legend">
              <span className="legend-chip"><span className="dot" style={{ background: 'var(--series-1)' }} /> PM2.5 esperado</span>
              <span className="legend-chip"><span className="dot" style={{ background: 'var(--accent-soft)', outline: '1px solid var(--series-1)' }} /> IC 95 %</span>
              <span className="legend-chip"><span className="dot" style={{ background: 'var(--orange-fill)' }} /> Guía OMS</span>
            </div>}
          >
            <div style={{ height: 340, opacity: res.loading ? 0.6 : 1, transition: 'opacity 200ms ease' }}>
              <ResponsiveContainer>
                <ComposedChart data={chartData} margin={{ top: 16, right: 12, bottom: 0, left: -12 }}>
                  <CartesianGrid vertical={false} stroke="var(--grid)" />
                  <XAxis dataKey="fecha" tickFormatter={fmtHour} minTickGap={36} {...axisProps} />
                  <YAxis width={48} {...axisProps} />
                  <Tooltip content={<ChartTooltip labelFormatter={(l) => fmtWeekdayHour(String(l))}
                    units={{ pm25: 'µg/m³', band: 'µg/m³' }} />} />
                  <Area dataKey="band" name="IC 95 %" stroke="none" fill="var(--series-1)" fillOpacity={0.12}
                    type="monotone" animationDuration={600} />
                  <ReferenceLine y={f.limiteOms} stroke="var(--orange-fill)" strokeDasharray="5 4"
                    label={{ value: 'OMS 15', position: 'insideTopRight', fill: 'var(--orange)', fontSize: 11 }} />
                  <Line dataKey="pm25" name="PM2.5" stroke="var(--series-1)" strokeWidth={2.5} dot={false}
                    type="monotone" animationDuration={800} />
                  <ReferenceDot x={f.pico.fecha} y={f.pico.pm25} r={6} fill={aqiFill(f.pico.ica.categoria)}
                    stroke="var(--bg-elevated)" strokeWidth={2.5} />
                </ComposedChart>
              </ResponsiveContainer>
            </div>
          </Panel>

          <Panel title="Desglose horario" caption="Desplace horizontalmente para ver todas las horas del horizonte." flush>
            <div className="hourly-strip">
              {f.pronostico.puntos.map((p) => {
                const isPeak = p.fecha === f.pico.fecha
                const cat = pm25Category(p.pm25)
                return (
                  <div key={p.fecha} className="hour-block" data-peak={isPeak}>
                    <div className="small muted">{fmtHour(p.fecha)}</div>
                    <div className="strong" style={{ fontSize: 17, color: aqiInk(cat), marginTop: 2 }}>{num(p.pm25)}</div>
                    <div className="small muted">ICA {p.ica.valor}</div>
                    {isPeak && <div className="small strong" style={{ color: 'var(--orange)', marginTop: 2 }}>Pico</div>}
                  </div>
                )
              })}
            </div>
          </Panel>

          <Callout tone="info" title="Cómo leer este pronóstico">
            Es una estimación estadística, no una certeza. Cada predicción identifica el modelo que la produjo y su error
            esperado; ante decisiones sanitarias, siga las indicaciones de la autoridad ambiental.
          </Callout>
        </>
      )}
    </>
  )
}
