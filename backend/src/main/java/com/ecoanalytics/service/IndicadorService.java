package com.ecoanalytics.service;

import com.ecoanalytics.exception.ExcepcionNegocio;
import com.ecoanalytics.model.*;
import com.ecoanalytics.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/** Indicadores agregados (HU-10): convierte lecturas horarias en cifras sintéticas y comparables. */
@Service
public class IndicadorService {

    private final ConfiguracionIndicadorRepository configuraciones;
    private final ValorIndicadorRepository valores;
    private final MedicionRepository mediciones;
    private final EstacionRepository estaciones;
    private final InsercionMasiva insercion;
    private final AuditoriaService auditoria;

    public IndicadorService(ConfiguracionIndicadorRepository configuraciones, ValorIndicadorRepository valores,
                            MedicionRepository mediciones, EstacionRepository estaciones, InsercionMasiva insercion,
                            AuditoriaService auditoria) {
        this.configuraciones = configuraciones;
        this.valores = valores;
        this.mediciones = mediciones;
        this.estaciones = estaciones;
        this.insercion = insercion;
        this.auditoria = auditoria;
    }

    public record IndicadoresEstacion(Long estacionId, String estacionNombre, Map<TipoIndicador, Double> valores,
                                      LocalDateTime fechaCalculo) {
    }

    public record ResultadoRecalculo(int registrosProcesados, int indicadores, LocalDateTime fechaEjecucion) {
    }

    public List<ConfiguracionIndicador> configuraciones() {
        return configuraciones.findAll().stream().sorted(Comparator.comparing(ConfiguracionIndicador::getTipo)).toList();
    }

    public ConfiguracionIndicador cambiarHabilitado(TipoIndicador tipo, boolean habilitado) {
        ConfiguracionIndicador configuracion = configuraciones.findById(tipo.name())
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("Indicador no encontrado."));
        configuracion.cambiarHabilitado(habilitado);
        configuraciones.save(configuracion);
        auditoria.info("Indicadores", "Indicador " + tipo + (habilitado ? " reanudado" : " suspendido (histórico conservado)"));
        return configuracion;
    }

    public List<IndicadoresEstacion> ultimos() {
        return estaciones.findAllByOrderByNombreAsc().stream().map(estacion -> {
            Map<TipoIndicador, Double> mapa = new EnumMap<>(TipoIndicador.class);
            LocalDateTime fechaCalculo = null;
            for (TipoIndicador tipo : TipoIndicador.values()) {
                Optional<ValorIndicador> v = valores.findFirstByEstacionIdAndTipoOrderByFechaCalculoDesc(estacion.getId(), tipo);
                if (v.isPresent()) {
                    mapa.put(tipo, v.get().getValor());
                    if (fechaCalculo == null || v.get().getFechaCalculo().isAfter(fechaCalculo)) {
                        fechaCalculo = v.get().getFechaCalculo();
                    }
                }
            }
            return new IndicadoresEstacion(estacion.getId(), estacion.getNombre(), mapa, fechaCalculo);
        }).toList();
    }

    /** Cada tipo de indicador sabe calcularse (Polimorfismo); el servicio solo los recorre y guarda. */
    public ResultadoRecalculo recalcular() {
        List<ConfiguracionIndicador> habilitados = configuraciones.findByHabilitadoTrue();
        LocalDateTime ahora = LocalDateTime.now();
        int procesados = 0;
        Map<TipoIndicador, Integer> porIndicador = new EnumMap<>(TipoIndicador.class);
        List<ValorIndicador> nuevos = new ArrayList<>();

        for (Estacion estacion : estaciones.findAll()) {
            List<Medicion> mes = mediciones.buscarUtilesPorEstacionYRango(estacion.getId(), ahora.minusDays(31), ahora)
                    .stream().filter(m -> m.getPm25() != null).toList();
            if (mes.isEmpty()) continue;
            procesados += mes.size();
            for (ConfiguracionIndicador configuracion : habilitados) {
                Double valor = configuracion.getTipo().calcular(mes, ahora);
                if (valor == null) continue;
                nuevos.add(new ValorIndicador(estacion.getId(), configuracion.getTipo(), valor));
                porIndicador.merge(configuracion.getTipo(), mes.size(), Integer::sum);
            }
        }
        insercion.insertar(nuevos, ValorIndicador.class);
        for (ConfiguracionIndicador configuracion : habilitados) {
            configuracion.registrarEjecucion(porIndicador.getOrDefault(configuracion.getTipo(), 0));
        }
        configuraciones.saveAll(habilitados);
        auditoria.info("Indicadores", "Recálculo de métricas: " + nuevos.size() + " indicadores sobre " + procesados + " registros");
        return new ResultadoRecalculo(procesados, nuevos.size(), ahora);
    }
}
