/* Contrato de la API de EcoAnalytics. Los nombres coinciden con los DTO del backend (Java en español). */

export type Rol = 'ADMINISTRADOR' | 'ANALISTA' | 'CIUDADANO'
export type EstadoUsuario = 'ACTIVO' | 'INACTIVO' | 'PENDIENTE'
export type Permiso =
  | 'VER_DATOS' | 'EXPORTAR_REPORTES' | 'PREDICCIONES_ML' | 'INGESTAR_DATOS'
  | 'GESTIONAR_ANOMALIAS' | 'GESTIONAR_ESTACIONES' | 'GESTIONAR_USUARIOS' | 'ADMINISTRAR_SISTEMA'

export interface Usuario {
  id: number
  nombreCompleto: string
  correo: string
  rol: Rol
  estado: EstadoUsuario
  permisos: Permiso[]
  fechaCreacion: string
  ultimoIngreso: string | null
}

export type EstadoEstacion = 'EN_LINEA' | 'MANTENIMIENTO' | 'FUERA_DE_LINEA'
export type TipoFuente = 'CSV' | 'JSON' | 'API' | 'SENSOR'
export type CategoriaIca = 'BUENA' | 'ACEPTABLE' | 'DANINA_SENSIBLES' | 'DANINA' | 'MUY_DANINA' | 'PELIGROSA'
export type Variable = 'PM25' | 'PM10' | 'TEMPERATURA' | 'HUMEDAD'
export type EstadoMedicion = 'VALIDA' | 'ADVERTENCIA' | 'DESCARTADA'

export interface Estacion {
  id: number
  nombre: string
  zona: string
  latitud: number
  longitud: number
  tipoFuente: TipoFuente
  variables: string
  estado: EstadoEstacion
  fechaInstalacion: string
  ultimaMedicion: string | null
}

export interface ResumenEstacion {
  id: number
  nombre: string
  zona: string
  estado: EstadoEstacion
  latitud: number
  longitud: number
  disponible: boolean
  ica: number | null
  categoria: CategoriaIca | null
  pm25: number | null
  fechaMedicion: string | null
}

export interface CondicionesActuales {
  estacionId: number
  estacionNombre: string
  zona: string
  estado: EstadoEstacion
  disponible: boolean
  motivoNoDisponible: string | null
  fechaMedicion: string | null
  minutosTranscurridos: number | null
  pm25: number | null
  pm10: number | null
  temperatura: number | null
  humedad: number | null
  ica: number | null
  categoria: CategoriaIca | null
  recomendacion: string | null
  tendencia24h: { fecha: string; pm25: number }[]
}

export interface FilaHistorico {
  id: number
  fecha: string
  pm25: number | null
  pm10: number | null
  temperatura: number | null
  humedad: number | null
  ica: number | null
  estado: EstadoMedicion
  fuente: TipoFuente
}

export interface PaginaHistorico {
  estacionNombre: string
  desde: string
  hasta: string
  resumen: { total: number; mediaPm25: number | null; maxPm25: number | null; minPm25: number | null; advertencias: number; descartadas: number }
  granularidad: 'DIARIA' | 'HORARIA'
  grafico: { fecha: string; pm25: number | null; pm10: number | null }[]
  filas: FilaHistorico[]
  pagina: number
  tamanoPagina: number
  totalPaginas: number
}

export interface EstadisticaPeriodo {
  desde: string
  hasta: string
  mediciones: number
  cobertura: number
  media: number | null
  maximo: number | null
  minimo: number | null
  diasExcedencia: number | null
  suficiente: boolean
}

export interface Comparativa {
  estacionNombre: string
  variable: Variable
  unidad: string
  periodoA: EstadisticaPeriodo
  periodoB: EstadisticaPeriodo
  variaciones: { cambioMedia: number | null; cambioMaximo: number | null; cambioMinimo: number | null; cambioDiasExcedencia: number | null } | null
  comparable: boolean
  advertencia: string | null
  serie: { dia: number; periodoA: number | null; periodoB: number | null }[]
}

export interface Visualizacion {
  estacionNombre: string
  dias: number
  serie: { fecha: string; pm25: number | null; pm10: number | null; temperatura: number | null; humedad: number | null }[]
  correlacion: { pearsonR: number; pendiente: number; intercepto: number; puntos: { temperatura: number; pm25: number }[] }
  mapaCalor: { celdas: (number | null)[][]; minimo: number | null; maximo: number | null }
}

export type TipoIndicador = 'PROMEDIO_MOVIL_PM25_24H' | 'DIAS_EXCEDENCIA_OMS' | 'VARIACION_SEMANAL' | 'MAXIMO_DIARIO'

export interface IndicadoresRespuesta {
  configuraciones: { tipo: TipoIndicador; habilitado: boolean; programacion: string; ultimaEjecucion: string | null; registrosProcesados: number }[]
  estaciones: { estacionId: number; estacionNombre: string; valores: Partial<Record<TipoIndicador, number>>; fechaCalculo: string | null }[]
}

export type SeveridadAnomalia = 'MODERADA' | 'ALTA' | 'CRITICA'
export type EstadoAnomalia = 'PENDIENTE' | 'NOTIFICADA' | 'DESCARTADA'

export interface Anomalia {
  id: number
  estacionId: number
  estacionNombre: string
  fechaMedicion: string
  variable: Variable
  valorObservado: number
  mediaEsperada: number
  desviacionEstandar: number
  puntajeZ: number
  severidad: SeveridadAnomalia
  estado: EstadoAnomalia
}

export interface EstadisticasAnomalias {
  pendientes: number
  notificadas: number
  descartadas: number
  umbral: number
}

export interface AnomaliasRespuesta {
  anomalias: Anomalia[]
  estadisticas: EstadisticasAnomalias
}

export type AlgoritmoModelo = 'MEDIA_MOVIL' | 'REGRESION_LINEAL' | 'K_VECINOS' | 'RANDOM_FOREST'

export interface VersionModelo {
  id: number
  version: string
  algoritmo: AlgoritmoModelo
  nombreAlgoritmo: string
  variableObjetivo: string
  caracteristicas: string
  r2: number
  rmse: number
  mae: number
  muestrasEntrenamiento: number
  muestrasPrueba: number
  duracionMs: number
  estado: 'PRODUCCION' | 'ARCHIVADO' | 'CANDIDATO'
  entrenadoPor: string
  fechaEntrenamiento: string
}

export interface EntrenamientoRespuesta {
  modelo: VersionModelo
  estacionNombre: string
  comparacion: { fecha: string; real: number; predicho: number }[]
}

export interface PuntoPronostico {
  fecha: string
  pm25: number
  inferior: number
  superior: number
  ica: { valor: number; categoria: CategoriaIca }
}

export interface VistaPronostico {
  pronostico: {
    estacionId: number
    estacionNombre: string
    versionModelo: string
    algoritmo: AlgoritmoModelo
    rmseModelo: number
    fechaGeneracion: string
    puntos: PuntoPronostico[]
  }
  horizonteHoras: number
  icaProyectado: number
  categoriaProyectada: CategoriaIca
  recomendacion: string
  pico: PuntoPronostico
  limiteOms: number
  horasSobreOms: number
}

export type TipoAlerta = 'REACTIVA' | 'PREDICTIVA' | 'EXTRAORDINARIA'
export type NivelAlerta = 'AMARILLA' | 'NARANJA' | 'ROJA' | 'PURPURA'
export type CodigoCanal = 'PUSH' | 'CORREO' | 'BANNER_WEB'

export interface Alerta {
  id: number
  tipo: TipoAlerta
  nivel: NivelAlerta
  titulo: string
  mensaje: string
  recomendacion: string | null
  estacionId: number | null
  estacionNombre: string | null
  valorDisparador: number | null
  canales: CodigoCanal[]
  origen: string
  estado: 'ACTIVA' | 'RESUELTA'
  fechaCreacion: string
}

export interface CentroAlertas {
  alertas: Alerta[]
  estadisticas: { activas: number; ultimas24h: number; reactivas: number; predictivas: number; extraordinarias: number }
  canales: { codigo: CodigoCanal; habilitado: boolean; entregas: number; ultimaEntrega: string | null }[]
}

export type TipoReporte = 'RESUMEN_EJECUTIVO' | 'DETALLE_HISTORICO' | 'PRONOSTICO'
export type FormatoReporte = 'PDF' | 'CSV' | 'EXCEL'

export interface DocumentoReporte {
  reporteId: string
  tipo: TipoReporte
  titulo: string
  estacionNombre: string
  zona: string
  desde: string
  hasta: string
  fechaEmision: string
  emitidoPor: string
  mediaPm25: number | null
  porcentajeCumplimiento: number | null
  totalMediciones: number
  variables: { variable: string; unidad: string; media: number | null; minimo: number | null; maximo: number | null; evaluacion: string }[]
  diario: { fecha: string; mediaPm25: number | null; maxPm25: number | null; mediaPm10: number | null; ica: number | null; categoria: string }[]
  pronostico: { fecha: string; pm25: number; inferior: number; superior: number; ica: number }[]
}

export type TipoIncidencia = 'FUERA_DE_RANGO' | 'DUPLICADO' | 'CAMPO_FALTANTE' | 'ESTACION_DESCONOCIDA' | 'FECHA_INVALIDA'

export interface Lote {
  id: number
  nombreArchivo: string
  formato: TipoFuente
  totalRegistros: number
  registrosValidos: number
  registrosAdvertencia: number
  registrosRechazados: number
  registrosImportados: number
  estado: 'PENDIENTE_REVISION' | 'IMPORTADO' | 'CANCELADO'
  cargadoPor: string
  fechaCreacion: string
  fechaImportacion: string | null
}

export interface CargaRespuesta {
  lote: Lote
  columnas: string[]
  vistaPrevia: { fila: number; estacionNombre: string | null; fecha: string | null; fechaOriginal: string | null; pm25: number | null; pm10: number | null; temperatura: number | null; humedad: number | null }[]
  incidencias: { id: number; fila: number; estacionNombre: string | null; variable: string | null; valorDetectado: string | null; rangoAceptable: string | null; tipo: TipoIncidencia; resolucion: 'DESCARTADO' | 'MARCADO' }[]
}

export interface ImportacionRespuesta {
  lote: Lote
  importados: number
  descartados: number
  interpolados: number
  yaExistentes: number
}

export type EstadoSincronizacion = 'SINCRONIZADA' | 'RETRASADA' | 'CAIDA' | 'MANTENIMIENTO'

export interface PanoramaMonitoreo {
  infraestructura: { segundosActivo: number; memoriaUsadaMb: number; memoriaMaximaMb: number; porcentajeCpu: number; procesadores: number; baseDatos: string }
  totales: { mediciones: number; ingestadas24h: number; estacionesActivas: number; totalEstaciones: number; anomaliasPendientes: number; alertas24h: number; usuarios: number; medicionesDescartadas: number }
  fuentes: { estacionId: number; nombre: string; tipo: TipoFuente; estado: EstadoEstacion; ultimaMedicion: string | null; minutosDesdeUltima: number | null; sincronizacion: EstadoSincronizacion }[]
  servicios: { nombre: string; descripcion: string; programacion: string; saludable: boolean; ultimaEjecucion: string | null; duracionMs: number; detalle: string }[]
}

export interface EventoAuditoria {
  id: number
  fecha: string
  nivel: 'INFO' | 'ADVERTENCIA' | 'ERROR'
  modulo: string
  descripcion: string
  actor: string
}
