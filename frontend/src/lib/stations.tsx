import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { api } from './api'
import type { Estacion } from './types'

interface StationsState {
  stations: Estacion[]
  loading: boolean
  reload: () => Promise<void>
  /** Station selected across views, so moving between modules keeps the same context. */
  selectedId: number | null
  select: (id: number) => void
}

const StationsContext = createContext<StationsState | null>(null)
const SELECTED_KEY = 'ecoanalytics.station'

function readSelected(): number | null {
  try {
    const v = localStorage.getItem(SELECTED_KEY)
    return v ? Number(v) : null
  } catch {
    return null
  }
}

export function StationsProvider({ children }: { children: ReactNode }) {
  const [stations, setStations] = useState<Estacion[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedId, setSelectedId] = useState<number | null>(readSelected)

  const reload = useCallback(async () => {
    try {
      const list = await api.get<Estacion[]>('/estaciones')
      setStations(list)
      setSelectedId((current) => {
        if (current && list.some((s) => s.id === current)) return current
        return (list.find((s) => s.nombre === 'Estación Centro') ?? list.find((s) => s.estado === 'EN_LINEA') ?? list[0])?.id ?? null
      })
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void reload()
  }, [reload])

  const select = useCallback((id: number) => {
    setSelectedId(id)
    try {
      localStorage.setItem(SELECTED_KEY, String(id))
    } catch {
      /* preference only */
    }
  }, [])

  return (
    <StationsContext.Provider value={{ stations, loading, reload, selectedId, select }}>
      {children}
    </StationsContext.Provider>
  )
}

export function useStations(): StationsState {
  const ctx = useContext(StationsContext)
  if (!ctx) throw new Error('useStations must be used inside StationsProvider')
  return ctx
}
