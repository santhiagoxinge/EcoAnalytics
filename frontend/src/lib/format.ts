import type {
  CategoriaIca, CodigoCanal, EstadoAnomalia, EstadoEstacion, EstadoMedicion, EstadoUsuario, NivelAlerta, Rol,
  SeveridadAnomalia, TipoAlerta, TipoFuente, TipoIncidencia, TipoIndicador, Variable,
} from './types'

const LOCALE = 'es-CO'

const numberFormats = new Map<number, Intl.NumberFormat>()
export function num(value: number | null | undefined, decimals = 1): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '—'
  let f = numberFormats.get(decimals)
  if (!f) {
    f = new Intl.NumberFormat(LOCALE, { minimumFractionDigits: decimals, maximumFractionDigits: decimals })
    numberFormats.set(decimals, f)
  }
  return f.format(value)
}

export function int(value: number | null | undefined): string {
  return value === null || value === undefined ? '—' : new Intl.NumberFormat(LOCALE).format(value)
}

export function signedPercent(value: number | null | undefined): string {
  if (value === null || value === undefined) return '—'
  return `${value > 0 ? '+' : value < 0 ? '−' : ''}${num(Math.abs(value))} %`
}

const dateTime = new Intl.DateTimeFormat(LOCALE, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit', hour12: false })
const dateOnly = new Intl.DateTimeFormat(LOCALE, { day: 'numeric', month: 'short', year: 'numeric' })
const shortDate = new Intl.DateTimeFormat(LOCALE, { day: 'numeric', month: 'short' })
const hourOnly = new Intl.DateTimeFormat(LOCALE, { hour: '2-digit', minute: '2-digit', hour12: false })
const weekdayHour = new Intl.DateTimeFormat(LOCALE, { weekday: 'short', hour: '2-digit', minute: '2-digit', hour12: false })

const parse = (iso: string) => new Date(iso)
export const fmtDateTime = (iso: string | null | undefined) => (iso ? dateTime.format(parse(iso)) : '—')
export const fmtDate = (iso: string | null | undefined) => (iso ? dateOnly.format(parse(iso.length === 10 ? `${iso}T00:00:00` : iso)) : '—')
export const fmtShortDate = (iso: string) => shortDate.format(parse(iso.length === 10 ? `${iso}T00:00:00` : iso))
export const fmtHour = (iso: string) => hourOnly.format(parse(iso))
export const fmtWeekdayHour = (iso: string) => weekdayHour.format(parse(iso))

export function relativeMinutes(minutes: number | null | undefined): string {
  if (minutes === null || minutes === undefined) return 'sin datos'
  if (minutes < 1) return 'hace instantes'
  if (minutes < 60) return `hace ${minutes} ${minutes === 1 ? 'minuto' : 'minutos'}`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `hace ${hours} ${hours === 1 ? 'hora' : 'horas'}`
  const days = Math.floor(hours / 24)
  return `hace ${days} ${days === 1 ? 'día' : 'días'}`
}

export function minutesSince(iso: string | null | undefined): number | null {
  return iso ? Math.max(0, Math.round((Date.now() - parse(iso).getTime()) / 60000)) : null
}

export function isoDate(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export function daysAgo(days: number): string {
  const d = new Date()
  d.setDate(d.getDate() - days)
  return isoDate(d)
}

export function initials(name: string): string {
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map((p) => p[0]?.toUpperCase()).join('')
}

/* ───────── Etiquetas en español para los enums de la API ───────── */

export const roleLabel: Record<Rol, string> = {
  ADMINISTRADOR: 'Administrador',
  ANALISTA: 'Analista Ambiental',
  CIUDADANO: 'Ciudadano',
}

export const userStatusLabel: Record<EstadoUsuario, string> = {
  ACTIVO: 'Activo',
  INACTIVO: 'Inactivo',
  PENDIENTE: 'Pendiente de verificación',
}

export const stationStatusLabel: Record<EstadoEstacion, string> = {
  EN_LINEA: 'En línea',
  MANTENIMIENTO: 'Mantenimiento',
  FUERA_DE_LINEA: 'Fuera de línea',
}

export const sourceLabel: Record<TipoFuente, string> = {
  CSV: 'Archivo CSV',
  JSON: 'Archivo JSON',
  API: 'API externa',
  SENSOR: 'Sensor IoT',
}

export const variableLabel: Record<Variable, string> = {
  PM25: 'PM2.5',
  PM10: 'PM10',
  TEMPERATURA: 'Temperatura',
  HUMEDAD: 'Humedad',
}

export const variableUnit: Record<Variable, string> = {
  PM25: 'µg/m³',
  PM10: 'µg/m³',
  TEMPERATURA: '°C',
  HUMEDAD: '%',
}

export const measurementStatusLabel: Record<EstadoMedicion, string> = {
  VALIDA: 'Válido',
  ADVERTENCIA: 'Advertencia',
  DESCARTADA: 'Descartado',
}

export const indicatorLabel: Record<TipoIndicador, { name: string; unit: string; description: string }> = {
  PROMEDIO_MOVIL_PM25_24H: { name: 'Promedio móvil 24 h', unit: 'µg/m³', description: 'Media de PM2.5 de las últimas 24 horas' },
  MAXIMO_DIARIO: { name: 'Máximo diario', unit: 'µg/m³', description: 'Valor máximo de PM2.5 en las últimas 24 horas' },
  VARIACION_SEMANAL: { name: 'Tasa de variación semanal', unit: '%', description: 'Cambio de la media semanal frente a la semana anterior' },
  DIAS_EXCEDENCIA_OMS: { name: 'Días de excedencia normativa', unit: 'días', description: 'Días del mes con media de PM2.5 sobre la guía OMS (15 µg/m³)' },
}

export const severityLabel: Record<SeveridadAnomalia, string> = {
  MODERADA: 'Moderada',
  ALTA: 'Alta',
  CRITICA: 'Crítica',
}

export const anomalyStatusLabel: Record<EstadoAnomalia, string> = {
  PENDIENTE: 'Pendiente',
  NOTIFICADA: 'Alerta notificada',
  DESCARTADA: 'Falso positivo descartado',
}

export const alertTypeLabel: Record<TipoAlerta, string> = {
  REACTIVA: 'Reactiva',
  PREDICTIVA: 'Predictiva',
  EXTRAORDINARIA: 'Extraordinaria',
}

export const alertLevelLabel: Record<NivelAlerta, string> = {
  AMARILLA: 'Amarilla',
  NARANJA: 'Naranja',
  ROJA: 'Roja',
  PURPURA: 'Púrpura',
}

export const channelLabel: Record<CodigoCanal, { name: string; description: string }> = {
  PUSH: { name: 'Notificaciones push', description: 'Web Push a ciudadanos registrados en el área de cobertura' },
  CORREO: { name: 'Boletín por correo institucional', description: 'Envío masivo a entidades y suscriptores' },
  BANNER_WEB: { name: 'Banner en portal público', description: 'Aviso destacado en la portada ciudadana' },
}

export const issueTypeLabel: Record<TipoIncidencia, string> = {
  FUERA_DE_RANGO: 'Valor fuera de rango',
  DUPLICADO: 'Dato duplicado',
  CAMPO_FALTANTE: 'Campo vacío',
  ESTACION_DESCONOCIDA: 'Estación no registrada',
  FECHA_INVALIDA: 'Fecha u hora inválida',
}

export const aqiCategoryLabel: Record<CategoriaIca, string> = {
  BUENA: 'Buena',
  ACEPTABLE: 'Aceptable',
  DANINA_SENSIBLES: 'Dañina para grupos sensibles',
  DANINA: 'Dañina para la salud',
  MUY_DANINA: 'Muy dañina',
  PELIGROSA: 'Peligrosa',
}

const aqiToken: Record<CategoriaIca, string> = {
  BUENA: 'good',
  ACEPTABLE: 'acceptable',
  DANINA_SENSIBLES: 'sensitive',
  DANINA: 'harmful',
  MUY_DANINA: 'very',
  PELIGROSA: 'hazard',
}

/** Color de relleno para marcas (indicador, barras). */
export const aqiFill = (c: CategoriaIca | null | undefined) => (c ? `var(--aqi-${aqiToken[c]})` : 'var(--gray-fill)')
/** Color de texto sobre superficies claras u oscuras. */
export const aqiInk = (c: CategoriaIca | null | undefined) => (c ? `var(--aqi-${aqiToken[c]}-ink)` : 'var(--text-2)')

export function aqiCategoryOf(value: number): CategoriaIca {
  if (value <= 50) return 'BUENA'
  if (value <= 100) return 'ACEPTABLE'
  if (value <= 150) return 'DANINA_SENSIBLES'
  if (value <= 200) return 'DANINA'
  if (value <= 300) return 'MUY_DANINA'
  return 'PELIGROSA'
}

/** Concentración de PM2.5 → categoría, con los mismos cortes del backend (Res. 2254/2017). */
export function pm25Category(value: number): CategoriaIca {
  if (value <= 12) return 'BUENA'
  if (value <= 37) return 'ACEPTABLE'
  if (value <= 55) return 'DANINA_SENSIBLES'
  if (value <= 150) return 'DANINA'
  if (value <= 250) return 'MUY_DANINA'
  return 'PELIGROSA'
}

export const alertLevelColor: Record<NivelAlerta, { fill: string; ink: string }> = {
  AMARILLA: { fill: 'var(--aqi-acceptable)', ink: 'var(--aqi-acceptable-ink)' },
  NARANJA: { fill: 'var(--aqi-sensitive)', ink: 'var(--aqi-sensitive-ink)' },
  ROJA: { fill: 'var(--aqi-harmful)', ink: 'var(--aqi-harmful-ink)' },
  PURPURA: { fill: 'var(--aqi-very)', ink: 'var(--aqi-very-ink)' },
}
