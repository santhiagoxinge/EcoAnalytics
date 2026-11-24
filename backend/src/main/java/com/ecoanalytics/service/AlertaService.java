package com.ecoanalytics.service;

import com.ecoanalytics.exception.ExcepcionNegocio;
import com.ecoanalytics.model.*;
import com.ecoanalytics.model.alerta.EmisorAlerta;
import com.ecoanalytics.model.evento.MedicionesRegistradasEvento;
import com.ecoanalytics.model.evento.PronosticoGeneradoEvento;
import com.ecoanalytics.model.prediccion.Pronostico;
import com.ecoanalytics.repository.AlertaRepository;
import com.ecoanalytics.repository.CanalAlertaRepository;
import com.ecoanalytics.repository.EstacionRepository;
import com.ecoanalytics.repository.MedicionRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Observador de mediciones nuevas y pronósticos: evalúa las reglas de alerta y difunde por cada canal
 * habilitado (HU-14). Reactiva significa "ya ocurrió"; predictiva, "puede ocurrir".
 */
@Service
public class AlertaService {

    /** Una alerta del mismo tipo por estación dentro de esta ventana, para no saturar a la ciudadanía. */
    private static final int HORAS_SIN_REPETIR = 6;
    private static final int VENTANA_PREDICTIVA_HORAS = 6;
    private static final int MAX_ALERTAS_BANNER = 3;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final AlertaRepository alertas;
    private final CanalAlertaRepository canales;
    private final MedicionRepository mediciones;
    private final EstacionRepository estaciones;
    private final ConfiguracionService configuracion;
    private final AuditoriaService auditoria;
    private final Map<CanalAlerta.Codigo, EmisorAlerta> emisores;

    public AlertaService(AlertaRepository alertas, CanalAlertaRepository canales, MedicionRepository mediciones,
                         EstacionRepository estaciones, ConfiguracionService configuracion, AuditoriaService auditoria,
                         List<EmisorAlerta> emisores) {
        this.alertas = alertas;
        this.canales = canales;
        this.mediciones = mediciones;
        this.estaciones = estaciones;
        this.configuracion = configuracion;
        this.auditoria = auditoria;
        this.emisores = emisores.stream().collect(Collectors.toMap(EmisorAlerta::canal, Function.identity()));
    }

    public record EstadisticasAlertas(long activas, long ultimas24h, long reactivas, long predictivas, long extraordinarias) {
    }

    @EventListener
    public void alRegistrarMediciones(MedicionesRegistradasEvento evento) {
        evaluarMediciones(mediciones.findAllById(evento.idsMediciones()));
    }

    /** Solo lecturas de las últimas 24 h pueden disparar una alerta reactiva; el histórico importado no. */
    public void evaluarMediciones(List<Medicion> lote) {
        LocalDateTime limiteReciente = LocalDateTime.now().minusHours(24);
        lote.stream()
                .filter(Medicion::esUtilizable)
                .filter(m -> m.getFechaMedicion().isAfter(limiteReciente))
                .collect(Collectors.groupingBy(Medicion::getEstacionId,
                        Collectors.maxBy(Comparator.comparingDouble(Medicion::getPm25))))
                .values().stream().flatMap(Optional::stream)
                .forEach(this::evaluarLectura);
    }

    private void evaluarLectura(Medicion m) {
        IndiceCalidadAire ica = m.ica();
        Optional<Alerta.Nivel> nivel = ica.categoria().nivelAlerta();
        if (nivel.isEmpty() || alertadaRecientemente(m.getEstacionId(), Alerta.Tipo.REACTIVA)) return;
        Estacion estacion = estaciones.findById(m.getEstacionId()).orElse(null);
        if (estacion == null) return;

        String mensaje = "%s registró PM2.5 de %.1f µg/m³ a las %s. ICA %d: %s."
                .formatted(estacion.getNombre(), m.getPm25(), HORA.format(m.getFechaMedicion()), ica.valor(),
                        ica.categoria().nombre().toLowerCase());
        emitir(new Alerta(Alerta.Tipo.REACTIVA, nivel.get(), nivel.get().nombre() + " · " + estacion.getNombre(), mensaje,
                ica.categoria().recomendacionSalud(), estacion, m.getPm25(), "Regla de umbral en tiempo real"));
    }

    @EventListener
    public void alGenerarPronostico(PronosticoGeneradoEvento evento) {
        if (!configuracion.alertasPredictivasHabilitadas()) return;
        Pronostico pronostico = evento.pronostico();
        Pronostico.Punto pico = pronostico.picoEnLasPrimeras(VENTANA_PREDICTIVA_HORAS);
        if (pico.ica().valor() <= 100) return;
        Estacion estacion = estaciones.findById(pronostico.estacionId()).orElse(null);
        if (estacion == null || alertadaRecientemente(estacion.getId(), Alerta.Tipo.PREDICTIVA)) return;

        Alerta.Nivel nivel = pico.ica().categoria().nivelAlerta().orElse(Alerta.Nivel.AMARILLA);
        String mensaje = ("Alerta anticipada: deterioro previsto en la calidad del aire. El modelo %s proyecta PM2.5 de "
                + "%.1f µg/m³ (ICA %d) en %s hacia las %s, con intervalo de confianza de %.1f a %.1f µg/m³.")
                .formatted(pronostico.versionModelo(), pico.pm25(), pico.ica().valor(), estacion.getNombre(),
                        HORA.format(pico.fecha()), pico.inferior(), pico.superior());
        emitir(new Alerta(Alerta.Tipo.PREDICTIVA, nivel, "Alerta predictiva · " + estacion.getNombre(), mensaje,
                "Personas con enfermedades respiratorias: planifique sus actividades y evite exposición prolongada en las próximas horas.",
                estacion, pico.pm25(), "Regla Alerta Predictiva ML (" + pronostico.versionModelo() + ")"));
    }

    public Alerta emitirExtraordinaria(String titulo, String mensaje, Alerta.Nivel nivel, Long estacionId) {
        Estacion estacion = estacionId == null ? null : estaciones.findById(estacionId).orElse(null);
        return emitir(new Alerta(Alerta.Tipo.EXTRAORDINARIA, nivel, titulo.trim(), mensaje.trim(),
                "Siga las indicaciones de las autoridades ambientales y de gestión del riesgo.", estacion, null,
                "Emisión manual del administrador"));
    }

    public Alerta emitirDesdeAnomalia(Anomalia anomalia) {
        IndiceCalidadAire ica = IndiceCalidadAire.desdePm25(anomalia.getValorObservado());
        Alerta.Nivel nivel = ica.categoria().nivelAlerta().orElse(Alerta.Nivel.AMARILLA);
        Estacion estacion = estaciones.findById(anomalia.getEstacionId()).orElse(null);
        String mensaje = "Anomalía %s confirmada por el analista en %s: %s = %.1f %s (+%.1fσ sobre la media móvil de %.1f)."
                .formatted(anomalia.getSeveridad() == Anomalia.Severidad.CRITICA ? "crítica" : "relevante",
                        anomalia.getEstacionNombre(), anomalia.getVariable().etiqueta(), anomalia.getValorObservado(),
                        anomalia.getVariable().unidad(), anomalia.getPuntajeZ(), anomalia.getMediaEsperada());
        return emitir(new Alerta(Alerta.Tipo.REACTIVA, nivel, nivel.nombre() + " · " + anomalia.getEstacionNombre(),
                mensaje, ica.categoria().recomendacionSalud(), estacion, anomalia.getValorObservado(),
                "Detección de anomalías (Z-score)"));
    }

    public List<Alerta> historial() {
        return alertas.findTop100ByOrderByFechaCreacionDesc();
    }

    /** Alertas activas de las últimas 24 h que se muestran en el banner del portal público. */
    public List<Alerta> banner() {
        LocalDateTime desde = LocalDateTime.now().minusHours(24);
        return alertas.findByEstadoOrderByFechaCreacionDesc(Alerta.Estado.ACTIVA).stream()
                .filter(a -> a.seMuestraEnBanner(desde))
                .limit(MAX_ALERTAS_BANNER)
                .toList();
    }

    public EstadisticasAlertas estadisticas() {
        List<Alerta> recientes = alertas.findTop100ByOrderByFechaCreacionDesc();
        return new EstadisticasAlertas(
                recientes.stream().filter(a -> a.getEstado() == Alerta.Estado.ACTIVA).count(),
                alertas.countByFechaCreacionAfter(LocalDateTime.now().minusHours(24)),
                contarTipo(recientes, Alerta.Tipo.REACTIVA),
                contarTipo(recientes, Alerta.Tipo.PREDICTIVA),
                contarTipo(recientes, Alerta.Tipo.EXTRAORDINARIA));
    }

    public List<CanalAlerta> canales() {
        return canales.findAllByOrderByCodigoAsc();
    }

    public CanalAlerta cambiarCanal(CanalAlerta.Codigo codigo, boolean habilitado) {
        CanalAlerta canal = canales.findById(codigo.name())
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("Canal no encontrado."));
        canal.cambiarHabilitado(habilitado);
        canales.save(canal);
        auditoria.info("Alertas", "Canal " + codigo + (habilitado ? " habilitado" : " suspendido"));
        return canal;
    }

    public Alerta resolver(Long id) {
        Alerta alerta = alertas.findById(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("Alerta no encontrada."));
        alerta.resolver();
        alertas.save(alerta);
        auditoria.info("Alertas", "Alerta #" + id + " marcada como resuelta");
        return alerta;
    }

    /** Difunde la alerta por cada canal habilitado que tenga un emisor (Variaciones Protegidas). */
    private Alerta emitir(Alerta alerta) {
        Alerta guardada = alertas.save(alerta);
        List<CanalAlerta.Codigo> entregados = new ArrayList<>();
        for (CanalAlerta canal : canales.findByHabilitadoTrue()) {
            EmisorAlerta emisor = emisores.get(canal.getCodigo());
            if (emisor == null) continue;
            emisor.enviar(guardada);
            canal.registrarEntrega();
            canales.save(canal);
            entregados.add(canal.getCodigo());
        }
        guardada.registrarEntrega(entregados);
        alertas.save(guardada);
        auditoria.advertencia("Alertas", guardada.getTitulo() + " difundida por "
                + (entregados.isEmpty() ? "ningún canal" : entregados.stream().map(Enum::name).collect(Collectors.joining(", "))));
        return guardada;
    }

    private boolean alertadaRecientemente(Long estacionId, Alerta.Tipo tipo) {
        return alertas.existsByEstacionIdAndTipoAndFechaCreacionAfter(estacionId, tipo,
                LocalDateTime.now().minusHours(HORAS_SIN_REPETIR));
    }

    private static long contarTipo(List<Alerta> alertas, Alerta.Tipo tipo) {
        return alertas.stream().filter(a -> a.getTipo() == tipo).count();
    }
}
