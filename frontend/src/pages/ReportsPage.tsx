import { Download, FileWarning, Leaf } from 'lucide-react'
import { useEffect, useState } from 'react'
import { toast } from 'sonner'
import { useEffectiveStation, StationSelect } from '../components/StationSelect'
import { Button, Callout, EmptyState, Field, PageHeader, Panel, Segmented, Skeleton } from '../components/ui'
import { api, ApiError } from '../lib/api'
import { daysAgo, fmtDate, fmtDateTime, int, isoDate, num } from '../lib/format'
import type { DocumentoReporte as ReportDocument, FormatoReporte as ReportFormat, TipoReporte as ReportType } from '../lib/types'

const TYPES: { value: ReportType; label: string }[] = [
  { value: 'RESUMEN_EJECUTIVO', label: 'Resumen ejecutivo de calidad del aire' },
  { value: 'DETALLE_HISTORICO', label: 'Detalle histórico diario' },
  { value: 'PRONOSTICO', label: 'Perspectiva y pronóstico 48 h' },
]

const FORMAT_LABEL: Record<ReportFormat, string> = { PDF: 'PDF', CSV: 'CSV', EXCEL: 'Excel' }

export default function ReportsPage() {
  const station = useEffectiveStation()
  const [type, setType] = useState<ReportType>('RESUMEN_EJECUTIVO')
  const [from, setFrom] = useState(daysAgo(21))
  const [to, setTo] = useState(isoDate(new Date()))
  const [format, setFormat] = useState<ReportFormat>('PDF')
  const [preview, setPreview] = useState<ReportDocument | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [downloading, setDownloading] = useState(false)

  const request = { tipo: type, estacionId: station?.id, desde: from, hasta: to }

  useEffect(() => {
    if (!station) return
    let cancelled = false
    setLoading(true)
    const t = setTimeout(() => {
      api.post<ReportDocument>('/reportes/vista-previa', request)
        .then((r) => { if (!cancelled) { setPreview(r); setError(null) } })
        .catch((e: ApiError) => { if (!cancelled) { setPreview(null); setError(e.message) } })
        .finally(() => { if (!cancelled) setLoading(false) })
    }, 250)
    return () => { cancelled = true; clearTimeout(t) }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [type, station?.id, from, to])

  const download = async () => {
    setDownloading(true)
    try {
      const name = await api.download(`/reportes/exportar?formato=${format}`, request)
      toast.success('Reporte generado', { description: name })
    } catch (e) {
      toast.error((e as ApiError).message)
    } finally {
      setDownloading(false)
    }
  }

  return (
    <>
      <PageHeader
        title="Generación de reportes"
        subtitle="Configure el informe, revise la vista previa y descárguelo en el formato que necesite para auditorías o entes reguladores."
      />

      <div className="grid grid-side-main">
        <Panel title="Configuración">
          <div className="stack" style={{ gap: 16 }}>
            <Field label="Tipo de reporte" htmlFor="r-type">
              <select id="r-type" className="select" value={type} onChange={(e) => setType(e.target.value as ReportType)}>
                {TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
              </select>
            </Field>
            <Field label="Estación"><StationSelect /></Field>
            <div className="grid grid-2" style={{ gap: 10 }}>
              <Field label="Desde" htmlFor="r-from"><input id="r-from" type="date" className="input" value={from} max={to} onChange={(e) => setFrom(e.target.value)} /></Field>
              <Field label="Hasta" htmlFor="r-to"><input id="r-to" type="date" className="input" value={to} min={from} onChange={(e) => setTo(e.target.value)} /></Field>
            </div>
            <div className="field">
              <span className="field-label">Formato</span>
              <Segmented<ReportFormat> label="Formato de exportación" value={format} onChange={setFormat}
                options={(['PDF', 'CSV', 'EXCEL'] as ReportFormat[]).map((f) => ({ value: f, label: FORMAT_LABEL[f] }))} />
            </div>
            <Button variant="primary" size="lg" icon={<Download size={16} />} loading={downloading} disabled={!preview || loading} onClick={download}>
              Descargar reporte ({FORMAT_LABEL[format]})
            </Button>
            {error && <Callout tone="warning">{error}</Callout>}
          </div>
        </Panel>

        <Panel title="Vista previa" caption="Así se verá el documento exportado.">
          {!preview && loading ? <Skeleton height={520} /> : !preview ? (
            <EmptyState icon={<FileWarning size={28} />} title="Sin vista previa">{error ?? 'Seleccione una estación y un periodo.'}</EmptyState>
          ) : (
            <article className="report-sheet" style={{ opacity: loading ? 0.6 : 1, transition: 'opacity 200ms ease' }}>
              <header className="row between" style={{ alignItems: 'flex-start', borderBottom: '1px solid #e5e5ea', paddingBottom: 16 }}>
                <div>
                  <p className="row strong" style={{ gap: 6, color: '#1f7a36' }}><Leaf size={15} /> EcoAnalytics</p>
                  <h3 style={{ fontSize: 20, letterSpacing: '-0.02em', marginTop: 6 }}>{TYPES.find((t) => t.value === preview.tipo)?.label}</h3>
                  <p style={{ color: '#6e6e73', marginTop: 4 }}>{preview.estacionNombre} · {preview.zona}</p>
                </div>
                <div style={{ textAlign: 'right', color: '#6e6e73' }}>
                  <p className="strong" style={{ color: '#1d1d1f' }}>{preview.reporteId}</p>
                  <p>Periodo {fmtDate(preview.desde)} – {fmtDate(preview.hasta)}</p>
                  <p>Emitido {fmtDateTime(preview.fechaEmision)}</p>
                </div>
              </header>

              <div className="grid grid-3" style={{ margin: '18px 0' }}>
                <ReportKpi label="Media PM2.5" value={`${num(preview.mediaPm25)} µg/m³`} />
                <ReportKpi label="Cumplimiento guía OMS" value={`${num(preview.porcentajeCumplimiento)} %`} />
                <ReportKpi label="Mediciones analizadas" value={int(preview.totalMediciones)} />
              </div>

              <p className="strong" style={{ marginBottom: 6 }}>Resumen por variable</p>
              <table className="table">
                <thead><tr><th>Variable</th><th className="num">Media</th><th className="num">Mín.</th><th className="num">Máx.</th><th>Evaluación</th></tr></thead>
                <tbody>
                  {preview.variables.map((v) => (
                    <tr key={v.variable}>
                      <td>{v.variable} <span style={{ color: '#8e8e93' }}>{v.unidad}</span></td>
                      <td className="num">{num(v.media)}</td><td className="num">{num(v.minimo)}</td><td className="num">{num(v.maximo)}</td>
                      <td>{v.evaluacion}</td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {preview.diario.length > 0 && (
                <>
                  <p className="strong" style={{ margin: '18px 0 6px' }}>Comportamiento diario</p>
                  <table className="table">
                    <thead><tr><th>Fecha</th><th className="num">Media PM2.5</th><th className="num">Máx. PM2.5</th><th className="num">ICA</th><th>Categoría</th></tr></thead>
                    <tbody>
                      {preview.diario.slice(0, 10).map((r) => (
                        <tr key={r.fecha}><td>{fmtDate(r.fecha)}</td><td className="num">{num(r.mediaPm25)}</td><td className="num">{num(r.maxPm25)}</td><td className="num">{r.ica}</td><td>{r.categoria}</td></tr>
                      ))}
                    </tbody>
                  </table>
                  {preview.diario.length > 10 && <p style={{ color: '#8e8e93', marginTop: 6 }}>… y {preview.diario.length - 10} días más en el documento exportado.</p>}
                </>
              )}

              {preview.pronostico.length > 0 && (
                <p style={{ marginTop: 18, color: '#6e6e73' }}>
                  Incluye pronóstico de {preview.pronostico.length} horas: pico de {num(Math.max(...preview.pronostico.map((f) => f.pm25)))} µg/m³.
                </p>
              )}
              <p style={{ marginTop: 22, fontSize: 11, color: '#8e8e93' }}>
                Información de apoyo al análisis. EcoAnalytics no reemplaza a la autoridad ambiental.
              </p>
            </article>
          )}
        </Panel>
      </div>
    </>
  )
}

function ReportKpi({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ border: '1px solid #e5e5ea', borderRadius: 10, padding: '10px 12px' }}>
      <p style={{ color: '#6e6e73', fontSize: 11.5, fontWeight: 600 }}>{label}</p>
      <p className="tabular" style={{ fontSize: 18, fontWeight: 700, marginTop: 2 }}>{value}</p>
    </div>
  )
}
