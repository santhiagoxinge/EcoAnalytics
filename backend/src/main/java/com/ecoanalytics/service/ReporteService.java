package com.ecoanalytics.service;

import com.ecoanalytics.exception.ExcepcionNegocio;
import com.ecoanalytics.infraestructura.seguridad.ActorActual;
import com.ecoanalytics.model.*;
import com.ecoanalytics.model.reporte.DocumentoReporte;
import com.ecoanalytics.model.reporte.DocumentoReporte.TipoReporte;
import com.ecoanalytics.model.reporte.ExportadorReporte;
import com.ecoanalytics.repository.MedicionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Generación de reportes y su exportación en PDF, CSV o Excel (HU-15). */
@Service
public class ReporteService {

    private final MedicionRepository mediciones;
    private final EstacionService estacionService;
    private final PronosticoService pronosticoService;
    private final AuditoriaService auditoria;
    private final Map<ExportadorReporte.Formato, ExportadorReporte> exportadores;

    public ReporteService(MedicionRepository mediciones, EstacionService estacionService,
                          PronosticoService pronosticoService, AuditoriaService auditoria,
                          List<ExportadorReporte> exportadores) {
        this.mediciones = mediciones;
        this.estacionService = estacionService;
        this.pronosticoService = pronosticoService;
        this.auditoria = auditoria;
        this.exportadores = exportadores.stream().collect(Collectors.toMap(ExportadorReporte::formato, Function.identity()));
    }

    public record SolicitudReporte(TipoReporte tipo, Long estacionId, LocalDate desde, LocalDate hasta) {
    }

    public record ReporteExportado(String nombreArchivo, String tipoContenido, byte[] contenido) {
    }

    public DocumentoReporte vistaPrevia(SolicitudReporte solicitud) {
        if (solicitud.hasta().isBefore(solicitud.desde())) {
            throw ExcepcionNegocio.invalido("La fecha final debe ser posterior a la inicial.");
        }
        Estacion estacion = estacionService.buscar(solicitud.estacionId());
        List<Medicion> datos = mediciones.buscarUtilesPorEstacionYRango(estacion.getId(),
                        solicitud.desde().atStartOfDay(), solicitud.hasta().plusDays(1).atStartOfDay().minusSeconds(1))
                .stream().filter(m -> m.getPm25() != null).toList();
        if (datos.isEmpty()) {
            throw ExcepcionNegocio.invalido("No hay datos disponibles para el periodo o estación seleccionada");
        }

        Map<LocalDate, List<Medicion>> porDia = datos.stream()
                .collect(Collectors.groupingBy(m -> m.getFechaMedicion().toLocalDate(), TreeMap::new, Collectors.toList()));
        List<DocumentoReporte.FilaDiaria> diario = porDia.entrySet().stream().map(e -> {
            Double media = Estadistica.media(valores(e.getValue(), VariableAmbiental.PM25));
            IndiceCalidadAire ica = IndiceCalidadAire.desdePm25(media);
            return new DocumentoReporte.FilaDiaria(e.getKey(), media,
                    Estadistica.maximo(valores(e.getValue(), VariableAmbiental.PM25)),
                    Estadistica.media(valores(e.getValue(), VariableAmbiental.PM10)), ica.valor(), ica.categoria().nombre());
        }).toList();
        long diasCumplimiento = diario.stream()
                .filter(f -> f.mediaPm25() <= IndiceCalidadAire.LIMITE_DIARIO_OMS_PM25).count();

        List<DocumentoReporte.FilaVariable> variables = Arrays.stream(VariableAmbiental.values())
                .map(v -> filaVariable(v, valores(datos, v)))
                .toList();

        List<DocumentoReporte.FilaPronostico> pronostico = List.of();
        if (solicitud.tipo() == TipoReporte.PRONOSTICO) {
            pronostico = pronosticoService.pronosticar(estacion.getId(), 48).pronostico().puntos().stream()
                    .map(p -> new DocumentoReporte.FilaPronostico(p.fecha(), p.pm25(), p.inferior(), p.superior(), p.ica().valor()))
                    .toList();
        }
        String reporteId = "ECO-RPT-%d-%04d".formatted(LocalDate.now().getYear(), Math.abs(Objects.hash(
                estacion.getId(), solicitud.desde(), solicitud.hasta(), solicitud.tipo())) % 10000);

        return new DocumentoReporte(reporteId, solicitud.tipo(), "Reporte " + reporteId, estacion.getNombre(),
                estacion.getZona(), solicitud.desde(), solicitud.hasta(), LocalDateTime.now(), ActorActual.nombre(),
                Estadistica.media(valores(datos, VariableAmbiental.PM25)),
                Estadistica.redondear(diasCumplimiento * 100.0 / diario.size()), datos.size(), variables,
                solicitud.tipo() == TipoReporte.RESUMEN_EJECUTIVO ? List.of() : diario, pronostico);
    }

    public ReporteExportado exportar(SolicitudReporte solicitud, ExportadorReporte.Formato formato) {
        DocumentoReporte documento = vistaPrevia(solicitud);
        ExportadorReporte exportador = exportadores.get(formato);
        byte[] contenido = exportador.exportar(documento);
        auditoria.info("Reportes", "Exportación " + formato + " del reporte " + documento.reporteId()
                + " (" + documento.estacionNombre() + ")");
        return new ReporteExportado("ecoanalytics_reporte_" + LocalDate.now() + "." + exportador.extension(),
                exportador.tipoContenido(), contenido);
    }

    private static List<Double> valores(List<Medicion> datos, VariableAmbiental variable) {
        return datos.stream().map(m -> m.valor(variable)).filter(Objects::nonNull).toList();
    }

    private static DocumentoReporte.FilaVariable filaVariable(VariableAmbiental variable, List<Double> valores) {
        Double media = Estadistica.media(valores);
        String evaluacion = switch (variable) {
            case PM25 -> evaluarOms(media, IndiceCalidadAire.LIMITE_DIARIO_OMS_PM25);
            case PM10 -> evaluarOms(media, IndiceCalidadAire.LIMITE_DIARIO_OMS_PM10);
            case TEMPERATURA, HUMEDAD -> "Variable de contexto";
        };
        return new DocumentoReporte.FilaVariable(variable.etiqueta(), variable.unidad(), media, Estadistica.minimo(valores),
                Estadistica.maximo(valores), evaluacion);
    }

    private static String evaluarOms(Double media, double limite) {
        if (media == null) return "Sin datos";
        return media <= limite ? "Dentro de la guía OMS" : "Supera la guía OMS (%d µg/m³)".formatted((int) limite);
    }
}
