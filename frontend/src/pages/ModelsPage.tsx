import { BrainCircuit, Play, Rocket } from 'lucide-react'
import { useState } from 'react'
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { toast } from 'sonner'
import { axisProps, ChartTooltip } from '../components/ChartTooltip'
import { Badge, Button, EmptyState, ErrorState, PageHeader, Panel, Skeleton, Stat } from '../components/ui'
import { api, ApiError } from '../lib/api'
import { fmtDateTime, fmtWeekdayHour, int, num } from '../lib/format'
import type { AlgoritmoModelo as ModelAlgorithm, EntrenamientoRespuesta as TrainingResponse, VersionModelo as ModelVersion } from '../lib/types'
import { useResource } from '../lib/useResource'

const ALGORITHMS: { value: ModelAlgorithm; name: string; description: string }[] = [
  { value: 'RANDOM_FOREST', name: 'Random Forest Regressor', description: 'Ensamble de 40 árboles de regresión con muestreo bootstrap.' },
  { value: 'REGRESION_LINEAL', name: 'Regresión lineal múltiple', description: 'Mínimos cuadrados con regularización ridge.' },
  { value: 'K_VECINOS', name: 'K vecinos más cercanos', description: 'Promedio ponderado de las 12 situaciones históricas más similares.' },
  { value: 'MEDIA_MOVIL', name: 'Media móvil estacional', description: 'Línea base: mezcla de la última hora, la media de 6 h y el mismo horario del día anterior.' },
]

const FEATURES = ['PM2.5 histórico (t-1, t-2, t-3, t-24)', 'Media móvil de 6 horas', 'Temperatura', 'Humedad', 'Hora del día', 'Fin de semana']

export default function ModelsPage() {
  const models = useResource<ModelVersion[]>('models', () => api.get('/modelos'))
  const [algorithm, setAlgorithm] = useState<ModelAlgorithm>('RANDOM_FOREST')
  const [training, setTraining] = useState(false)
  const [last, setLast] = useState<TrainingResponse | null>(null)
  const [deploying, setDeploying] = useState<number | null>(null)

  const train = async () => {
    setTraining(true)
    try {
      const r = await api.post<TrainingResponse>('/modelos/entrenar', { algoritmo: algorithm })
      setLast(r)
      toast.success(`Modelo ${r.modelo.version} entrenado`, { description: `R² ${num(r.modelo.r2, 3)} · RMSE ${num(r.modelo.rmse, 2)} µg/m³` })
      void models.reload()
    } catch (e) {
      toast.error((e as ApiError).message)
    } finally {
      setTraining(false)
    }
  }

  const deploy = async (m: ModelVersion) => {
    setDeploying(m.id)
    try {
      await api.post(`/modelos/${m.id}/desplegar`)
      toast.success(`${m.version} en producción`, { description: `${m.nombreAlgoritmo} genera ahora los pronósticos.` })
      await models.reload()
    } catch (e) {
      toast.error((e as ApiError).message)
    } finally {
      setDeploying(null)
    }
  }

  const production = models.data?.find((m) => m.estado === 'PRODUCCION')
  const shown = last?.modelo ?? production

  return (
    <>
      <PageHeader
        title="Laboratorio de modelos predictivos"
        subtitle="Entrene, evalúe y despliegue modelos de Machine Learning para pronosticar PM2.5. Solo se usan datos validados."
      />

      <div className="grid grid-side-main">
        <Panel title="Configuración del entrenamiento">
          <div className="stack" style={{ gap: 16 }}>
            <fieldset className="choice-list">
              <legend className="field-label">Algoritmo</legend>
              {ALGORITHMS.map((a) => (
                <label key={a.value} className="choice" data-selected={algorithm === a.value}>
                  <input type="radio" name="algorithm" value={a.value} checked={algorithm === a.value} onChange={() => setAlgorithm(a.value)} />
                  <span>
                    <span className="strong small" style={{ display: 'block' }}>{a.name}</span>
                    <span className="small muted">{a.description}</span>
                  </span>
                </label>
              ))}
            </fieldset>
            <div className="field">
              <span className="field-label">Variable objetivo</span>
              <p className="small">PM2.5 a 24 horas (pronóstico recursivo hora a hora)</p>
            </div>
            <div className="field">
              <span className="field-label">Variables predictoras</span>
              <ul className="feature-list">
                {FEATURES.map((f) => <li key={f}>{f}</li>)}
              </ul>
            </div>
            <p className="small muted">Partición cronológica 80 % entrenamiento / 20 % prueba, últimos 120 días de cada estación.</p>
            <Button variant="primary" size="lg" icon={<Play size={16} />} loading={training} onClick={train}>
              {training ? 'Entrenando…' : 'Entrenar y evaluar modelo'}
            </Button>
          </div>
        </Panel>

        <div className="stack" style={{ gap: 16 }}>
          <Panel
            title={shown ? `Evaluación · ${shown.version} ${shown.nombreAlgoritmo}` : 'Evaluación'}
            caption={last ? `Conjunto de prueba de ${last.estacionNombre}` : production ? 'Modelo actualmente en producción' : undefined}
            actions={shown && <Badge tone={shown.estado === 'PRODUCCION' ? 'green' : 'blue'} dot>{shown.estado === 'PRODUCCION' ? 'En producción' : 'Candidato'}</Badge>}
          >
            {!shown ? <Skeleton height={90} /> : (
              <div className="kpi-row">
                <Stat label="R² (ajuste)" value={num(shown.r2, 3)} foot="1,0 = ajuste perfecto" />
                <Stat label="RMSE" value={num(shown.rmse, 2)} unit="µg/m³" foot="Error cuadrático medio" />
                <Stat label="MAE" value={num(shown.mae, 2)} unit="µg/m³" foot="Error absoluto medio" />
                <Stat label="Muestras" value={int(shown.muestrasEntrenamiento)} foot={`${int(shown.muestrasPrueba)} de prueba · ${int(shown.duracionMs)} ms`} />
              </div>
            )}
            {last ? (
              <div style={{ height: 260, marginTop: 18 }}>
                <ResponsiveContainer>
                  <LineChart data={last.comparacion} margin={{ top: 8, right: 8, bottom: 0, left: -14 }}>
                    <CartesianGrid vertical={false} stroke="var(--grid)" />
                    <XAxis dataKey="fecha" tickFormatter={fmtWeekdayHour} minTickGap={60} {...axisProps} />
                    <YAxis width={48} {...axisProps} />
                    <Tooltip content={<ChartTooltip labelFormatter={(l) => fmtWeekdayHour(String(l))} units={{ real: 'µg/m³', predicho: 'µg/m³' }} />} />
                    <Line dataKey="real" name="Real" stroke="var(--series-1)" strokeWidth={2.2} dot={false} animationDuration={700} />
                    <Line dataKey="predicho" name="Predicho" stroke="var(--series-4)" strokeWidth={2.2} dot={false} strokeDasharray="5 3" animationDuration={700} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            ) : (
              <div style={{ marginTop: 12 }}>
                <EmptyState icon={<BrainCircuit size={26} />} title="Curva real vs. predicha">
                  Entrene un modelo para comparar sus predicciones con los valores reales del conjunto de prueba.
                </EmptyState>
              </div>
            )}
          </Panel>

          <Panel title="Registro de modelos" caption="Solo una versión genera pronósticos a la vez; desplegar otra archiva la actual." flush>
            {models.error && !models.data ? <ErrorState message={models.error.message} onRetry={models.reload} /> : (
              <div className="table-wrap">
                <table className="table">
                  <thead><tr><th>Versión</th><th>Algoritmo</th><th className="num">R²</th><th className="num">RMSE</th><th className="num">MAE</th><th>Estado</th><th>Entrenado</th><th /></tr></thead>
                  <tbody>
                    {models.data?.map((m) => (
                      <tr key={m.id} className={m.estado === 'PRODUCCION' ? 'selected' : ''}>
                        <td className="strong">{m.version}</td>
                        <td>{m.nombreAlgoritmo}</td>
                        <td className="num">{num(m.r2, 3)}</td>
                        <td className="num">{num(m.rmse, 2)}</td>
                        <td className="num">{num(m.mae, 2)}</td>
                        <td>
                          <Badge tone={m.estado === 'PRODUCCION' ? 'green' : m.estado === 'CANDIDATO' ? 'blue' : 'neutral'} dot>
                            {m.estado === 'PRODUCCION' ? 'En producción' : m.estado === 'CANDIDATO' ? 'Candidato' : 'Archivado'}
                          </Badge>
                        </td>
                        <td className="muted">{fmtDateTime(m.fechaEntrenamiento)}</td>
                        <td>{m.estado !== 'PRODUCCION' && (
                          <Button size="sm" variant="secondary" icon={<Rocket size={14} />} loading={deploying === m.id} onClick={() => deploy(m)}>Desplegar</Button>
                        )}</td>
                      </tr>
                    )) ?? <tr><td colSpan={8}><Skeleton height={120} /></td></tr>}
                  </tbody>
                </table>
              </div>
            )}
          </Panel>
        </div>
      </div>
    </>
  )
}
