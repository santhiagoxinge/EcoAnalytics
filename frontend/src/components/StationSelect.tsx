import { useStations } from '../lib/stations'
import { stationStatusLabel } from '../lib/format'

/** Station picker bound to the shared selection. */
export function StationSelect({ onlyReporting = false, label = 'Estación' }: { onlyReporting?: boolean; label?: string }) {
  const { stations, selectedId, select } = useStations()
  const options = onlyReporting ? stations.filter((s) => s.estado === 'EN_LINEA') : stations
  const value = options.some((s) => s.id === selectedId) ? selectedId! : options[0]?.id

  return (
    <label className="row" style={{ gap: 8 }}>
      <span className="sr-only">{label}</span>
      <select
        className="select"
        style={{ width: 'auto', minWidth: 230 }}
        value={value ?? ''}
        onChange={(e) => select(Number(e.target.value))}
        aria-label={label}
      >
        {options.map((s) => (
          <option key={s.id} value={s.id}>
            {s.nombre}{s.estado !== 'EN_LINEA' ? ` · ${stationStatusLabel[s.estado]}` : ''}
          </option>
        ))}
      </select>
    </label>
  )
}

/** The effective station for views that only accept reporting stations. */
export function useEffectiveStation(onlyReporting = false) {
  const { stations, selectedId } = useStations()
  const options = onlyReporting ? stations.filter((s) => s.estado === 'EN_LINEA') : stations
  return options.find((s) => s.id === selectedId) ?? options[0] ?? null
}
