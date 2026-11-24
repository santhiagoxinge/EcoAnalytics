import { CheckCircle2, Download, FileSpreadsheet, FileUp, Trash2, Wand2 } from 'lucide-react'
import { useRef, useState, type DragEvent } from 'react'
import { toast } from 'sonner'
import { Badge, Button, Callout, PageHeader, Panel, Skeleton, Stat } from '../components/ui'
import { api, ApiError } from '../lib/api'
import { fmtDateTime, int, issueTypeLabel, num, sourceLabel } from '../lib/format'
import { useStations } from '../lib/stations'
import type { CargaRespuesta as UploadResponse, ImportacionRespuesta as ImportResponse, Lote as Batch } from '../lib/types'
import { useResource } from '../lib/useResource'

const ACCEPTED = ['.csv', '.json']

export default function IngestionPage() {
  const { stations } = useStations()
  const [dragging, setDragging] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [upload, setUpload] = useState<UploadResponse | null>(null)
  const [result, setResult] = useState<ImportResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [confirming, setConfirming] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)
  const batches = useResource<Batch[]>('batches', () => api.get('/ingesta/lotes'))

  const handleFile = async (file: File) => {
    setError(null)
    setResult(null)
    setUpload(null)
    if (!ACCEPTED.some((ext) => file.name.toLowerCase().endsWith(ext))) {
      setError('Formato no compatible. Únicamente se admiten archivos .csv o .json')
      return
    }
    setUploading(true)
    try {
      setUpload(await api.upload<UploadResponse>('/ingesta/cargar', file))
      void batches.reload()
    } catch (e) {
      setError((e as ApiError).message)
    } finally {
      setUploading(false)
    }
  }

  const onDrop = (e: DragEvent) => {
    e.preventDefault()
    setDragging(false)
    const file = e.dataTransfer.files[0]
    if (file) void handleFile(file)
  }

  const confirm = async () => {
    if (!upload) return
    setConfirming(true)
    try {
      const r = await api.post<ImportResponse>(`/ingesta/${upload.lote.id}/confirmar`)
      setResult(r)
      setUpload(null)
      toast.success('¡Ingesta exitosa!', { description: `${int(r.importados)} registros de calidad del aire fueron incorporados. Datos limpios listos para análisis y predicciones.` })
      void batches.reload()
    } catch (e) {
      toast.error((e as ApiError).message)
    } finally {
      setConfirming(false)
    }
  }

  const cancel = async () => {
    if (!upload) return
    try {
      await api.post(`/ingesta/${upload.lote.id}/cancelar`)
      toast('Lote descartado', { description: 'No se almacenó ningún registro.' })
      setUpload(null)
      void batches.reload()
    } catch (e) {
      toast.error((e as ApiError).message)
    }
  }

  const exportQuality = async (batchId: number) => {
    try {
      toast.success('Reporte de calidad exportado', { description: await api.download(`/ingesta/${batchId}/incidencias/exportar`) })
    } catch (e) {
      toast.error((e as ApiError).message)
    }
  }

  const downloadTemplate = () => {
    const name = stations[0]?.nombre ?? 'Estación Centro'
    const csv = `Estación,Fecha,Hora,PM2.5,PM10,Temperatura,Humedad\n${name},2026-10-25,08:00,14.2,27.1,19.5,72\n`
    const url = URL.createObjectURL(new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' }))
    const a = Object.assign(document.createElement('a'), { href: url, download: 'plantilla_mediciones.csv' })
    a.click()
    URL.revokeObjectURL(url)
  }

  const b = upload?.lote
  const usable = b ? b.registrosValidos + b.registrosAdvertencia : 0

  return (
    <>
      <PageHeader
        title="Ingreso y validación de datos"
        subtitle="Cargue lotes de mediciones en CSV o JSON. Cada registro se valida (rangos físicos, duplicados, campos vacíos) antes de almacenarse."
        actions={<Button variant="secondary" icon={<FileSpreadsheet size={16} />} onClick={downloadTemplate}>Descargar plantilla</Button>}
      />

      <Panel>
        <div
          className="dropzone"
          data-active={dragging}
          role="button"
          tabIndex={0}
          aria-label="Cargar archivo de mediciones"
          onClick={() => inputRef.current?.click()}
          onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && inputRef.current?.click()}
          onDragOver={(e) => { e.preventDefault(); setDragging(true) }}
          onDragLeave={() => setDragging(false)}
          onDrop={onDrop}
        >
          <FileUp size={30} />
          <p className="strong">Arrastre el archivo aquí o haga clic para seleccionarlo</p>
          <p className="small muted">Columnas obligatorias: Estación, Fecha, Hora, PM2.5, PM10, Temperatura y Humedad · Máximo 20 MB</p>
          <input ref={inputRef} type="file" accept=".csv,.json" hidden onChange={(e) => {
            const f = e.target.files?.[0]
            if (f) void handleFile(f)
            e.target.value = ''
          }} />
        </div>
        {uploading && (
          <div style={{ marginTop: 14 }}>
            <p className="small muted" style={{ marginBottom: 6 }}>Analizando estructura y validando registros…</p>
            <div className="progress"><span className="indeterminate" /></div>
          </div>
        )}
        {error && <div style={{ marginTop: 14 }}><Callout tone="danger" title="El archivo no se puede importar">{error}</Callout></div>}
      </Panel>

      {result && (
        <Callout tone="success" title={`¡Ingesta exitosa! ${int(result.importados)} registros de calidad del aire fueron incorporados`}>
          {int(result.descartados)} registros excluidos por no superar la validación · {int(result.interpolados)} completados por interpolación de mediana
          {result.yaExistentes > 0 && ` · ${int(result.yaExistentes)} ya existían y se omitieron`}. Datos limpios listos para análisis y predicciones.
        </Callout>
      )}

      {upload && b && (
        <>
          <Panel
            title={b.nombreArchivo}
            caption={`${int(b.totalRegistros)} registros detectados · Formato válido (${sourceLabel[b.formato]})`}
            actions={<Badge tone="blue">Lote #{b.id} · pendiente de confirmación</Badge>}
          >
            <div className="kpi-row">
              <Stat label="Válidos" value={int(b.registrosValidos)} color="var(--green)" />
              <Stat label="Con advertencia" value={int(b.registrosAdvertencia)} color="var(--orange)" foot="Se completan por interpolación" />
              <Stat label="Rechazados" value={int(b.registrosRechazados)} color="var(--red)" foot="Excluidos del análisis" />
              <Stat label="Calidad del lote" value={`${Math.round((b.registrosValidos / Math.max(1, b.totalRegistros)) * 100)} %`} />
            </div>
          </Panel>

          <Panel title="Vista previa" caption="Primeras 5 filas tal como se interpretaron" flush>
            <div className="table-wrap">
              <table className="table">
                <thead><tr><th>Fila</th><th>Estación</th><th>Fecha y hora</th><th className="num">PM2.5</th><th className="num">PM10</th><th className="num">Temp.</th><th className="num">Humedad</th></tr></thead>
                <tbody>
                  {upload.vistaPrevia.map((r) => (
                    <tr key={r.fila}>
                      <td className="muted">{r.fila}</td>
                      <td>{r.estacionNombre ?? '—'}</td>
                      <td>{r.fecha ? fmtDateTime(r.fecha) : <span className="delta-bad">{r.fechaOriginal ?? 'vacío'}</span>}</td>
                      <td className="num">{num(r.pm25)}</td><td className="num">{num(r.pm10)}</td>
                      <td className="num">{num(r.temperatura)}</td><td className="num">{num(r.humedad, 0)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Panel>

          <Panel
            title="Reporte de calidad"
            caption={upload.incidencias.length ? `${int(upload.incidencias.length)} problemas encontrados` : 'Ningún problema encontrado'}
            actions={upload.incidencias.length > 0 && <Button size="sm" variant="secondary" icon={<Download size={14} />} onClick={() => exportQuality(b.id)}>Exportar reporte</Button>}
            flush
          >
            {upload.incidencias.length > 0 ? (
              <div className="table-wrap" style={{ maxHeight: 360 }}>
                <table className="table">
                  <thead><tr><th>Fila</th><th>Estación</th><th>Variable</th><th>Valor detectado</th><th>Rango aceptable</th><th>Problema</th><th>Resolución</th></tr></thead>
                  <tbody>
                    {upload.incidencias.map((i) => (
                      <tr key={i.id}>
                        <td className="muted">{i.fila}</td>
                        <td>{i.estacionNombre ?? '—'}</td>
                        <td>{i.variable ?? '—'}</td>
                        <td className="strong">{i.valorDetectado ?? '—'}</td>
                        <td className="muted">{i.rangoAceptable ?? '—'}</td>
                        <td>{issueTypeLabel[i.tipo]}</td>
                        <td><Badge tone={i.resolucion === 'DESCARTADO' ? 'red' : 'orange'} dot>{i.resolucion === 'DESCARTADO' ? 'Descartado' : 'Marcado'}</Badge></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="panel-body row"><CheckCircle2 size={18} style={{ color: 'var(--green)' }} /> Todos los registros superaron la validación.</div>
            )}
            <div className="pagination" style={{ justifyContent: 'flex-end' }}>
              <Button variant="danger" icon={<Trash2 size={15} />} onClick={cancel}>Descartar lote</Button>
              <Button variant="primary" icon={<Wand2 size={15} />} loading={confirming} disabled={usable === 0} onClick={confirm}>
                Aplicar corrección automática y confirmar
              </Button>
            </div>
          </Panel>
        </>
      )}

      <Panel title="Lotes recientes" flush>
        {!batches.data ? <div className="panel-body"><Skeleton height={80} /></div> : batches.data.length === 0 ? (
          <p className="panel-body muted small">Aún no se han cargado archivos.</p>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Lote</th><th>Archivo</th><th className="num">Registros</th><th className="num">Importados</th><th className="num">Rechazados</th><th>Estado</th><th>Cargado por</th><th>Fecha</th><th /></tr></thead>
              <tbody>
                {batches.data.map((x) => (
                  <tr key={x.id}>
                    <td className="muted">#{x.id}</td>
                    <td className="strong">{x.nombreArchivo}</td>
                    <td className="num">{int(x.totalRegistros)}</td>
                    <td className="num">{int(x.registrosImportados)}</td>
                    <td className="num">{int(x.registrosRechazados)}</td>
                    <td><Badge tone={x.estado === 'IMPORTADO' ? 'green' : x.estado === 'CANCELADO' ? 'neutral' : 'blue'}>
                      {x.estado === 'IMPORTADO' ? 'Importado' : x.estado === 'CANCELADO' ? 'Descartado' : 'En revisión'}</Badge></td>
                    <td className="muted">{x.cargadoPor}</td>
                    <td className="muted">{fmtDateTime(x.fechaCreacion)}</td>
                    <td>{x.registrosRechazados + x.registrosAdvertencia > 0 && (
                      <Button size="sm" variant="plain" onClick={() => exportQuality(x.id)}>Reporte</Button>
                    )}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Panel>
    </>
  )
}
