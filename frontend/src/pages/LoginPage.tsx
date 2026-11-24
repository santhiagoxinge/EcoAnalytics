import { Leaf, Lock, Mail, User as UserIcon } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { Button, Callout, Field, Segmented } from '../components/ui'
import { api, ApiError } from '../lib/api'
import { homeFor, useAuth } from '../lib/auth'
import { aqiCategoryLabel, aqiFill, aqiInk, fmtHour, num } from '../lib/format'
import type { ResumenEstacion, Rol } from '../lib/types'
import './login.css'

type Mode = 'login' | 'register'

const DEMO_ACCOUNTS = [
  { role: 'Administrador', email: 'admin@ecoanalytics.org', password: 'Admin123!' },
  { role: 'Analista', email: 'analista@ecoanalytics.org', password: 'Analista123!' },
  { role: 'Ciudadano', email: 'ciudadano@ecoanalytics.org', password: 'Ciudadano123!' },
]

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** El backend nombra los campos en español; el formulario usa sus propios identificadores. */
const CAMPOS_FORMULARIO: Record<string, string> = { nombreCompleto: 'fullName', correo: 'email', contrasena: 'password' }
const mapearCampos = (campos: Record<string, string>) =>
  Object.fromEntries(Object.entries(campos).map(([k, v]) => [CAMPOS_FORMULARIO[k] ?? k, v]))

export function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [mode, setMode] = useState<Mode>('login')
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<Rol>('CIUDADANO')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (user) return <Navigate to={homeFor(user.rol)} replace />

  const validate = () => {
    const e: Record<string, string> = {}
    if (mode === 'register' && !fullName.trim()) e.fullName = 'El nombre es obligatorio.'
    if (!email.trim()) e.email = 'El campo de correo electrónico es obligatorio.'
    else if (!EMAIL_PATTERN.test(email.trim())) e.email = 'Ingrese un formato de correo electrónico válido.'
    if (!password) e.password = 'La contraseña es obligatoria.'
    else if (mode === 'register' && password.length < 8) e.password = 'La contraseña debe contener al menos 8 caracteres.'
    setErrors(e)
    return Object.keys(e).length === 0
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setFormError(null)
    if (!validate()) return
    setSubmitting(true)
    try {
      if (mode === 'login') {
        const signedIn = await login(email.trim(), password)
        navigate(homeFor(signedIn.rol), { replace: true })
      } else {
        const created = await api.post<{ estado: string }>('/auth/registro', { nombreCompleto: fullName, correo: email.trim(), contrasena: password, rol: role })
        if (created.estado === 'PENDIENTE') {
          toast.success('Registro recibido', { description: 'Su cuenta de analista quedará activa cuando un administrador la verifique.' })
          setMode('login')
        } else {
          toast.success('¡Registro exitoso!', { description: 'Su cuenta de ciudadano está lista. Iniciando sesión…' })
          const signedIn = await login(email.trim(), password)
          navigate(homeFor(signedIn.rol), { replace: true })
        }
      }
    } catch (e) {
      if (e instanceof ApiError) {
        setFormError(e.message)
        setErrors(mapearCampos(e.fields))
      }
    } finally {
      setSubmitting(false)
    }
  }

  const switchMode = (next: Mode) => {
    setMode(next)
    setErrors({})
    setFormError(null)
  }

  return (
    <div className="login-screen">
      <div className="login-card">
        <aside className="login-hero">
          <div className="row" style={{ gap: 12 }}>
            <div className="brand-mark" style={{ width: 40, height: 40, borderRadius: 11 }}><Leaf size={22} strokeWidth={2.4} /></div>
            <div>
              <div className="login-brand">EcoAnalytics</div>
              <div className="muted small">Análisis y monitoreo de calidad del aire</div>
            </div>
          </div>
          <LiveAirWidget />
          <p className="login-tagline">
            Recibir, entender, analizar, aprender, predecir y alertar: información ambiental para decidir mejor.
          </p>
        </aside>

        <section className="login-form-side">
          <Segmented<Mode>
            label="Modo de acceso"
            value={mode}
            onChange={switchMode}
            options={[{ value: 'login', label: 'Iniciar sesión' }, { value: 'register', label: 'Crear cuenta' }]}
          />

          <form className="stack" style={{ gap: 14, marginTop: 22 }} onSubmit={submit} noValidate>
            {mode === 'register' && (
              <Field label="Nombre completo" htmlFor="fullName" error={errors.fullName}>
                <div className="input-icon">
                  <UserIcon size={16} aria-hidden />
                  <input id="fullName" className="input" value={fullName} onChange={(e) => setFullName(e.target.value)}
                    placeholder="Carlos Mendoza" autoComplete="name" aria-invalid={!!errors.fullName} />
                </div>
              </Field>
            )}
            <Field label="Correo electrónico" htmlFor="email" error={errors.email}>
              <div className="input-icon">
                <Mail size={16} aria-hidden />
                <input id="email" className="input" type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                  placeholder="nombre@ecoanalytics.org" autoComplete="email" aria-invalid={!!errors.email} />
              </div>
            </Field>
            <Field label="Contraseña" htmlFor="password" error={errors.password}
              hint={mode === 'register' ? 'Mínimo 8 caracteres.' : undefined}>
              <div className="input-icon">
                <Lock size={16} aria-hidden />
                <input id="password" className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} aria-invalid={!!errors.password} />
              </div>
            </Field>
            {mode === 'register' && (
              <div className="field">
                <span className="field-label">Perfil</span>
                <Segmented<Rol>
                  label="Perfil solicitado"
                  value={role}
                  onChange={setRole}
                  options={[{ value: 'CIUDADANO', label: 'Ciudadano' }, { value: 'ANALISTA', label: 'Analista ambiental' }]}
                />
                {role === 'ANALISTA' && (
                  <span className="field-hint">Las cuentas de analista requieren verificación institucional de un administrador.</span>
                )}
              </div>
            )}

            {formError && <Callout tone="danger">{formError}</Callout>}

            <Button type="submit" variant="primary" size="lg" loading={submitting} style={{ marginTop: 4 }}>
              {mode === 'login' ? 'Iniciar sesión' : 'Crear cuenta EcoAnalytics'}
            </Button>
          </form>

          {mode === 'login' && (
            <div className="demo-accounts">
              <p className="small muted">Cuentas de demostración</p>
              <div className="row" style={{ gap: 6 }}>
                {DEMO_ACCOUNTS.map((a) => (
                  <button key={a.email} type="button" className="btn btn-sm btn-secondary"
                    onClick={() => { setEmail(a.email); setPassword(a.password); setErrors({}); setFormError(null) }}>
                    {a.role}
                  </button>
                ))}
              </div>
            </div>
          )}
        </section>
      </div>
    </div>
  )
}

/** Public, real-time snapshot of the station network. */
function LiveAirWidget() {
  const [stations, setStations] = useState<ResumenEstacion[] | null>(null)

  useEffect(() => {
    api.get<ResumenEstacion[]>('/condiciones').then(setStations).catch(() => setStations([]))
  }, [])

  const available = (stations ?? []).filter((s) => s.disponible)
  const main = available.find((s) => s.nombre === 'Estación Centro') ?? available[0]

  return (
    <div className="live-widget">
      <div className="row between">
        <span className="strong small">Calidad del aire ahora</span>
        <span className="row small muted" style={{ gap: 6 }}><span className="live-dot" /> En vivo</span>
      </div>
      {main ? (
        <>
          <div className="row" style={{ gap: 14, marginTop: 10, alignItems: 'flex-end' }}>
            <div className="tabular" style={{ fontSize: 44, fontWeight: 700, letterSpacing: '-0.04em', lineHeight: 1, color: aqiInk(main.categoria) }}>
              {main.ica}
            </div>
            <div style={{ paddingBottom: 4 }}>
              <div className="strong" style={{ color: aqiInk(main.categoria) }}>{aqiCategoryLabel[main.categoria!]}</div>
              <div className="small muted">ICA · {main.nombre}{main.fechaMedicion ? ` · ${fmtHour(main.fechaMedicion)}` : ''}</div>
            </div>
          </div>
          <ul className="live-list">
            {available.filter((s) => s.id !== main.id).map((s) => (
              <li key={s.id}>
                <span className="row" style={{ gap: 8 }}>
                  <span className="dot" style={{ background: aqiFill(s.categoria) }} />{s.nombre}
                </span>
                <span className="tabular small muted">PM2.5 {num(s.pm25)} · <strong style={{ color: aqiInk(s.categoria) }}>ICA {s.ica}</strong></span>
              </li>
            ))}
          </ul>
        </>
      ) : (
        <p className="small muted" style={{ marginTop: 10 }}>{stations === null ? 'Cargando mediciones…' : 'Sin conexión con las estaciones.'}</p>
      )}
    </div>
  )
}
