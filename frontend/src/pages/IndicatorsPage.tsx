import { RefreshCw } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import { Badge, Button, ErrorState, PageHeader, Panel, Skeleton, Switch } from '../components/ui'
import { api, ApiError } from '../lib/api'
import { useAuth } from '../lib/auth'
import { fmtDateTime, indicatorLabel, int, num, signedPercent } from '../lib/format'
import type { IndicadoresRespuesta as IndicatorsResponse, TipoIndicador as IndicatorType } from '../lib/types'
import { useResource } from '../lib/useResource'

const ORDER: IndicatorType[] = ['PROMEDIO_MOVIL_PM25_24H', 'MAXIMO_DIARIO', 'VARIACION_SEMANAL', 'DIAS_EXCEDENCIA_OMS']

export default function IndicatorsPage() {
  const { hasRole } = useAuth()
  const isAdmin = hasRole('ADMINISTRADOR')
  const res = useResource<IndicatorsResponse>('indicators', () => api.get('/indicadores'))
  const [running, setRunning] = useState(false)
  const d = res.data

  const recalculate = async () => {
    setRunning(true)
    try {
      const r = await api.post<{ registrosProcesados: number; indicadores: number }>('/indicadores/recalcular')
      toast.success('Métricas recalculadas', { description: `${int(r.indicadores)} indicadores sobre ${int(r.registrosProcesados)} registros.` })
      await res.reload()
    } catch (e) {
      toast.error((e as ApiError).message)
    } finally {
      setRunning(false)
    }
  }

  const toggle = async (type: IndicatorType, enabled: boolean) => {
    res.setData((prev) => prev && { ...prev, configuraciones: prev.configuraciones.map((c) => (c.tipo === type ? { ...c, habilitado: enabled } : c)) })
    try {
      await api.patch(`/indicadores/${type}`, { habilitado: enabled })
      toast.success(enabled ? 'Cálculo reanudado' : 'Cálculo suspendido', { description: enabled ? indicatorLabel[type].name : 'El histórico previo se conserva.' })
    } catch (e) {
      res.setData((prev) => prev && { ...prev, configuraciones: prev.configuraciones.map((c) => (c.tipo === type ? { ...c, habilitado: !enabled } : c)) })
      toast.error((e as ApiError).message)
    }
  }

  const configs = ORDER.map((t) => d?.configuraciones.find((c) => c.tipo === t)).filter(Boolean) as IndicatorsResponse['configuraciones']

  return (
    <>
      <PageHeader
        title="Métricas agregadas e indicadores"
        subtitle="Indicadores sintéticos calculados a partir de las mediciones horarias depuradas. Alimentan reportes, tableros y modelos."
        actions={<Button variant="primary" icon={<RefreshCw size={16} />} loading={running} onClick={recalculate}>Recalcular métricas ahora</Button>}
      />

      {res.error && !d ? <Panel><ErrorState message={res.error.message} onRetry={res.reload} /></Panel> : (
        <>
          <Panel title="Configuración de indicadores" caption={isAdmin ? 'Un indicador suspendido deja de calcularse sin borrar su histórico.' : 'Solo el administrador puede suspender o reanudar indicadores.'} flush>
            {!d ? <div className="panel-body"><Skeleton height={180} /></div> : configs.map((c) => (
              <div className="list-row" key={c.tipo}>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div className="row" style={{ gap: 8 }}>
                    <span className="strong">{indicatorLabel[c.tipo].name}</span>
                    <Badge tone={c.habilitado ? 'green' : 'neutral'} dot>{c.habilitado ? 'Activo' : 'En reposo'}</Badge>
                  </div>
                  <p className="small muted">{indicatorLabel[c.tipo].description}</p>
                </div>
                <div className="small muted" style={{ textAlign: 'right', minWidth: 170 }}>
                  <div>{c.programacion}</div>
                  <div>{c.ultimaEjecucion ? `Última ejecución ${fmtDateTime(c.ultimaEjecucion)}` : 'Sin ejecuciones'}</div>
                  <div className="tabular">{int(c.registrosProcesados)} registros procesados</div>
                </div>
                <Switch checked={c.habilitado} label={indicatorLabel[c.tipo].name} disabled={!isAdmin} onChange={(v) => toggle(c.tipo, v)} />
              </div>
            ))}
          </Panel>

          <Panel title="Indicadores por estación" caption="Último valor calculado de cada indicador." flush>
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Estación</th>
                    {ORDER.map((t) => <th key={t} className="num">{indicatorLabel[t].name}</th>)}
                    <th>Calculado</th>
                  </tr>
                </thead>
                <tbody>
                  {d?.estaciones.map((s) => (
                    <tr key={s.estacionId}>
                      <td className="strong">{s.estacionNombre}</td>
                      <td className="num">{num(s.valores.PROMEDIO_MOVIL_PM25_24H)} <span className="muted">µg/m³</span></td>
                      <td className="num">{num(s.valores.MAXIMO_DIARIO)} <span className="muted">µg/m³</span></td>
                      <td className={`num ${(s.valores.VARIACION_SEMANAL ?? 0) > 0 ? 'delta-bad' : 'delta-good'}`}>
                        {signedPercent(s.valores.VARIACION_SEMANAL)}
                      </td>
                      <td className="num">
                        {s.valores.DIAS_EXCEDENCIA_OMS === undefined ? '—' : (
                          <Badge tone={s.valores.DIAS_EXCEDENCIA_OMS > 0 ? 'orange' : 'green'}>{int(s.valores.DIAS_EXCEDENCIA_OMS)} días</Badge>
                        )}
                      </td>
                      <td className="muted">{fmtDateTime(s.fechaCalculo)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Panel>
        </>
      )}
    </>
  )
}
