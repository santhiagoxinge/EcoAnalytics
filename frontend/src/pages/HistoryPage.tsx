import { ChevronLeft, ChevronRight, Download, Inbox } from 'lucide-react'
import { useState } from 'react'
import { Area, CartesianGrid, ComposedChart, Line, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { toast } from 'sonner'
import { axisProps, ChartTooltip } from '../components/ChartTooltip'
import { StationSelect, useEffectiveStation } from '../components/StationSelect'
import { Badge, Button, EmptyState, ErrorState, Field, PageHeader, Panel, Skeleton, Stat } from '../components/ui'
import { api, ApiError, qs } from '../lib/api'
import { daysAgo, fmtDateTime, fmtShortDate, fmtWeekdayHour, int, isoDate, measurementStatusLabel, num, sourceLabel } from '../lib/format'
import type { PaginaHistorico as HistoryData } from '../lib/types'
import { useResource } from '../lib/useResource'

export default function HistoryPage() {
  const station = useEffectiveStation()
  const [from, setFrom] = useState(daysAgo(7))
  const [to, setTo] = useState(isoDate(new Date()))
  const [warnings, setWarnings] = useState(true)
  const [discarded, setDiscarded] = useState(false)
  const [page, setPage] = useState(1)
  const [exporting, setExporting] = useState(false)

  const params = { estacionId: station?.id, desde: from, hasta: to, advertencias: warnings, descartadas: discarded }
  const res = useResource<HistoryData>(station ? `h-${qs(params)}-${page}` : null,
    () => api.get(`/historico?${qs({ ...params, pagina: page })}`))
  const d = res.data
  const daily = d?.granularidad === 'DIARIA'

  const updateFilter = <T,>(setter: (v: T) => void) => (v: T) => {
    setter(v)
    setPage(1)
  }

  const exportCsv = async () => {
    setExporting(true)
    try {
      const name = await api.download(`/historico/exportar?${qs(params)}`)
      toast.success('Exportación lista', { description: name })
    } catch (e) {
      toast.error((e as ApiError).message)
    } finally {
      setExporting(false)
    }
  }

  return (
    <>
      <PageHeader
        title="Explorador de datos históricos"
        subtitle="Registros de calidad del aire por estación y rango de fechas, con su estado de validación."
        actions={<Button variant="secondary" icon={<Download size={16} />} loading={exporting} onClick={exportCsv} disabled={!d}>Exportar CSV</Button>}
      />

      <Panel>
        <div className="filters">
          <Field label="Estación"><StationSelect /></Field>
          <Field label="Desde" htmlFor="h-from">
            <input id="h-from" type="date" className="input" value={from} max={to} onChange={(e) => updateFilter(setFrom)(e.target.value)} />
          </Field>
          <Field label="Hasta" htmlFor="h-to">
            <input id="h-to" type="date" className="input" value={to} min={from} onChange={(e) => updateFilter(setTo)(e.target.value)} />
          </Field>
          <div className="row" style={{ gap: 18, height: 34 }}>
            <label className="check"><input type="checkbox" checked={warnings} onChange={(e) => updateFilter(setWarnings)(e.target.checked)} /> Mostrar advertencias</label>
            <label className="check"><input type="checkbox" checked={discarded} onChange={(e) => updateFilter(setDiscarded)(e.target.checked)} /> Mostrar descartados</label>
          </div>
        </div>
      </Panel>

      {res.error && !d ? <Panel><ErrorState message={res.error.message} onRetry={res.reload} /></Panel> : (
        <>
          <div className="kpi-row">
            <div className="panel kpi"><Stat label="Registros" value={d ? int(d.resumen.total) : '—'} foot={d && `${int(d.resumen.advertencias)} con advertencia · ${int(d.resumen.descartadas)} descartados`} /></div>
            <div className="panel kpi"><Stat label="Promedio PM2.5" value={num(d?.resumen.mediaPm25)} unit="µg/m³" /></div>
            <div className="panel kpi"><Stat label="Máximo PM2.5" value={num(d?.resumen.maxPm25)} unit="µg/m³" /></div>
            <div className="panel kpi"><Stat label="Mínimo PM2.5" value={num(d?.resumen.minPm25)} unit="µg/m³" /></div>
          </div>

          <Panel title={`PM2.5 y PM10 · ${d?.estacionNombre ?? station?.nombre ?? ''}`}
            caption={daily ? 'Promedios diarios del rango seleccionado' : 'Valores horarios del rango seleccionado'}>
            {!d ? <Skeleton height={280} /> : d.grafico.length === 0 ? (
              <EmptyState icon={<Inbox size={28} />} title="Sin registros en este rango">Pruebe ampliar las fechas o elegir otra estación.</EmptyState>
            ) : (
              <div style={{ height: 280, opacity: res.loading ? 0.6 : 1, transition: 'opacity 200ms ease' }}>
                <ResponsiveContainer>
                  <ComposedChart data={d.grafico} margin={{ top: 8, right: 8, bottom: 0, left: -14 }}>
                    <defs>
                      <linearGradient id="hist-fill" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="var(--series-1)" stopOpacity={0.28} />
                        <stop offset="100%" stopColor="var(--series-1)" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid vertical={false} stroke="var(--grid)" />
                    <XAxis dataKey="fecha" tickFormatter={(t) => (daily ? fmtShortDate(t) : fmtWeekdayHour(t))} minTickGap={40} {...axisProps} />
                    <YAxis width={48} {...axisProps} />
                    <Tooltip content={<ChartTooltip labelFormatter={(l) => (daily ? fmtShortDate(String(l)) : fmtWeekdayHour(String(l)))}
                      units={{ pm25: 'µg/m³', pm10: 'µg/m³' }} />} />
                    <Area dataKey="pm25" name="PM2.5" type="monotone" stroke="var(--series-1)" strokeWidth={2.2} fill="url(#hist-fill)" animationDuration={600} />
                    <Line dataKey="pm10" name="PM10" type="monotone" stroke="var(--series-2)" strokeWidth={1.6} dot={false} animationDuration={600} />
                  </ComposedChart>
                </ResponsiveContainer>
              </div>
            )}
          </Panel>

          <Panel title="Registros" caption="Del más reciente al más antiguo" flush>
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Fecha y hora</th><th className="num">PM2.5</th><th className="num">PM10</th>
                    <th className="num">Temp. °C</th><th className="num">Humedad %</th><th className="num">ICA</th>
                    <th>Estado</th><th>Fuente</th>
                  </tr>
                </thead>
                <tbody>
                  {d?.filas.map((r) => (
                    <tr key={r.id}>
                      <td>{fmtDateTime(r.fecha)}</td>
                      <td className="num">{num(r.pm25)}</td>
                      <td className="num">{num(r.pm10)}</td>
                      <td className="num">{num(r.temperatura)}</td>
                      <td className="num">{num(r.humedad, 0)}</td>
                      <td className="num">{r.ica ?? '—'}</td>
                      <td>
                        <Badge tone={r.estado === 'VALIDA' ? 'green' : r.estado === 'ADVERTENCIA' ? 'orange' : 'red'} dot>
                          {measurementStatusLabel[r.estado]}
                        </Badge>
                      </td>
                      <td className="muted">{sourceLabel[r.fuente]}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {d && d.filas.length > 0 && (
              <div className="pagination">
                <span className="muted">Página {d.pagina} de {d.totalPaginas} · {int(d.resumen.total)} registros</span>
                <div className="row">
                  <Button size="sm" variant="secondary" icon={<ChevronLeft size={15} />} disabled={d.pagina <= 1} onClick={() => setPage(d.pagina - 1)}>Anterior</Button>
                  <Button size="sm" variant="secondary" disabled={d.pagina >= d.totalPaginas} onClick={() => setPage(d.pagina + 1)}>Siguiente <ChevronRight size={15} /></Button>
                </div>
              </div>
            )}
          </Panel>
        </>
      )}
    </>
  )
}
