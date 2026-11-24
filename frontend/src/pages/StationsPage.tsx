import L from 'leaflet'
import { Pencil, Plus, Power, Search, Wrench } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { MapContainer, Marker, TileLayer, Tooltip, useMap } from 'react-leaflet'
import { toast } from 'sonner'
import { Sheet } from '../components/Sheet'
import { Badge, Button, EmptyState, Field, PageHeader, Panel, type BadgeTone } from '../components/ui'
import { api, ApiError } from '../lib/api'
import { fmtDateTime, minutesSince, relativeMinutes, sourceLabel, stationStatusLabel } from '../lib/format'
import { useStations } from '../lib/stations'
import type { Estacion as Station, EstadoEstacion as StationStatus, TipoFuente as DataSourceType } from '../lib/types'

const statusTone: Record<StationStatus, BadgeTone> = { EN_LINEA: 'green', MANTENIMIENTO: 'orange', FUERA_DE_LINEA: 'red' }
const statusColor: Record<StationStatus, string> = { EN_LINEA: '#34c759', MANTENIMIENTO: '#ff9500', FUERA_DE_LINEA: '#ff3b30' }
const VARIABLES = ['PM2.5', 'PM10', 'Temp', 'Humedad']

/** El backend nombra los campos en español; el formulario usa sus propios identificadores. */
const CAMPOS_FORMULARIO: Record<string, string> = { nombre: 'name', zona: 'district', latitud: 'coordinates', longitud: 'coordinates' }
const mapearCampos = (campos: Record<string, string>) =>
  Object.fromEntries(Object.entries(campos).map(([k, v]) => [CAMPOS_FORMULARIO[k] ?? k, v]))

function pin(status: StationStatus, selected: boolean) {
  return L.divIcon({
    className: '',
    html: `<div class="station-pin" data-selected="${selected}" style="background:${statusColor[status]}"></div>`,
    iconSize: [18, 18],
    iconAnchor: [9, 9],
  })
}

function FlyTo({ station }: { station: Station | undefined }) {
  const map = useMap()
  useEffect(() => {
    if (station) map.flyTo([station.latitud, station.longitud], Math.max(map.getZoom(), 13), { duration: 0.6 })
  }, [station, map])
  return null
}

export default function StationsPage() {
  const { stations, reload } = useStations()
  const [query, setQuery] = useState('')
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [editing, setEditing] = useState<Station | 'new' | null>(null)
  const [busy, setBusy] = useState(false)

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    return q ? stations.filter((s) => `${s.nombre} ${s.zona}`.toLowerCase().includes(q)) : stations
  }, [stations, query])

  useEffect(() => {
    if (filtered.length === 1) setSelectedId(filtered[0].id)
  }, [filtered])

  const selected = stations.find((s) => s.id === selectedId)
  const center: [number, number] = stations.length
    ? [stations.reduce((a, s) => a + s.latitud, 0) / stations.length, stations.reduce((a, s) => a + s.longitud, 0) / stations.length]
    : [6.2442, -75.5812]

  const changeStatus = async (station: Station, status: StationStatus) => {
    setBusy(true)
    try {
      await api.patch(`/estaciones/${station.id}/estado`, { estado: status })
      toast.success(`${station.nombre}: ${stationStatusLabel[status]}`, {
        description: status === 'MANTENIMIENTO' ? 'Se pausan las alertas por pérdida de señal de esta estación.' : undefined,
      })
      await reload()
    } catch (e) {
      toast.error((e as ApiError).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <PageHeader
        title="Red de estaciones y sensores"
        subtitle="Registre, ubique y supervise las fuentes de datos de calidad del aire."
        actions={<Button variant="primary" icon={<Plus size={16} />} onClick={() => setEditing('new')}>Registrar estación</Button>}
      />

      <div className="grid grid-side-main">
        <div className="stack" style={{ gap: 16 }}>
          <Panel flush>
            <div style={{ padding: '14px 16px 4px' }}>
              <div className="input-search">
                <Search size={15} aria-hidden />
                <input className="input" placeholder="Buscar por nombre o comuna" value={query} onChange={(e) => setQuery(e.target.value)} aria-label="Buscar estaciones" />
              </div>
            </div>
            {filtered.length === 0 ? <EmptyState title="Sin coincidencias">Ninguna estación coincide con “{query}”.</EmptyState> : (
              <ul className="station-list">
                {filtered.map((s) => (
                  <li key={s.id}>
                    <button onClick={() => setSelectedId(s.id)} style={selectedId === s.id ? { background: 'var(--accent-soft)' } : undefined}>
                      <span className="dot" style={{ background: statusColor[s.estado] }} />
                      <span style={{ flex: 1, textAlign: 'left' }}>
                        <span className="strong">{s.nombre}</span>
                        <span className="small muted" style={{ display: 'block' }}>{s.zona}</span>
                      </span>
                      <Badge tone={statusTone[s.estado]}>{stationStatusLabel[s.estado]}</Badge>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </Panel>

          {selected && (
            <Panel title={selected.nombre} caption={selected.zona}
              actions={<Button size="sm" variant="plain" icon={<Pencil size={14} />} onClick={() => setEditing(selected)}>Editar</Button>}>
              <dl className="details">
                <dt>Estado</dt><dd><Badge tone={statusTone[selected.estado]} dot>{stationStatusLabel[selected.estado]}</Badge></dd>
                <dt>Coordenadas</dt><dd className="tabular">{selected.latitud.toFixed(4)}, {selected.longitud.toFixed(4)}</dd>
                <dt>Tipo de fuente</dt><dd>{sourceLabel[selected.tipoFuente]}</dd>
                <dt>Variables</dt><dd>{selected.variables}</dd>
                <dt>Última medición</dt><dd>{selected.ultimaMedicion ? `${fmtDateTime(selected.ultimaMedicion)} (${relativeMinutes(minutesSince(selected.ultimaMedicion))})` : 'Sin mediciones'}</dd>
                <dt>Instalada</dt><dd>{fmtDateTime(selected.fechaInstalacion)}</dd>
              </dl>
              <div className="row" style={{ marginTop: 14 }}>
                {selected.estado !== 'MANTENIMIENTO' && (
                  <Button size="sm" variant="secondary" icon={<Wrench size={14} />} disabled={busy} onClick={() => changeStatus(selected, 'MANTENIMIENTO')}>Poner en mantenimiento</Button>
                )}
                {selected.estado !== 'EN_LINEA' && (
                  <Button size="sm" variant="success" icon={<Power size={14} />} disabled={busy} onClick={() => changeStatus(selected, 'EN_LINEA')}>Reactivar</Button>
                )}
                {selected.estado === 'EN_LINEA' && (
                  <Button size="sm" variant="danger" icon={<Power size={14} />} disabled={busy} onClick={() => changeStatus(selected, 'FUERA_DE_LINEA')}>Marcar fuera de línea</Button>
                )}
              </div>
            </Panel>
          )}
        </div>

        <div className="map-frame">
          <MapContainer center={center} zoom={12} scrollWheelZoom style={{ height: '100%' }}>
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
              url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            {filtered.map((s) => (
              <Marker key={s.id} position={[s.latitud, s.longitud]} icon={pin(s.estado, s.id === selectedId)}
                eventHandlers={{ click: () => setSelectedId(s.id) }}>
                <Tooltip direction="top" offset={[0, -10]}>{s.nombre} · {stationStatusLabel[s.estado]}</Tooltip>
              </Marker>
            ))}
            <FlyTo station={selected} />
          </MapContainer>
        </div>
      </div>

      <StationSheet station={editing} onClose={() => setEditing(null)} onSaved={async (s) => {
        setEditing(null)
        await reload()
        setSelectedId(s.id)
      }} />
    </>
  )
}

function parseCoordinates(text: string): [number, number] | null {
  const m = /^\s*(-?\d+(?:\.\d+)?)\s*[,;\s]\s*(-?\d+(?:\.\d+)?)\s*$/.exec(text)
  if (!m) return null
  const lat = Number(m[1])
  const lon = Number(m[2])
  return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180 ? [lat, lon] : null
}

function StationSheet({ station, onClose, onSaved }: { station: Station | 'new' | null; onClose: () => void; onSaved: (s: Station) => void }) {
  const editing = station !== null && station !== 'new' ? station : null
  const [name, setName] = useState('')
  const [district, setDistrict] = useState('')
  const [coordinates, setCoordinates] = useState('')
  const [sourceType, setSourceType] = useState<DataSourceType>('SENSOR')
  const [variables, setVariables] = useState<string[]>(VARIABLES)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (station === null) return
    setErrors({})
    setName(editing?.nombre ?? '')
    setDistrict(editing?.zona ?? '')
    setCoordinates(editing ? `${editing.latitud}, ${editing.longitud}` : '')
    setSourceType(editing?.tipoFuente ?? 'SENSOR')
    setVariables(editing ? editing.variables.split(',').map((v) => v.trim()) : VARIABLES)
  }, [station, editing])

  const coords = parseCoordinates(coordinates)

  const save = async () => {
    const e: Record<string, string> = {}
    if (!name.trim()) e.name = 'El nombre de la estación es obligatorio.'
    if (!district.trim()) e.district = 'La comuna o zona es obligatoria.'
    if (!coords) e.coordinates = 'Coordenadas inválidas. Use el formato latitud, longitud (ej. 6.2510, -75.5920).'
    if (!variables.length) e.variables = 'Seleccione al menos una variable.'
    setErrors(e)
    if (Object.keys(e).length) return
    setSaving(true)
    try {
      const body = { nombre: name, zona: district, latitud: coords![0], longitud: coords![1], tipoFuente: sourceType, variables: variables.join(', ') }
      const saved = editing ? await api.put<Station>(`/estaciones/${editing.id}`, body) : await api.post<Station>('/estaciones', body)
      toast.success(editing ? 'Estación actualizada' : 'Estación registrada y conectada', { description: `${saved.nombre} · estado inicial En línea` })
      onSaved(saved)
    } catch (err) {
      const apiError = err as ApiError
      setErrors(mapearCampos(apiError.fields))
      toast.error(apiError.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Sheet open={station !== null} onClose={onClose} title={editing ? `Editar ${editing.nombre}` : 'Registrar estación'} footer={
      <><Button onClick={onClose}>Cancelar</Button><Button variant="primary" loading={saving} onClick={save}>{editing ? 'Guardar cambios' : 'Guardar y conectar'}</Button></>
    }>
      <Field label="Nombre" htmlFor="st-name" error={errors.name}>
        <input id="st-name" className="input" value={name} onChange={(e) => setName(e.target.value)} placeholder="Estación Occidente La América" aria-invalid={!!errors.name} />
      </Field>
      <Field label="Comuna o zona" htmlFor="st-district" error={errors.district}>
        <input id="st-district" className="input" value={district} onChange={(e) => setDistrict(e.target.value)} placeholder="Comuna 12 - La América" aria-invalid={!!errors.district} />
      </Field>
      <Field label="Coordenadas" htmlFor="st-coords" error={errors.coordinates}
        hint={coordinates && coords ? `Coordenadas válidas: ${coords[0]}, ${coords[1]}` : 'Latitud, longitud en grados decimales'}>
        <input id="st-coords" className="input tabular" value={coordinates} onChange={(e) => setCoordinates(e.target.value)} placeholder="6.2510, -75.5920" aria-invalid={!!errors.coordinates} />
      </Field>
      <Field label="Tipo de fuente" htmlFor="st-source">
        <select id="st-source" className="select" value={sourceType} onChange={(e) => setSourceType(e.target.value as DataSourceType)}>
          {(Object.keys(sourceLabel) as DataSourceType[]).map((t) => <option key={t} value={t}>{sourceLabel[t]}</option>)}
        </select>
      </Field>
      <div className="field">
        <span className="field-label">Variables de medición</span>
        <div className="row" style={{ gap: 16 }}>
          {VARIABLES.map((v) => (
            <label key={v} className="check">
              <input type="checkbox" checked={variables.includes(v)}
                onChange={(e) => setVariables((prev) => (e.target.checked ? [...prev, v] : prev.filter((x) => x !== v)))} />
              {v}
            </label>
          ))}
        </div>
        {errors.variables && <span className="field-error">{errors.variables}</span>}
      </div>
    </Sheet>
  )
}
