import { lazy, Suspense, type ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { Toaster } from 'sonner'
import { AppShell, NAVIGATION } from './components/AppShell'
import { Skeleton } from './components/ui'
import { homeFor, useAuth } from './lib/auth'
import { StationsProvider } from './lib/stations'
import { LoginPage } from './pages/LoginPage'

const pages: Record<string, React.LazyExoticComponent<() => ReactNode>> = {
  '/condiciones': lazy(() => import('./pages/ConditionsPage')),
  '/pronostico': lazy(() => import('./pages/ForecastPage')),
  '/alertas': lazy(() => import('./pages/AlertsPage')),
  '/historico': lazy(() => import('./pages/HistoryPage')),
  '/comparativa': lazy(() => import('./pages/ComparisonPage')),
  '/visualizacion': lazy(() => import('./pages/VisualizationPage')),
  '/indicadores': lazy(() => import('./pages/IndicatorsPage')),
  '/anomalias': lazy(() => import('./pages/AnomaliesPage')),
  '/ingesta': lazy(() => import('./pages/IngestionPage')),
  '/modelos': lazy(() => import('./pages/ModelsPage')),
  '/reportes': lazy(() => import('./pages/ReportsPage')),
  '/estaciones': lazy(() => import('./pages/StationsPage')),
  '/usuarios': lazy(() => import('./pages/UsersPage')),
  '/monitoreo': lazy(() => import('./pages/MonitoringPage')),
}

function PageFallback() {
  return (
    <div className="stack">
      <Skeleton height={34} width={280} />
      <Skeleton height={16} width={420} />
      <div className="grid grid-3" style={{ marginTop: 8 }}>
        <Skeleton height={120} radius={14} />
        <Skeleton height={120} radius={14} />
        <Skeleton height={120} radius={14} />
      </div>
      <Skeleton height={320} radius={14} />
    </div>
  )
}

function Protected() {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return null
  if (!user) return <Navigate to="/acceso" replace state={{ from: location.pathname }} />

  const allowed = NAVIGATION.flatMap((g) => g.items).filter((i) => i.roles.includes(user.rol))
  return (
    <StationsProvider>
      <AppShell>
        <Suspense fallback={<PageFallback />}>
          <Routes>
            {allowed.map((item) => {
              const Page = pages[item.to]
              return <Route key={item.to} path={item.to} element={<Page />} />
            })}
            <Route path="*" element={<Navigate to={homeFor(user.rol)} replace />} />
          </Routes>
        </Suspense>
      </AppShell>
    </StationsProvider>
  )
}

export function App() {
  return (
    <>
      <Routes>
        <Route path="/acceso" element={<LoginPage />} />
        <Route path="/*" element={<Protected />} />
      </Routes>
      <Toaster position="top-right" richColors closeButton toastOptions={{ style: { fontFamily: 'var(--font)' } }} />
    </>
  )
}
