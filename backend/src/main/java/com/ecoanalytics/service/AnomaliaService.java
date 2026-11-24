package com.ecoanalytics.service;

import com.ecoanalytics.exception.ExcepcionNegocio;
import com.ecoanalytics.model.*;
import com.ecoanalytics.model.evento.MedicionesRegistradasEvento;
import com.ecoanalytics.repository.AnomaliaRepository;
import com.ecoanalytics.repository.MedicionRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Marca lecturas cuyo puntaje Z frente a los 7 días previos de la estación supera el umbral (HU-11). */
@Service
public class AnomaliaService {

    private static final int HORAS_LINEA_BASE = 168;
    private static final int MIN_PUNTOS_LINEA_BASE = 48;
    private static final double DESVIACION_MINIMA = 1.0;
    private static final double UMBRAL_MINIMO = 1.5;
    private static final double UMBRAL_MAXIMO = 6.0;

    private final AnomaliaRepository anomalias;
    private final MedicionRepository mediciones;
    private final EstacionService estacionService;
    private final ConfiguracionService configuracion;
    private final AlertaService alertaService;
    private final AuditoriaService auditoria;

    public AnomaliaService(AnomaliaRepository anomalias, MedicionRepository mediciones, EstacionService estacionService,
                           ConfiguracionService configuracion, AlertaService alertaService, AuditoriaService auditoria) {
        this.anomalias = anomalias;
        this.mediciones = mediciones;
        this.estacionService = estacionService;
        this.configuracion = configuracion;
        this.alertaService = alertaService;
        this.auditoria = auditoria;
    }

    public record EstadisticasAnomalias(long pendientes, long notificadas, long descartadas, double umbral) {
    }

    public record PuntoLineaTiempo(LocalDateTime fecha, double pm25, Double puntajeZ) {
    }

    @EventListener
    public void alRegistrarMediciones(MedicionesRegistradasEvento evento) {
        Map<Long, List<Medicion>> porEstacion = mediciones.findAllById(evento.idsMediciones()).stream()
                .filter(m -> m.getPm25() != null)
                .collect(Collectors.groupingBy(Medicion::getEstacionId));
        porEstacion.forEach((estacionId, lista) -> {
            LocalDateTime desde = lista.stream().map(Medicion::getFechaMedicion).min(Comparator.naturalOrder()).orElseThrow();
            LocalDateTime hasta = lista.stream().map(Medicion::getFechaMedicion).max(Comparator.naturalOrder()).orElseThrow();
            analizar(estacionId, desde, hasta);
        });
    }

    /** Recorre [desde, hasta] de una estación y guarda las anomalías nuevas. Devuelve cuántas encontró. */
    public int analizar(Long estacionId, LocalDateTime desde, LocalDateTime hasta) {
        Estacion estacion = estacionService.buscar(estacionId);
        double umbral = configuracion.umbralAnomalias();
        List<Medicion> serie = mediciones.buscarUtilesPorEstacionYRango(estacionId, desde.minusHours(HORAS_LINEA_BASE), hasta)
                .stream().filter(m -> m.getPm25() != null).toList();

        // Ventana deslizante de las 168 h previas, mantenida con sumas acumuladas.
        Deque<Medicion> ventana = new ArrayDeque<>();
        double suma = 0, sumaCuadrados = 0;
        List<Anomalia> nuevas = new ArrayList<>();
        for (Medicion m : serie) {
            LocalDateTime inicioVentana = m.getFechaMedicion().minusHours(HORAS_LINEA_BASE);
            while (!ventana.isEmpty() && ventana.peekFirst().getFechaMedicion().isBefore(inicioVentana)) {
                double antigua = ventana.pollFirst().getPm25();
                suma -= antigua;
                sumaCuadrados -= antigua * antigua;
            }
            if (!m.getFechaMedicion().isBefore(desde) && ventana.size() >= MIN_PUNTOS_LINEA_BASE) {
                double media = suma / ventana.size();
                double desviacion = Math.max(DESVIACION_MINIMA, Math.sqrt(Math.max(0, sumaCuadrados / ventana.size() - media * media)));
                double z = (m.getPm25() - media) / desviacion;
                if (z >= umbral && !anomalias.existsByEstacionIdAndFechaMedicionAndVariable(
                        estacionId, m.getFechaMedicion(), VariableAmbiental.PM25)) {
                    nuevas.add(new Anomalia(estacion, m.getFechaMedicion(), VariableAmbiental.PM25, m.getPm25(),
                            Estadistica.redondear(media), Estadistica.redondear(desviacion, 2), Estadistica.redondear(z, 2)));
                }
            }
            ventana.addLast(m);
            suma += m.getPm25();
            sumaCuadrados += m.getPm25() * m.getPm25();
        }
        anomalias.saveAll(nuevas);
        if (!nuevas.isEmpty()) {
            auditoria.advertencia("Anomalías", nuevas.size() + " anomalía(s) detectada(s) en " + estacion.getNombre());
        }
        return nuevas.size();
    }

    public List<Anomalia> recientes() {
        return anomalias.findTop100ByOrderByFechaMedicionDesc();
    }

    public EstadisticasAnomalias estadisticas() {
        return new EstadisticasAnomalias(anomalias.countByEstado(Anomalia.Estado.PENDIENTE),
                anomalias.countByEstado(Anomalia.Estado.NOTIFICADA), anomalias.countByEstado(Anomalia.Estado.DESCARTADA),
                configuracion.umbralAnomalias());
    }

    public List<PuntoLineaTiempo> lineaTiempo(Long estacionId, int horas) {
        int horasSeguras = Math.min(Math.max(horas, 24), 336);
        LocalDateTime hasta = LocalDateTime.now();
        LocalDateTime desde = hasta.minusHours(horasSeguras);
        Map<LocalDateTime, Double> marcadas = anomalias
                .findByEstacionIdAndFechaMedicionAfterOrderByFechaMedicionAsc(estacionId, desde).stream()
                .collect(Collectors.toMap(Anomalia::getFechaMedicion, Anomalia::getPuntajeZ, (a, b) -> a));
        return mediciones.buscarUtilesPorEstacionYRango(estacionId, desde, hasta).stream()
                .filter(m -> m.getPm25() != null)
                .map(m -> new PuntoLineaTiempo(m.getFechaMedicion(), m.getPm25(), marcadas.get(m.getFechaMedicion())))
                .toList();
    }

    public Anomalia notificar(Long id) {
        Anomalia anomalia = buscar(id);
        anomalia.notificar();
        alertaService.emitirDesdeAnomalia(anomalia);
        anomalias.save(anomalia);
        auditoria.advertencia("Anomalías", "Anomalía #" + id + " notificada como alerta (" + anomalia.getEstacionNombre() + ")");
        return anomalia;
    }

    public Anomalia descartar(Long id) {
        Anomalia anomalia = buscar(id);
        anomalia.descartar();
        anomalias.save(anomalia);
        auditoria.info("Anomalías", "Anomalía #" + id + " descartada como falso positivo");
        return anomalia;
    }

    public void cambiarUmbral(double umbral) {
        if (umbral < UMBRAL_MINIMO || umbral > UMBRAL_MAXIMO) {
            throw ExcepcionNegocio.invalido("El umbral de desviación debe estar entre 1.5σ y 6σ.");
        }
        configuracion.cambiar(ConfiguracionSistema.UMBRAL_Z_ANOMALIAS, String.valueOf(umbral));
        auditoria.info("Anomalías", "Umbral de detección ajustado a " + umbral + "σ");
    }

    private Anomalia buscar(Long id) {
        return anomalias.findById(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("Anomalía no encontrada."));
    }
}
