package com.ecoanalytics.dto;

import com.ecoanalytics.model.*;
import com.ecoanalytics.model.prediccion.AlgoritmoModelo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Cuerpos de respuesta. Las entidades del modelo nunca salen de la aplicación a través de la API. */
public final class Respuestas {

    private Respuestas() {
    }

    public record UsuarioRespuesta(Long id, String nombreCompleto, String correo, Rol rol, EstadoUsuario estado,
                                   Set<Rol.Permiso> permisos, LocalDateTime fechaCreacion, LocalDateTime ultimoIngreso) {
        public static UsuarioRespuesta de(Usuario u) {
            return new UsuarioRespuesta(u.getId(), u.getNombreCompleto(), u.getCorreo(), u.getRol(), u.getEstado(),
                    u.getRol().permisos(), u.getFechaCreacion(), u.getUltimoIngreso());
        }
    }

    public record SesionRespuesta(String token, UsuarioRespuesta usuario) {
    }

    public record EstacionRespuesta(Long id, String nombre, String zona, double latitud, double longitud,
                                    TipoFuente tipoFuente, String variables, EstadoEstacion estado,
                                    LocalDateTime fechaInstalacion, LocalDateTime ultimaMedicion) {
        public static EstacionRespuesta de(Estacion e, LocalDateTime ultimaMedicion) {
            return new EstacionRespuesta(e.getId(), e.getNombre(), e.getZona(), e.getLatitud(), e.getLongitud(),
                    e.getTipoFuente(), e.getVariables(), e.getEstado(), e.getFechaInstalacion(), ultimaMedicion);
        }
    }

    public record LoteRespuesta(Long id, String nombreArchivo, TipoFuente formato, int totalRegistros,
                                int registrosValidos, int registrosAdvertencia, int registrosRechazados,
                                int registrosImportados, LoteIngesta.Estado estado, String cargadoPor,
                                LocalDateTime fechaCreacion, LocalDateTime fechaImportacion) {
        public static LoteRespuesta de(LoteIngesta l) {
            return new LoteRespuesta(l.getId(), l.getNombreArchivo(), l.getFormato(), l.getTotalRegistros(),
                    l.getRegistrosValidos(), l.getRegistrosAdvertencia(), l.getRegistrosRechazados(),
                    l.getRegistrosImportados(), l.getEstado(), l.getCargadoPor(), l.getFechaCreacion(),
                    l.getFechaImportacion());
        }
    }

    public record IncidenciaRespuesta(Long id, int fila, String estacionNombre, String variable, String valorDetectado,
                                      String rangoAceptable, IncidenciaValidacion.Tipo tipo,
                                      IncidenciaValidacion.Resolucion resolucion) {
        public static IncidenciaRespuesta de(IncidenciaValidacion i) {
            return new IncidenciaRespuesta(i.getId(), i.getFila(), i.getEstacionNombre(), i.getVariable(),
                    i.getValorDetectado(), i.getRangoAceptable(), i.getTipo(), i.getResolucion());
        }
    }

    public record AnomaliaRespuesta(Long id, Long estacionId, String estacionNombre, LocalDateTime fechaMedicion,
                                    VariableAmbiental variable, double valorObservado, double mediaEsperada,
                                    double desviacionEstandar, double puntajeZ, Anomalia.Severidad severidad,
                                    Anomalia.Estado estado) {
        public static AnomaliaRespuesta de(Anomalia a) {
            return new AnomaliaRespuesta(a.getId(), a.getEstacionId(), a.getEstacionNombre(), a.getFechaMedicion(),
                    a.getVariable(), a.getValorObservado(), a.getMediaEsperada(), a.getDesviacionEstandar(),
                    a.getPuntajeZ(), a.getSeveridad(), a.getEstado());
        }
    }

    public record ModeloRespuesta(Long id, String version, AlgoritmoModelo algoritmo, String nombreAlgoritmo,
                                  String variableObjetivo, String caracteristicas, double r2, double rmse, double mae,
                                  int muestrasEntrenamiento, int muestrasPrueba, long duracionMs,
                                  VersionModelo.Estado estado, String entrenadoPor, LocalDateTime fechaEntrenamiento) {
        public static ModeloRespuesta de(VersionModelo m) {
            return new ModeloRespuesta(m.getId(), m.getVersion(), m.getAlgoritmo(), m.getAlgoritmo().nombre(),
                    m.getVariableObjetivo(), m.getCaracteristicas(), m.getR2(), m.getRmse(), m.getMae(),
                    m.getMuestrasEntrenamiento(), m.getMuestrasPrueba(), m.getDuracionMs(), m.getEstado(),
                    m.getEntrenadoPor(), m.getFechaEntrenamiento());
        }
    }

    public record AlertaRespuesta(Long id, Alerta.Tipo tipo, Alerta.Nivel nivel, String titulo, String mensaje,
                                  String recomendacion, Long estacionId, String estacionNombre, Double valorDisparador,
                                  List<CanalAlerta.Codigo> canales, String origen, Alerta.Estado estado,
                                  LocalDateTime fechaCreacion) {
        public static AlertaRespuesta de(Alerta a) {
            return new AlertaRespuesta(a.getId(), a.getTipo(), a.getNivel(), a.getTitulo(), a.getMensaje(),
                    a.getRecomendacion(), a.getEstacionId(), a.getEstacionNombre(), a.getValorDisparador(),
                    a.getCanalesEntregados(), a.getOrigen(), a.getEstado(), a.getFechaCreacion());
        }
    }

    public record CanalRespuesta(CanalAlerta.Codigo codigo, boolean habilitado, int entregas, LocalDateTime ultimaEntrega) {
        public static CanalRespuesta de(CanalAlerta c) {
            return new CanalRespuesta(c.getCodigo(), c.isHabilitado(), c.getEntregas(), c.getUltimaEntrega());
        }
    }

    public record ConfiguracionIndicadorRespuesta(TipoIndicador tipo, boolean habilitado, String programacion,
                                                  LocalDateTime ultimaEjecucion, int registrosProcesados) {
        public static ConfiguracionIndicadorRespuesta de(ConfiguracionIndicador c) {
            return new ConfiguracionIndicadorRespuesta(c.getTipo(), c.isHabilitado(), c.getProgramacion(),
                    c.getUltimaEjecucion(), c.getRegistrosProcesados());
        }
    }

    public record AuditoriaRespuesta(Long id, LocalDateTime fecha, EventoAuditoria.Nivel nivel, String modulo,
                                     String descripcion, String actor) {
        public static AuditoriaRespuesta de(EventoAuditoria e) {
            return new AuditoriaRespuesta(e.getId(), e.getFecha(), e.getNivel(), e.getModulo(), e.getDescripcion(),
                    e.getActor());
        }
    }
}
