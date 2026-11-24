import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, setUnauthorizedHandler, tokenStore } from './api'
import type { Permiso, Rol, Usuario } from './types'

interface AuthState {
  user: Usuario | null
  loading: boolean
  login: (correo: string, contrasena: string) => Promise<Usuario>
  logout: () => void
  can: (permiso: Permiso) => boolean
  hasRole: (...roles: Rol[]) => boolean
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<Usuario | null>(null)
  const [loading, setLoading] = useState(() => tokenStore.get() !== null)

  const logout = useCallback(() => {
    tokenStore.set(null)
    setUser(null)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(logout)
    if (!tokenStore.get()) return
    api.get<Usuario>('/perfil')
      .then(setUser)
      .catch(() => tokenStore.set(null))
      .finally(() => setLoading(false))
  }, [logout])

  const login = useCallback(async (correo: string, contrasena: string) => {
    const sesion = await api.post<{ token: string; usuario: Usuario }>('/auth/login', { correo, contrasena })
    tokenStore.set(sesion.token)
    setUser(sesion.usuario)
    return sesion.usuario
  }, [])

  const value = useMemo<AuthState>(() => ({
    user,
    loading,
    login,
    logout,
    can: (permiso) => user?.permisos.includes(permiso) ?? false,
    hasRole: (...roles) => (user ? roles.includes(user.rol) : false),
  }), [user, loading, login, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}

/** Vista de inicio según el rol (HU-01). */
export function homeFor(rol: Rol): string {
  switch (rol) {
    case 'ADMINISTRADOR': return '/monitoreo'
    case 'ANALISTA': return '/pronostico'
    default: return '/condiciones'
  }
}
