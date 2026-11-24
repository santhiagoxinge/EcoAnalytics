import { Cpu, Database, HardDrive, RefreshCw, Timer } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Badge, Button, ErrorState, PageHeader, Panel, Segmented, Skeleton, Stat, type BadgeTone } from '../components/ui'
import { api } from '../lib/api'
import { fmtDateTime, int, num, relativeMinutes, sourceLabel } from '../lib/format'
import type { EventoAuditoria as AuditEvent, PanoramaMonitoreo as MonitoringOverview } from '../lib/types'
import { useResource } from '../lib/useResource'

const syncLabel: Record<MonitoringOverview['fuentes'][number]['sincronizacion'], { text: string; tone: BadgeTone }> = {
  SINCRONIZADA: { text: 'Sincronizada', tone: 'green' },
  RETRASADA: { text: 'Retrasada', tone: 'orange' },
  CAIDA: { text: 'Sin señal', tone: 'red' },
  MANTENIMIENTO: { text: 'Mantenimiento', tone: 'orange' },
}

const levelTone: Record<AuditEvent['nivel'], BadgeTone> = { INFO: 'blue', ADVERTENCIA: 'orange', ERROR: 'red' }

function uptime(seconds: number) {
  const d = Math.floor(seconds / 86400)
  const h = Math.floor((seconds % 86400) / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  return d ? `${d} d ${h} h` : h ? `${h} h ${m} min` : `${m} min`
}

export default function MonitoringPage() {
  const [level, setLevel] = useState<'ALL' | AuditEvent['nivel']>('ALL')
  const overview = useResource<MonitoringOverview>('monitoring', () => api.get('/monitoreo'))
  const audit = useResource<AuditEvent[]>(`audit-${level}`, () => api.get(`/monitoreo/auditoria${level === 'ALL' ? '' : `?nivel=${level}`}`))

  useEffect(() => {
    const t = setInterval(() => void overview.reload(), 30_000)
    return () => clearInterval(t)
  }, [overview.reload])

  const o = overview.data

  return (
    <>
      <PageHeader
        title="Monitoreo y administración del sistema"
        subtitle="Salud de la infraestructura, estado de las fuentes de datos, pipelines en segundo plano y bitácora de auditoría."
        actions={<Button variant="secondary" icon={<RefreshCw size={15} />} loading={overview.loading} onClick={() => { void overview.reload(); void audit.reload() }}>Actualizar</Button>}
      />

      {overview.error && !o ? <Panel><ErrorState message={overview.error.message} onRetry={overview.reload} /></Panel> : (
        <>
          <div className="kpi-row">
            <div className="panel kpi"><Stat label="Disponibilidad" value={o ? uptime(o.infraestructura.segundosActivo) : '—'} foot={<span className="row" style={{ gap: 4 }}><Timer size={13} /> Tiempo en servicio</span>} /></div>
            <div className="panel kpi"><Stat label="Memoria JVM" value={o ? int(o.infraestructura.memoriaUsadaMb) : '—'} unit="MB" foot={<span className="row" style={{ gap: 4 }}><HardDrive size={13} /> de {o ? int(o.infraestructura.memoriaMaximaMb) : '—'} MB</span>} /></div>
            <div className="panel kpi"><Stat label="CPU del proceso" value={o ? num(o.infraestructura.porcentajeCpu) : '—'} unit="%" foot={<span className="row" style={{ gap: 4 }}><Cpu size={13} /> {o?.infraestructura.procesadores} núcleos</span>} /></div>
            <div className="panel kpi"><Stat label="Base de datos" value={o ? int(o.totales.mediciones) : '—'} unit="mediciones" foot={<span className="row" style={{ gap: 4 }}><Database size={13} /> {o?.infraestructura.baseDatos}</span>} /></div>
          </div>

          <div className="kpi-row">
            <div className="panel kpi"><Stat label="Ingestadas en 24 h" value={o ? int(o.totales.ingestadas24h) : '—'} /></div>
            <div className="panel kpi"><Stat label="Estaciones activas" value={o ? `${o.totales.estacionesActivas} de ${o.totales.totalEstaciones}` : '—'} /></div>
            <div className="panel kpi"><Stat label="Anomalías pendientes" value={o ? int(o.totales.anomaliasPendientes) : '—'} /></div>
            <div className="panel kpi"><Stat label="Alertas en 24 h" value={o ? int(o.totales.alertas24h) : '—'} /></div>
            <div className="panel kpi"><Stat label="Usuarios" value={o ? int(o.totales.usuarios) : '—'} /></div>
          </div>

          <div className="grid grid-2">
            <Panel title="Fuentes de datos y conectores" flush>
              {!o ? <div className="panel-body"><Skeleton height={200} /></div> : o.fuentes.map((s) => (
                <div className="list-row" key={s.estacionId}>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <p className="strong small">{s.nombre}</p>
                    <p className="small muted">{sourceLabel[s.tipo]} · última lectura {s.ultimaMedicion ? relativeMinutes(s.minutosDesdeUltima) : 'nunca'}</p>
                  </div>
                  <Badge tone={syncLabel[s.sincronizacion].tone} dot>{syncLabel[s.sincronizacion].text}</Badge>
                </div>
              ))}
            </Panel>

            <Panel title="Pipelines y servicios en segundo plano" flush>
              {!o ? <div className="panel-body"><Skeleton height={200} /></div> : o.servicios.map((j) => (
                <div className="list-row" key={j.nombre}>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <p className="strong small">{j.nombre}</p>
                    <p className="small muted">{j.descripcion} · {j.programacion}</p>
                    <p className="small muted">{j.ultimaEjecucion ? `${fmtDateTime(j.ultimaEjecucion)} · ${j.detalle} · ${int(j.duracionMs)} ms` : j.detalle}</p>
                  </div>
                  <Badge tone={!j.ultimaEjecucion ? 'neutral' : j.saludable ? 'green' : 'red'} dot>{!j.ultimaEjecucion ? 'En espera' : j.saludable ? 'Activo' : 'Con fallos'}</Badge>
                </div>
              ))}
            </Panel>
          </div>
        </>
      )}

      <Panel title="Bitácora de auditoría" caption="Últimos 200 eventos" flush actions={
        <Segmented label="Nivel" value={level} onChange={setLevel}
          options={[{ value: 'ALL', label: 'Todos' }, { value: 'INFO', label: 'INFO' }, { value: 'ADVERTENCIA', label: 'ADVERTENCIA' }, { value: 'ERROR', label: 'ERROR' }]} />
      }>
        <div className="table-wrap" style={{ maxHeight: 460 }}>
          <table className="table">
            <thead><tr><th>Marca de tiempo</th><th>Nivel</th><th>Módulo</th><th>Descripción</th><th>Usuario / proceso</th></tr></thead>
            <tbody>
              {!audit.data ? <tr><td colSpan={5}><Skeleton height={160} /></td></tr> : audit.data.length === 0 ? (
                <tr><td colSpan={5} className="muted" style={{ textAlign: 'center', padding: 28 }}>Sin eventos de este nivel.</td></tr>
              ) : audit.data.map((e) => (
                <tr key={e.id}>
                  <td className="muted" style={{ whiteSpace: 'nowrap' }}>{fmtDateTime(e.fecha)}</td>
                  <td><Badge tone={levelTone[e.nivel]}>{e.nivel}</Badge></td>
                  <td>{e.modulo}</td>
                  <td>{e.descripcion}</td>
                  <td className="muted">{e.actor}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Panel>
    </>
  )
}
