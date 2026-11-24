package com.ecoanalytics.service;

import com.ecoanalytics.model.*;
import com.ecoanalytics.repository.EstacionRepository;
import com.ecoanalytics.repository.MedicionRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Condiciones actuales del aire por estación (HU-06), también para el portal ciudadano. */
@Service
public class CondicionesService {

    private final EstacionRepository estaciones;
    private final MedicionRepository mediciones;
    private final EstacionService estacionService;

    public CondicionesService(EstacionRepository estaciones, MedicionRepository mediciones, EstacionService estacionService) {
        this.estaciones = estaciones;
        this.mediciones = mediciones;
        this.estacionService = estacionService;
    }

    public record PuntoTendencia(LocalDateTime fecha, double pm25) {
    }

    public record CondicionesActuales(Long estacionId, String estacionNombre, String zona, EstadoEstacion estado,
                                      boolean disponible, String motivoNoDisponible, LocalDateTime fechaMedicion,
                                      Long minutosTranscurridos, Double pm25, Double pm10, Double temperatura,
                                      Double humedad, Integer ica, CategoriaIca categoria, String recomendacion,
                                      List<PuntoTendencia> tendencia24h) {
    }

    public record ResumenEstacion(Long id, String nombre, String zona, EstadoEstacion estado, double latitud,
                                  double longitud, boolean disponible, Integer ica, CategoriaIca categoria,
                                  Double pm25, LocalDateTime fechaMedicion) {
    }

    public CondicionesActuales actuales(Long estacionId) {
        Estacion estacion = estacionService.buscar(estacionId);
        Optional<Medicion> ultima = ultimaUtil(estacion);
        LocalDateTime ahora = LocalDateTime.now();
        Optional<String> motivo = estacion.motivoNoDisponible(ultima, ahora);
        if (motivo.isPresent()) {
            return new CondicionesActuales(estacion.getId(), estacion.getNombre(), estacion.getZona(), estacion.getEstado(),
                    false, motivo.get(), ultima.map(Medicion::getFechaMedicion).orElse(null), null,
                    null, null, null, null, null, null, null, List.of());
        }
        Medicion m = ultima.get();
        IndiceCalidadAire ica = m.ica();
        List<PuntoTendencia> tendencia = mediciones.buscarUtilesPorEstacionYRango(
                        estacion.getId(), m.getFechaMedicion().minusHours(24), m.getFechaMedicion())
                .stream()
                .filter(x -> x.getPm25() != null)
                .map(x -> new PuntoTendencia(x.getFechaMedicion(), x.getPm25()))
                .toList();
        return new CondicionesActuales(estacion.getId(), estacion.getNombre(), estacion.getZona(), estacion.getEstado(),
                true, null, m.getFechaMedicion(), Math.max(0, Duration.between(m.getFechaMedicion(), ahora).toMinutes()),
                m.getPm25(), m.getPm10(), m.getTemperatura(), m.getHumedad(),
                ica.valor(), ica.categoria(), ica.categoria().recomendacionSalud(), tendencia);
    }

    public List<ResumenEstacion> resumen() {
        LocalDateTime ahora = LocalDateTime.now();
        return estaciones.findAllByOrderByNombreAsc().stream().map(estacion -> {
            Optional<Medicion> ultima = ultimaUtil(estacion);
            boolean disponible = estacion.motivoNoDisponible(ultima, ahora).isEmpty();
            IndiceCalidadAire ica = disponible ? ultima.get().ica() : null;
            return new ResumenEstacion(estacion.getId(), estacion.getNombre(), estacion.getZona(), estacion.getEstado(),
                    estacion.getLatitud(), estacion.getLongitud(), disponible,
                    ica == null ? null : ica.valor(), ica == null ? null : ica.categoria(),
                    disponible ? ultima.get().getPm25() : null,
                    ultima.map(Medicion::getFechaMedicion).orElse(null));
        }).toList();
    }

    private Optional<Medicion> ultimaUtil(Estacion estacion) {
        return mediciones.findFirstByEstacionIdAndEstadoNotOrderByFechaMedicionDesc(estacion.getId(), EstadoMedicion.DESCARTADA);
    }
}
