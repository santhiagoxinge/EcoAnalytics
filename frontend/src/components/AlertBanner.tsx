import { Siren } from 'lucide-react'
import { useEffect, useState } from 'react'
import { api } from '../lib/api'
import { alertLevelColor, fmtHour } from '../lib/format'
import type { Alerta } from '../lib/types'

/** Banner del portal público con las alertas activas entregadas por el canal web (HU-14). */
export function AlertBanner() {
  const [alertas, setAlertas] = useState<Alerta[]>([])

  useEffect(() => {
    api.get<Alerta[]>('/alertas/banner').then(setAlertas).catch(() => setAlertas([]))
  }, [])

  if (!alertas.length) return null
  const alerta = alertas[0]
  const color = alertLevelColor[alerta.nivel]

  return (
    <div className="alert-banner" role="alert" style={{ ['--banner' as string]: color.fill, ['--banner-ink' as string]: color.ink }}>
      <div className="alert-banner-icon"><Siren size={18} /></div>
      <div style={{ minWidth: 0 }}>
        <p className="strong">{alerta.titulo} <span className="muted small" style={{ fontWeight: 500 }}>· {fmtHour(alerta.fechaCreacion)}</span></p>
        <p className="small">{alerta.mensaje}</p>
        {alerta.recomendacion && <p className="small muted" style={{ marginTop: 2 }}>{alerta.recomendacion}</p>}
      </div>
      {alertas.length > 1 && <span className="badge" style={{ marginLeft: 'auto' }}>+{alertas.length - 1}</span>}
    </div>
  )
}
