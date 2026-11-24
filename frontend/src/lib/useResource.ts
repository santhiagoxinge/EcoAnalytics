import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError } from './api'

interface Resource<T> {
  data: T | null
  error: ApiError | null
  loading: boolean
  reload: () => Promise<void>
  setData: (updater: (prev: T | null) => T | null) => void
}

/**
 * Loads data whenever the key changes. Keeps showing the previous data while reloading,
 * so filters update in place instead of flashing an empty state.
 */
export function useResource<T>(key: string | null, loader: () => Promise<T>): Resource<T> {
  const [data, setDataState] = useState<T | null>(null)
  const [error, setError] = useState<ApiError | null>(null)
  const [loading, setLoading] = useState(key !== null)
  const loaderRef = useRef(loader)
  loaderRef.current = loader
  const requestId = useRef(0)

  const run = useCallback(async () => {
    if (key === null) return
    const id = ++requestId.current
    setLoading(true)
    try {
      const result = await loaderRef.current()
      if (id === requestId.current) {
        setDataState(result)
        setError(null)
      }
    } catch (e) {
      if (id === requestId.current) setError(e instanceof ApiError ? e : new ApiError(0, String(e)))
    } finally {
      if (id === requestId.current) setLoading(false)
    }
  }, [key])

  useEffect(() => {
    void run()
  }, [run])

  const setData = useCallback((updater: (prev: T | null) => T | null) => setDataState(updater), [])

  return { data, error, loading, reload: run, setData }
}
