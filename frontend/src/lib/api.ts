const TOKEN_KEY = 'ecoanalytics.token'

export class ApiError extends Error {
  status: number
  fields: Record<string, string>

  constructor(status: number, message: string, fields: Record<string, string> = {}) {
    super(message)
    this.status = status
    this.fields = fields
  }
}

export const tokenStore = {
  get(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY)
    } catch {
      return null
    }
  },
  set(token: string | null) {
    try {
      if (token) localStorage.setItem(TOKEN_KEY, token)
      else localStorage.removeItem(TOKEN_KEY)
    } catch {
      /* storage unavailable: the session simply won't persist */
    }
  },
}

let onUnauthorized: (() => void) | null = null
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

async function request(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers)
  const token = tokenStore.get()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (init.body && !(init.body instanceof FormData)) headers.set('Content-Type', 'application/json')

  let response: Response
  try {
    response = await fetch(`/api${path}`, { ...init, headers })
  } catch {
    throw new ApiError(0, 'No hay conexión con el servidor de EcoAnalytics. Verifique que el backend esté en ejecución.')
  }

  if (!response.ok) {
    let message = 'Ocurrió un error inesperado.'
    let fields: Record<string, string> = {}
    try {
      const body = await response.json()
      message = body.mensaje ?? message
      fields = body.campos ?? {}
    } catch {
      /* non-JSON error body */
    }
    if (response.status === 401 && token && !path.startsWith('/auth/')) {
      onUnauthorized?.()
      message = 'Su sesión expiró. Inicie sesión nuevamente.'
    }
    throw new ApiError(response.status, message, fields)
  }
  return response
}

export const api = {
  async get<T>(path: string): Promise<T> {
    return (await request(path)).json()
  },
  async post<T>(path: string, body?: unknown): Promise<T> {
    const r = await request(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) })
    const text = await r.text()
    return (text ? JSON.parse(text) : undefined) as T
  },
  async put<T>(path: string, body: unknown): Promise<T> {
    return (await request(path, { method: 'PUT', body: JSON.stringify(body) })).json()
  },
  async patch<T>(path: string, body: unknown): Promise<T> {
    return (await request(path, { method: 'PATCH', body: JSON.stringify(body) })).json()
  },
  async upload<T>(path: string, file: File): Promise<T> {
    const form = new FormData()
    form.append('archivo', file)
    return (await request(path, { method: 'POST', body: form })).json()
  },
  /** Downloads a file response (GET, or POST when a body is given) and saves it with the server's name. */
  async download(path: string, body?: unknown): Promise<string> {
    const r = await request(path, body === undefined ? {} : { method: 'POST', body: JSON.stringify(body) })
    const disposition = r.headers.get('Content-Disposition') ?? ''
    const match = /filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/i.exec(disposition)
    const fileName = decodeURIComponent(match?.[1] ?? match?.[2] ?? 'ecoanalytics.dat')
    const blob = await r.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    a.remove()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
    return fileName
  },
}

export function qs(params: Record<string, string | number | boolean | undefined | null>): string {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') search.set(k, String(v))
  })
  return search.toString()
}
