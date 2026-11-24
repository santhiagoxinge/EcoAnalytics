import { BellRing, CheckCheck, Mail, Megaphone, MonitorSmartphone, Plus } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import { Sheet } from '../components/Sheet'
import { Badge, Button, EmptyState, ErrorState, Field, PageHeader, Panel, Segmented, Skeleton, Switch } from '../components/ui'
import { api, ApiError } from '../lib/api'
import { useAuth } from '../lib/auth'
import { alertLevelColor, alertLevelLabel, alertTypeLabel, channelLabel, fmtDateTime, int } from '../lib/format'
import { useStations } from '../lib/stations'
import type { Alerta as Alert, CentroAlertas as AlertCenter, CodigoCanal as ChannelCode, NivelAlerta as AlertLevel } from '../lib/types'
import { useResource } from '../lib/useResource'

const channelIcons: Record<ChannelCode, typeof Mail> = { PUSH: MonitorSmartphone, CORREO: Mail, BANNER_WEB: Megaphone }

export default function AlertsPage() {
  const { hasRole } = useAuth()
  const isAdmin = hasRole('ADMINISTRADOR')
  const isStaff = hasRole('ADMINISTRADOR', 'ANALISTA')
  const res = useResource<AlertCenter>('alerts', () => api.get('/alertas'))
  const [composing, setComposing] = useState(false)
  const [filter, setFilter] = useState<'ALL' | 'ACTIVA'>('ACTIVA')
  const data = res.data

  const toggleChannel = async (code: ChannelCode, enabled: boolean) => {
    res.setData((d) => d && { ...d, canales: d.canales.map((c) => (c.codigo === code ? { ...c, habilitado: enabled } : c)) })
    try {
      await api.patch(`/alertas/canales/${code}`, { habilitado: enabled })
      toast.success(enabled ? `${channelLabel[code].name} habilitado` : `${channelLabel[code].name} suspendido`,
        { description: enabled ? 'Recibirá los próximos eventos.' : 'Los demás canales siguen activos.' })
    } catch (e) {
      res.setData((d) => d && { ...d, canales: d.canales.map((c) => (c.codigo === code ? { ...c, habilitado: !enabled } : c)) })
      toast.error((e as ApiError).message)
    }
  }

  const resolve = async (alert: Alert) => {
    try {
      const updated = await api.post<Alert>(`/alertas/${alert.id}/resolver`)
      res.setData((d) => d && { ...d, alertas: d.alertas.map((a) => (a.id === updated.id ? updated : a)) })
      toast.success('Alerta marcada como resuelta')
    } catch (e) {
      toast.error((e as ApiError).message)
    }
  }

  const alerts = (data?.alertas ?? []).filter((a) => filter === 'ALL' || a.estado === 'ACTIVA')

  return (
    <>
      <PageHeader
        title="Centro de alertas"
        subtitle="Alertas reactivas (umbral superado), predictivas (el modelo anticipa deterioro) y extraordinarias emitidas por la autoridad."
        actions={isAdmin && <Button variant="primary" icon={<Plus size={16} />} onClick={() => setComposing(true)}>Emitir alerta extraordinaria</Button>}
      />

      {res.error && !data ? <Panel><ErrorState message={res.error.message} onRetry={res.reload} /></Panel> : (
        <>
          <div className="kpi-row">
            {[
              ['Activas', data?.estadisticas.activas],
              ['Últimas 24 h', data?.estadisticas.ultimas24h],
              ['Reactivas', data?.estadisticas.reactivas],
              ['Predictivas', data?.estadisticas.predictivas],
              ['Extraordinarias', data?.estadisticas.extraordinarias],
            ].map(([label, value]) => (
              <div className="panel kpi" key={label as string}>
                <p className="stat-label">{label}</p>
                <p className="stat-value">{data ? int(value as number) : <Skeleton height={28} width={50} />}</p>
              </div>
            ))}
          </div>

          <div className="grid grid-main-side">
            <Panel
              title="Historial de alertas difundidas"
              actions={<Segmented label="Filtro" value={filter} onChange={setFilter}
                options={[{ value: 'ACTIVA', label: 'Activas' }, { value: 'ALL', label: 'Todas' }]} />}
              flush
            >
              {!data ? <div className="panel-body"><Skeleton height={200} /></div> : alerts.length === 0 ? (
                <EmptyState icon={<CheckCheck size={28} />} title="Sin alertas activas">
                  Ninguna estación ni predicción supera actualmente los umbrales configurados.
                </EmptyState>
              ) : alerts.map((a) => {
                const color = alertLevelColor[a.nivel]
                return (
                  <article key={a.id} className="list-row" style={{ alignItems: 'flex-start' }}>
                    <div className="alert-level-mark" style={{ background: color.fill }} aria-hidden><BellRing size={15} /></div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div className="row" style={{ gap: 6 }}>
                        <span className="strong" style={{ color: color.ink }}>{a.titulo}</span>
                        <Badge tone={a.tipo === 'PREDICTIVA' ? 'purple' : a.tipo === 'EXTRAORDINARIA' ? 'red' : 'blue'}>
                          {alertTypeLabel[a.tipo]}
                        </Badge>
                        {a.estado === 'RESUELTA' && <Badge>Resuelta</Badge>}
                      </div>
                      <p className="small" style={{ marginTop: 4 }}>{a.mensaje}</p>
                      {a.recomendacion && <p className="small muted" style={{ marginTop: 4 }}>{a.recomendacion}</p>}
                      <p className="small muted" style={{ marginTop: 6 }}>
                        {fmtDateTime(a.fechaCreacion)} · {a.origen} · {a.canales.length
                          ? `Difundida por ${a.canales.map((c) => channelLabel[c].name.toLowerCase()).join(', ')}`
                          : 'Sin canales habilitados'}
                      </p>
                    </div>
                    {isStaff && a.estado === 'ACTIVA' && (
                      <Button size="sm" variant="secondary" onClick={() => resolve(a)}>Resolver</Button>
                    )}
                  </article>
                )
              })}
            </Panel>

            <Panel title="Canales de difusión" caption={isAdmin ? 'Suspender un canal no afecta a los demás.' : 'Configurados por el administrador.'} flush>
              {data?.canales.map((c) => {
                const Icon = channelIcons[c.codigo]
                return (
                  <div className="list-row" key={c.codigo}>
                    <Icon size={18} style={{ color: 'var(--accent)', flex: 'none' }} />
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <p className="strong small">{channelLabel[c.codigo].name}</p>
                      <p className="small muted">{channelLabel[c.codigo].description}</p>
                      <p className="small muted">{int(c.entregas)} envíos{c.ultimaEntrega ? ` · último ${fmtDateTime(c.ultimaEntrega)}` : ''}</p>
                    </div>
                    <Switch checked={c.habilitado} label={channelLabel[c.codigo].name} disabled={!isAdmin}
                      onChange={(v) => toggleChannel(c.codigo, v)} />
                  </div>
                )
              }) ?? <div className="panel-body"><Skeleton height={160} /></div>}
            </Panel>
          </div>
        </>
      )}

      <ExtraordinaryAlertSheet open={composing} onClose={() => setComposing(false)} onIssued={() => { setComposing(false); void res.reload() }} />
    </>
  )
}

function ExtraordinaryAlertSheet({ open, onClose, onIssued }: { open: boolean; onClose: () => void; onIssued: () => void }) {
  const { stations } = useStations()
  const [title, setTitle] = useState('')
  const [message, setMessage] = useState('')
  const [level, setLevel] = useState<AlertLevel>('NARANJA')
  const [stationId, setStationId] = useState<string>('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [sending, setSending] = useState(false)

  const submit = async () => {
    const e: Record<string, string> = {}
    if (!title.trim()) e.title = 'El título es obligatorio.'
    if (!message.trim()) e.message = 'Redacte el comunicado.'
    setErrors(e)
    if (Object.keys(e).length) return
    setSending(true)
    try {
      const alert = await api.post<Alert>('/alertas/extraordinarias', { titulo: title, mensaje: message, nivel: level, estacionId: stationId ? Number(stationId) : null })
      toast.success('Alerta extraordinaria difundida', { description: `Enviada por ${alert.canales.length} canal(es) habilitado(s).` })
      setTitle('')
      setMessage('')
      onIssued()
    } catch (err) {
      toast.error((err as ApiError).message)
    } finally {
      setSending(false)
    }
  }

  return (
    <Sheet open={open} onClose={onClose} title="Emitir alerta extraordinaria" footer={
      <>
        <Button onClick={onClose}>Cancelar</Button>
        <Button variant="primary" loading={sending} onClick={submit}>Difundir a todos los canales</Button>
      </>
    }>
      <p className="small muted">Use esta opción ante contingencias declaradas por la autoridad (incendios forestales, emergencias). Se distribuye de inmediato.</p>
      <Field label="Título" htmlFor="al-title" error={errors.title}>
        <input id="al-title" className="input" value={title} onChange={(e) => setTitle(e.target.value)}
          placeholder="Contingencia por incendio forestal" aria-invalid={!!errors.title} />
      </Field>
      <Field label="Comunicado" htmlFor="al-msg" error={errors.message}>
        <textarea id="al-msg" className="textarea" value={message} onChange={(e) => setMessage(e.target.value)}
          placeholder="Describa la situación, las zonas afectadas y las medidas recomendadas." aria-invalid={!!errors.message} />
      </Field>
      <div className="field">
        <span className="field-label">Nivel</span>
        <Segmented<AlertLevel> label="Nivel de alerta" value={level} onChange={setLevel}
          options={(['AMARILLA', 'NARANJA', 'ROJA', 'PURPURA'] as AlertLevel[]).map((l) => ({ value: l, label: alertLevelLabel[l] }))} />
      </div>
      <Field label="Estación o zona (opcional)" htmlFor="al-station">
        <select id="al-station" className="select" value={stationId} onChange={(e) => setStationId(e.target.value)}>
          <option value="">Toda la red de monitoreo</option>
          {stations.map((s) => <option key={s.id} value={s.id}>{s.nombre}</option>)}
        </select>
      </Field>
    </Sheet>
  )
}
