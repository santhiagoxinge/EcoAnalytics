import {
  Bell, BrainCircuit, ChartSpline, CloudSun, FileText, Gauge, GitCompareArrows, History, Leaf, LogOut, Menu,
  RadioTower, ScanSearch, Server, Sigma, Upload, Users, type LucideIcon,
} from 'lucide-react'
import { useEffect, useState, type ReactNode } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { api } from '../lib/api'
import { useAuth } from '../lib/auth'
import { initials, roleLabel } from '../lib/format'
import type { Rol } from '../lib/types'

export interface NavEntry {
  to: string
  label: string
  icon: LucideIcon
  roles: Rol[]
}

const ALL: Rol[] = ['ADMINISTRADOR', 'ANALISTA', 'CIUDADANO']
const STAFF: Rol[] = ['ADMINISTRADOR', 'ANALISTA']
const ADMIN: Rol[] = ['ADMINISTRADOR']

export const NAVIGATION: { group: string; items: NavEntry[] }[] = [
  {
    group: 'Monitoreo',
    items: [
      { to: '/condiciones', label: 'Condiciones actuales', icon: Gauge, roles: ALL },
      { to: '/pronostico', label: 'Pronóstico', icon: CloudSun, roles: ALL },
      { to: '/alertas', label: 'Alertas', icon: Bell, roles: ALL },
    ],
  },
  {
    group: 'Análisis',
    items: [
      { to: '/historico', label: 'Datos históricos', icon: History, roles: ALL },
      { to: '/comparativa', label: 'Comparativa de periodos', icon: GitCompareArrows, roles: ALL },
      { to: '/visualizacion', label: 'Visualización', icon: ChartSpline, roles: ALL },
      { to: '/indicadores', label: 'Indicadores', icon: Sigma, roles: STAFF },
      { to: '/anomalias', label: 'Anomalías', icon: ScanSearch, roles: STAFF },
    ],
  },
  {
    group: 'Datos y modelos',
    items: [
      { to: '/ingesta', label: 'Ingesta y validación', icon: Upload, roles: STAFF },
      { to: '/modelos', label: 'Modelos predictivos', icon: BrainCircuit, roles: STAFF },
      { to: '/reportes', label: 'Reportes', icon: FileText, roles: STAFF },
    ],
  },
  {
    group: 'Administración',
    items: [
      { to: '/estaciones', label: 'Estaciones', icon: RadioTower, roles: ADMIN },
      { to: '/usuarios', label: 'Usuarios y roles', icon: Users, roles: ADMIN },
      { to: '/monitoreo', label: 'Monitoreo del sistema', icon: Server, roles: ADMIN },
    ],
  },
]

export function titleFor(pathname: string): string {
  for (const g of NAVIGATION) for (const i of g.items) if (pathname.startsWith(i.to)) return i.label
  return 'EcoAnalytics'
}

export function AppShell({ children }: { children: ReactNode }) {
  const { user, logout, hasRole } = useAuth()
  const location = useLocation()
  const [menuOpen, setMenuOpen] = useState(false)
  const [pendingAnomalies, setPendingAnomalies] = useState(0)

  useEffect(() => setMenuOpen(false), [location.pathname])

  useEffect(() => {
    if (!hasRole('ADMINISTRADOR', 'ANALISTA')) return
    api.get<{ estadisticas: { pendientes: number } }>('/anomalias')
      .then((r) => setPendingAnomalies(r.estadisticas.pendientes))
      .catch(() => undefined)
  }, [hasRole, location.pathname])

  if (!user) return null
  const title = titleFor(location.pathname)

  return (
    <div className="app">
      {menuOpen && <div className="sidebar-scrim" onClick={() => setMenuOpen(false)} aria-hidden />}
      <aside className="sidebar" data-open={menuOpen} aria-label="Navegación principal">
        <div className="brand">
          <div className="brand-mark"><Leaf size={17} strokeWidth={2.4} /></div>
          <div>
            <div className="brand-name">EcoAnalytics</div>
            <div className="brand-sub">Calidad del aire</div>
          </div>
        </div>

        <nav>
          {NAVIGATION.map((group) => {
            const items = group.items.filter((i) => i.roles.includes(user.rol))
            if (!items.length) return null
            return (
              <div className="nav-group" key={group.group}>
                <div className="nav-group-label">{group.group}</div>
                {items.map((item) => (
                  <NavLink key={item.to} to={item.to} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
                    <item.icon size={17} strokeWidth={2} aria-hidden />
                    <span>{item.label}</span>
                    {item.to === '/anomalias' && pendingAnomalies > 0 && (
                      <span className="nav-count" aria-label={`${pendingAnomalies} pendientes`}>{pendingAnomalies}</span>
                    )}
                  </NavLink>
                ))}
              </div>
            )
          })}
        </nav>

        <div className="sidebar-footer">
          <div className="user-chip">
            <div className="avatar" aria-hidden>{initials(user.nombreCompleto)}</div>
            <div style={{ minWidth: 0, flex: 1 }}>
              <div className="user-name">{user.nombreCompleto}</div>
              <div className="user-role">{roleLabel[user.rol]}</div>
            </div>
            <button className="btn btn-plain btn-icon btn-sm" onClick={logout} aria-label="Cerrar sesión" title="Cerrar sesión">
              <LogOut size={16} />
            </button>
          </div>
        </div>
      </aside>

      <div className="main">
        <header className="toolbar">
          <button className="btn btn-plain btn-icon menu-button" onClick={() => setMenuOpen(true)} aria-label="Abrir menú">
            <Menu size={19} />
          </button>
          <div className="toolbar-title">{title}</div>
          <div className="toolbar-actions">
            <span className="badge badge-green" title="Las estaciones en línea reportan cada hora">
              <span className="live-dot" aria-hidden /> Datos en vivo
            </span>
          </div>
        </header>
        <main key={location.pathname} className="page">{children}</main>
      </div>
    </div>
  )
}
