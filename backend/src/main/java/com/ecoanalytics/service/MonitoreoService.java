package com.ecoanalytics.service;

import com.ecoanalytics.model.*;
import com.ecoanalytics.repository.*;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Consola técnica (HU-16): salud de la infraestructura, totales, fuentes de datos y tareas programadas. */
@Service
public class MonitoreoService {

    private final EstacionRepository estaciones;
    private final MedicionRepository mediciones;
    private final AnomaliaRepository anomalias;
    private final AlertaRepository alertas;
    private final UsuarioRepository usuarios;
    private final RegistroTareas tareas;
    private final MongoTemplate mongo;

    public MonitoreoService(EstacionRepository estaciones, MedicionRepository mediciones, AnomaliaRepository anomalias,
                            AlertaRepository alertas, UsuarioRepository usuarios, RegistroTareas tareas, MongoTemplate mongo) {
        this.estaciones = estaciones;
        this.mediciones = mediciones;
        this.anomalias = anomalias;
        this.alertas = alertas;
        this.usuarios = usuarios;
        this.tareas = tareas;
        this.mongo = mongo;
    }

    public record Infraestructura(long segundosActivo, long memoriaUsadaMb, long memoriaMaximaMb, double porcentajeCpu,
                                  int procesadores, String baseDatos) {
    }

    public record Totales(long mediciones, long ingestadas24h, long estacionesActivas, long totalEstaciones,
                          long anomaliasPendientes, long alertas24h, long usuarios, long medicionesDescartadas) {
    }

    public record Fuente(Long estacionId, String nombre, TipoFuente tipo, EstadoEstacion estado,
                         LocalDateTime ultimaMedicion, Long minutosDesdeUltima, EstadoSincronizacion sincronizacion) {
    }

    public record Panorama(Infraestructura infraestructura, Totales totales, List<Fuente> fuentes,
                           List<RegistroTareas.EjecucionTarea> servicios) {
    }

    public Panorama panorama() {
        LocalDateTime ahora = LocalDateTime.now();
        List<Fuente> fuentes = estaciones.findAllByOrderByNombreAsc().stream().map(e -> {
            LocalDateTime ultima = mediciones.findFirstByEstacionIdOrderByFechaMedicionDesc(e.getId())
                    .map(Medicion::getFechaMedicion).orElse(null);
            Long minutos = ultima == null ? null : Duration.between(ultima, ahora).toMinutes();
            return new Fuente(e.getId(), e.getNombre(), e.getTipoFuente(), e.getEstado(), ultima, minutos,
                    e.sincronizacion(minutos));
        }).toList();

        Totales totales = new Totales(mediciones.count(), mediciones.countByFechaMedicionAfter(ahora.minusHours(24)),
                estaciones.countByEstado(EstadoEstacion.EN_LINEA), estaciones.count(),
                anomalias.countByEstado(Anomalia.Estado.PENDIENTE), alertas.countByFechaCreacionAfter(ahora.minusHours(24)),
                usuarios.count(), mediciones.countByEstado(EstadoMedicion.DESCARTADA));

        return new Panorama(infraestructura(), totales, fuentes,
                tareas.todas().stream().sorted(Comparator.comparing(RegistroTareas.EjecucionTarea::nombre)).toList());
    }

    private Infraestructura infraestructura() {
        Runtime runtime = Runtime.getRuntime();
        long usada = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        double cpu = -1;
        if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean so) {
            cpu = Estadistica.redondear(Math.max(0, so.getProcessCpuLoad()) * 100);
        }
        return new Infraestructura(ManagementFactory.getRuntimeMXBean().getUptime() / 1000, usada,
                runtime.maxMemory() / (1024 * 1024), cpu, runtime.availableProcessors(), versionBaseDatos());
    }

    private String versionBaseDatos() {
        try {
            Document info = mongo.executeCommand(new Document("buildInfo", 1));
            return "MongoDB " + info.getString("version");
        } catch (RuntimeException e) {
            return "No disponible";
        }
    }
}
