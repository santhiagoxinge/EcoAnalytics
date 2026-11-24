import { Check, Minus, Pencil, Plus, Search } from 'lucide-react'
import { useEffect, useState } from 'react'
import { toast } from 'sonner'
import { Sheet } from '../components/Sheet'
import { Badge, Button, ErrorState, Field, PageHeader, Panel, Skeleton, type BadgeTone } from '../components/ui'
import { api, ApiError } from '../lib/api'
import { useAuth } from '../lib/auth'
import { fmtDateTime, initials, roleLabel, userStatusLabel } from '../lib/format'
import type { EstadoUsuario as UserStatus, Permiso as Permission, Rol as Role, Usuario as User } from '../lib/types'
import { useResource } from '../lib/useResource'

/** El backend nombra los campos en español; el formulario usa sus propios identificadores. */
const CAMPOS_FORMULARIO: Record<string, string> = { nombreCompleto: 'fullName', correo: 'email', contrasena: 'password' }
const mapearCampos = (campos: Record<string, string>) =>
  Object.fromEntries(Object.entries(campos).map(([k, v]) => [CAMPOS_FORMULARIO[k] ?? k, v]))

const statusTone: Record<UserStatus, BadgeTone> = { ACTIVO: 'green', INACTIVO: 'neutral', PENDIENTE: 'blue' }

const PERMISSIONS: { key: Permission; label: string }[] = [
  { key: 'VER_DATOS', label: 'Consulta de datos ambientales' },
  { key: 'EXPORTAR_REPORTES', label: 'Exportación de reportes' },
  { key: 'PREDICCIONES_ML', label: 'Predicciones IA y modelos' },
  { key: 'INGESTAR_DATOS', label: 'Ingesta de datos' },
  { key: 'GESTIONAR_ANOMALIAS', label: 'Gestión de anomalías' },
  { key: 'GESTIONAR_ESTACIONES', label: 'Gestión de estaciones' },
  { key: 'GESTIONAR_USUARIOS', label: 'Gestión de usuarios' },
  { key: 'ADMINISTRAR_SISTEMA', label: 'Administración del sistema' },
]

/** Refleja el mapeo Rol → permisos del backend (enum Rol en Java) para previsualizar un cambio de rol. */
const ROLE_PERMISSIONS: Record<Role, Permission[]> = {
  ADMINISTRADOR: PERMISSIONS.map((p) => p.key),
  ANALISTA: ['VER_DATOS', 'EXPORTAR_REPORTES', 'PREDICCIONES_ML', 'INGESTAR_DATOS', 'GESTIONAR_ANOMALIAS'],
  CIUDADANO: ['VER_DATOS'],
}

export default function UsersPage() {
  const { user: me } = useAuth()
  const [query, setQuery] = useState('')
  const [debounced, setDebounced] = useState('')
  const [editing, setEditing] = useState<User | 'new' | null>(null)
  const [busy, setBusy] = useState<number | null>(null)

  useEffect(() => {
    const t = setTimeout(() => setDebounced(query), 200)
    return () => clearTimeout(t)
  }, [query])

  const res = useResource<User[]>(`users-${debounced}`, () => api.get(`/usuarios${debounced ? `?q=${encodeURIComponent(debounced)}` : ''}`))

  const setStatus = async (u: User, action: 'activar' | 'desactivar') => {
    setBusy(u.id)
    try {
      const updated = await api.post<User>(`/usuarios/${u.id}/${action}`)
      res.setData((list) => list && list.map((x) => (x.id === updated.id ? updated : x)))
      toast.success(action === 'desactivar' ? `${u.nombreCompleto} quedó inactivo` : u.estado === 'PENDIENTE' ? `Cuenta de ${u.nombreCompleto} aprobada` : `${u.nombreCompleto} reactivado`,
        { description: action === 'desactivar' ? 'No podrá iniciar sesión mientras permanezca inactivo.' : 'Ya puede acceder a la plataforma.' })
    } catch (e) {
      toast.error((e as ApiError).message)
    } finally {
      setBusy(null)
    }
  }

  return (
    <>
      <PageHeader
        title="Usuarios y roles"
        subtitle="Controle quién accede a EcoAnalytics y qué puede hacer cada perfil."
        actions={<Button variant="primary" icon={<Plus size={16} />} onClick={() => setEditing('new')}>Nuevo usuario</Button>}
      />

      <Panel flush>
        <div style={{ padding: '14px 18px 4px' }}>
          <div className="input-search" style={{ maxWidth: 380 }}>
            <Search size={15} aria-hidden />
            <input className="input" placeholder="Buscar por nombre o correo" value={query} onChange={(e) => setQuery(e.target.value)} aria-label="Buscar usuarios" />
          </div>
        </div>
        {res.error && !res.data ? <ErrorState message={res.error.message} onRetry={res.reload} /> : (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Usuario</th><th>Rol</th><th>Estado</th><th>Último acceso</th><th /></tr></thead>
              <tbody>
                {!res.data ? <tr><td colSpan={5}><Skeleton height={160} /></td></tr> : res.data.length === 0 ? (
                  <tr><td colSpan={5} className="muted" style={{ textAlign: 'center', padding: 28 }}>Ningún usuario coincide con “{query}”.</td></tr>
                ) : res.data.map((u) => (
                  <tr key={u.id}>
                    <td>
                      <div className="row" style={{ gap: 10, flexWrap: 'nowrap' }}>
                        <div className="avatar" aria-hidden>{initials(u.nombreCompleto)}</div>
                        <div>
                          <div className="strong">{u.nombreCompleto}{u.id === me?.id && <span className="muted small"> (usted)</span>}</div>
                          <div className="small muted">{u.correo}</div>
                        </div>
                      </div>
                    </td>
                    <td><Badge tone={u.rol === 'ADMINISTRADOR' ? 'purple' : u.rol === 'ANALISTA' ? 'blue' : 'neutral'}>{roleLabel[u.rol]}</Badge></td>
                    <td><Badge tone={statusTone[u.estado]} dot>{userStatusLabel[u.estado]}</Badge></td>
                    <td className="muted">{u.ultimoIngreso ? fmtDateTime(u.ultimoIngreso) : 'Nunca'}</td>
                    <td>
                      <div className="row" style={{ justifyContent: 'flex-end', flexWrap: 'nowrap' }}>
                        <Button size="sm" variant="plain" icon={<Pencil size={14} />} onClick={() => setEditing(u)}>Editar</Button>
                        {u.id !== me?.id && (u.estado === 'ACTIVO' ? (
                          <Button size="sm" variant="danger" loading={busy === u.id} onClick={() => setStatus(u, 'desactivar')}>Desactivar</Button>
                        ) : (
                          <Button size="sm" variant="success" loading={busy === u.id} onClick={() => setStatus(u, 'activar')}>
                            {u.estado === 'PENDIENTE' ? 'Aprobar' : 'Reactivar'}
                          </Button>
                        ))}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Panel>

      <UserSheet user={editing} onClose={() => setEditing(null)} onSaved={(saved, created) => {
        setEditing(null)
        if (created) void res.reload()
        else res.setData((list) => list && list.map((x) => (x.id === saved.id ? saved : x)))
      }} />
    </>
  )
}

function UserSheet({ user, onClose, onSaved }: { user: User | 'new' | null; onClose: () => void; onSaved: (u: User, created: boolean) => void }) {
  const editing = user !== null && user !== 'new' ? user : null
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [role, setRole] = useState<Role>('CIUDADANO')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (user === null) return
    setErrors({})
    setPassword('')
    setFullName(editing?.nombreCompleto ?? '')
    setEmail(editing?.correo ?? '')
    setRole(editing?.rol ?? 'CIUDADANO')
  }, [user, editing])

  const save = async () => {
    const e: Record<string, string> = {}
    if (!fullName.trim()) e.fullName = 'El campo Nombre y Apellidos es obligatorio.'
    if (!email.trim()) e.email = 'El campo Correo Electrónico es obligatorio.'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) e.email = 'Ingrese un formato de correo electrónico válido.'
    if (password && password.length < 8) e.password = 'La contraseña debe contener al menos 8 caracteres.'
    setErrors(e)
    if (Object.keys(e).length) return
    setSaving(true)
    try {
      const body = { nombreCompleto: fullName, correo: email, rol: role, contrasena: password || null }
      const saved = editing ? await api.put<User>(`/usuarios/${editing.id}`, body) : await api.post<User>('/usuarios', body)
      toast.success(`Cambios guardados con éxito para ${saved.nombreCompleto} con rol [${roleLabel[saved.rol].toUpperCase()}]`,
        { description: !editing && !password ? 'Contraseña inicial: EcoAnalytics2026!' : undefined })
      onSaved(saved, !editing)
    } catch (err) {
      setErrors(mapearCampos((err as ApiError).fields))
      toast.error((err as ApiError).message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Sheet open={user !== null} onClose={onClose} title={editing ? `Editar ${editing.nombreCompleto}` : 'Nuevo usuario'} footer={
      <><Button onClick={onClose}>Cancelar</Button><Button variant="primary" loading={saving} onClick={save}>Guardar cambios</Button></>
    }>
      <Field label="Nombre y apellidos" htmlFor="u-name" error={errors.fullName}>
        <input id="u-name" className="input" value={fullName} onChange={(e) => setFullName(e.target.value)} aria-invalid={!!errors.fullName} />
      </Field>
      <Field label="Correo electrónico" htmlFor="u-email" error={errors.email}>
        <input id="u-email" className="input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} aria-invalid={!!errors.email} />
      </Field>
      <Field label="Rol" htmlFor="u-role">
        <select id="u-role" className="select" value={role} onChange={(e) => setRole(e.target.value as Role)}>
          {(Object.keys(roleLabel) as Role[]).map((r) => <option key={r} value={r}>{roleLabel[r]}</option>)}
        </select>
      </Field>
      {!editing && (
        <Field label="Contraseña inicial (opcional)" htmlFor="u-pass" error={errors.password} hint="Si la deja vacía se asigna EcoAnalytics2026!">
          <input id="u-pass" className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" />
        </Field>
      )}
      <div className="field">
        <span className="field-label">Permisos específicos del rol</span>
        <ul className="permission-list">
          {PERMISSIONS.map((p) => {
            const granted = ROLE_PERMISSIONS[role].includes(p.key)
            return (
              <li key={p.key} data-granted={granted}>
                {granted ? <Check size={14} /> : <Minus size={14} />}
                {p.label}
                <span className="muted small" style={{ marginLeft: 'auto' }}>{granted ? 'Habilitado' : 'Deshabilitado'}</span>
              </li>
            )
          })}
        </ul>
      </div>
    </Sheet>
  )
}
