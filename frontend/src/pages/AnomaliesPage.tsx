import { BellRing, SlidersHorizontal, X } from 'lucide-react'
import { useMemo, useState } from 'react'
import { CartesianGrid, ComposedChart, Line, ReferenceDot, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { toast } from 'sonner'
import { axisProps, ChartTooltip } from '../components/ChartTooltip'
import { Sheet } from '../components/Sheet'
import { StationSelect, useEffectiveStation } from '../components/StationSelect'
import { Badge, Button, ErrorState, Field, PageHeader, Panel, Segmented, Skeleton, type BadgeTone } from '../components/ui'
import { api, ApiError } from '../lib/api'
import { useAuth } from '../lib/auth'
import { anomalyStatusLabel, fmtDateTime, fmtWeekdayHour, int, num, severityLabel } from '../lib/format'
import type { Anomalia as Anomaly, AnomaliasRespuesta as AnomaliesResponse, SeveridadAnomalia as AnomalySeverity } from '../lib/types'
import { useResource } from '../lib/useResource'

const severityTone: Record<AnomalySeverity, BadgeTone> = { MODERADA: 'orange', ALTA: 'red', CRITICA: 'red' }

export default function AnomaliesPage() {
  const { hasRole } = useAuth()
  const station = useEffectiveStation()
  const [filter, setFilter] = useState<'PENDIENTE' | 'ALL'>('PENDIENTE')
  const [configuring, setConfiguring] = useState(false)
  const [busy, setBusy] = useState<number | null>(null)
  const list = useResource<AnomaliesResponse>('anomalies', () => api.get('/anomalias'))
  const timeline = useResource<{ fecha: string; pm25: number; puntajeZ: number | null }[]>(
    station ? `tl-${station.id}` : null, () => api.get(`/anomalias/linea-tiempo?estacionId=${station!.id}&horas=168`))

  const flagged = useMemo(() => (timeline.data ?? []).filter((p) => p.puntajeZ !== null), [timeline.data])

  const act = async (a: Anomaly, action: 'notificar' | 'descartar') => {
    setBusy(a.id)
    try {
      const updated = await api.post<Anomaly>(`/anomalias/${a.id}/${action}`)
      list.setData((d) => d && {
        anomalias: d.anomalias.map((x) => (x.id === updated.id ? updated : x)),
        estadisticas: {
          ...d.estadisticas,
          pendientes: d.estadisticas.pendientes - 1,
          notificadas: d.estadisticas.notificadas + (action === 'notificar' ? 1 : 0),
          descartadas: d.estadisticas.descartadas + (action === 'descartar' ? 1 : 0),
        },
      })
      if (action === 'notificar') toast.success('Alerta notificada', { description: `Difundida a los canales configurados (${a.estacionNombre}).` })
      else toast('Descartado como falso positivo', { description: 'No se envió ninguna alerta a la ciudadanía.' })
    } catch (e) {
      toast.error((e as ApiError).message)
    } finally {
      setBusy(null)
    }
  }

  const d = list.data
  const rows = (d?.anomalias ?? []).filter((a) => filter === 'ALL' || a.estado === 'PENDIENTE')

  return (
    <>
      <PageHeader
        title="Detección de anomalías"
        subtitle={`Picos atípicos frente a la media móvil de 7 días de cada estación (Z-score). Umbral actual: ${d ? num(d.estadisticas.umbral) : '—'} σ.`}
        actions={hasRole('ADMINISTRADOR') && (
          <Button variant="secondary" icon={<SlidersHorizontal size={16} />} onClick={() => setConfiguring(true)} disabled={!d}>Configurar umbral</Button>
        )}
      />

      <div className="kpi-row">
        {[['Pendientes de validación', d?.estadisticas.pendientes], ['Alertas notificadas', d?.estadisticas.notificadas], ['Falsos positivos', d?.estadisticas.descartadas]].map(([label, value]) => (
          <div className="panel kpi" key={label as string}>
            <p className="stat-label">{label}</p>
            <p className="stat-value">{d ? int(value as number) : '—'}</p>
          </div>
        ))}
      </div>

      <Panel title="Línea de tiempo PM2.5" caption="Últimos 7 días. Los puntos rojos marcan lecturas clasificadas como anomalía." actions={<StationSelect />}>
        {!timeline.data ? <Skeleton height={240} /> : (
          <div style={{ height: 240 }}>
            <ResponsiveContainer>
              <ComposedChart data={timeline.data} margin={{ top: 12, right: 8, bottom: 0, left: -14 }}>
                <CartesianGrid vertical={false} stroke="var(--grid)" />
                <XAxis dataKey="fecha" tickFormatter={fmtWeekdayHour} minTickGap={60} {...axisProps} />
                <YAxis width={48} {...axisProps} />
                <Tooltip content={<ChartTooltip labelFormatter={(l) => fmtWeekdayHour(String(l))} units={{ pm25: 'µg/m³' }} hide={['puntajeZ']} />} />
                <Line dataKey="pm25" name="PM2.5" stroke="var(--series-1)" strokeWidth={1.8} dot={false} type="monotone" animationDuration={600} />
                {flagged.map((p) => (
                  <ReferenceDot key={p.fecha} x={p.fecha} y={p.pm25} r={5.5} fill="var(--red-fill)" stroke="var(--bg-elevated)" strokeWidth={2} />
                ))}
              </ComposedChart>
            </ResponsiveContainer>
          </div>
        )}
      </Panel>

      <Panel title="Incidentes detectados" flush actions={
        <Segmented label="Filtro" value={filter} onChange={setFilter} options={[{ value: 'PENDIENTE', label: 'Pendientes' }, { value: 'ALL', label: 'Todos' }]} />
      }>
        {list.error && !d ? <ErrorState message={list.error.message} onRetry={list.reload} /> : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Estación</th><th>Lectura</th><th className="num">PM2.5</th><th className="num">Media móvil</th>
                  <th className="num">Desviación</th><th>Severidad</th><th>Estado</th><th />
                </tr>
              </thead>
              <tbody>
                {!d ? (
                  <tr><td colSpan={8}><Skeleton height={120} /></td></tr>
                ) : rows.length === 0 ? (
                  <tr><td colSpan={8} className="muted" style={{ textAlign: 'center', padding: 28 }}>No hay incidentes pendientes de validación.</td></tr>
                ) : rows.map((a) => (
                  <tr key={a.id}>
                    <td className="strong">{a.estacionNombre}</td>
                    <td>{fmtDateTime(a.fechaMedicion)}</td>
                    <td className="num strong">{num(a.valorObservado)}</td>
                    <td className="num muted">{num(a.mediaEsperada)} ± {num(a.desviacionEstandar)}</td>
                    <td className="num">+{num(a.puntajeZ)} σ</td>
                    <td><Badge tone={severityTone[a.severidad]} dot>{severityLabel[a.severidad]}</Badge></td>
                    <td className="muted">{anomalyStatusLabel[a.estado]}</td>
                    <td>
                      {a.estado === 'PENDIENTE' && (
                        <div className="row" style={{ justifyContent: 'flex-end', flexWrap: 'nowrap' }}>
                          <Button size="sm" variant="danger" icon={<BellRing size={14} />} loading={busy === a.id} onClick={() => act(a, 'notificar')}>Notificar alerta</Button>
                          <Button size="sm" variant="secondary" icon={<X size={14} />} disabled={busy === a.id} onClick={() => act(a, 'descartar')}>Descartar</Button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Panel>

      {d && (
        <ThresholdSheet open={configuring} current={d.estadisticas.umbral} onClose={() => setConfiguring(false)}
          onSaved={(threshold) => { list.setData((p) => p && { ...p, estadisticas: { ...p.estadisticas, umbral: threshold } }); setConfiguring(false) }} />
      )}
    </>
  )
}

function ThresholdSheet({ open, current, onClose, onSaved }: { open: boolean; current: number; onClose: () => void; onSaved: (t: number) => void }) {
  const [value, setValue] = useState(current)
  const [saving, setSaving] = useState(false)

  const save = async () => {
    setSaving(true)
    try {
      await api.put('/anomalias/umbral', { umbral: value })
      toast.success(`Umbral ajustado a ${num(value)} σ`, { description: 'Se aplicará a las siguientes mediciones entrantes.' })
      onSaved(value)
    } catch (e) {
      toast.error((e as ApiError).message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Sheet open={open} onClose={onClose} title="Configurar umbral de detección" width={460} footer={
      <><Button onClick={onClose}>Cancelar</Button><Button variant="primary" loading={saving} onClick={save}>Guardar umbral</Button></>
    }>
      <Field label={`Sensibilidad de desviación: ${num(value)} σ`} htmlFor="z" hint="Valores bajos detectan más picos (y más falsos positivos); valores altos solo eventos extremos.">
        <input id="z" type="range" min={1.5} max={6} step={0.1} value={value} onChange={(e) => setValue(Number(e.target.value))} style={{ accentColor: 'var(--accent)' }} />
      </Field>
      <div className="row between small muted"><span>1,5 σ · sensible</span><span>6,0 σ · estricto</span></div>
    </Sheet>
  )
}
