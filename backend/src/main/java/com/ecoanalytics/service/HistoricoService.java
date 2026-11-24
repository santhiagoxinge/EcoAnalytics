package com.ecoanalytics.service;

import com.ecoanalytics.exception.ExcepcionNegocio;
import com.ecoanalytics.model.*;
import com.ecoanalytics.repository.MedicionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Consulta y exportación del histórico de mediciones (HU-07). */
@Service
public class HistoricoService {

    private static final DateTimeFormatter FECHA_CSV = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int RANGO_MAXIMO_DIAS = 400;

    private final MedicionRepository mediciones;
    private final EstacionService estacionService;

    public HistoricoService(MedicionRepository mediciones, EstacionService estacionService) {
        this.mediciones = mediciones;
        this.estacionService = estacionService;
    }

    public record Filtro(Long estacionId, LocalDate desde, LocalDate hasta, boolean incluirAdvertencias,
                         boolean incluirDescartadas) {
        public Filtro {
            if (hasta.isBefore(desde)) throw ExcepcionNegocio.invalido("La fecha final debe ser posterior a la inicial.");
            if (desde.plusDays(RANGO_MAXIMO_DIAS).isBefore(hasta)) {
                throw ExcepcionNegocio.invalido("El rango máximo de consulta es de " + RANGO_MAXIMO_DIAS + " días.");
            }
        }
    }

    public enum Granularidad { DIARIA, HORARIA }

    public record Fila(Long id, LocalDateTime fecha, Double pm25, Double pm10, Double temperatura, Double humedad,
                       Integer ica, EstadoMedicion estado, TipoFuente fuente) {
    }

    public record PuntoGrafico(LocalDateTime fecha, Double pm25, Double pm10) {
    }

    public record Resumen(int total, Double mediaPm25, Double maxPm25, Double minPm25, long advertencias, long descartadas) {
    }

    public record PaginaHistorico(String estacionNombre, LocalDate desde, LocalDate hasta, Resumen resumen,
                                  Granularidad granularidad, List<PuntoGrafico> grafico, List<Fila> filas,
                                  int pagina, int tamanoPagina, int totalPaginas) {
    }

    public PaginaHistorico consultar(Filtro filtro, int pagina, int tamanoPagina) {
        Estacion estacion = estacionService.buscar(filtro.estacionId());
        List<Medicion> todas = cargar(filtro);
        List<Medicion> utiles = todas.stream().filter(m -> m.getEstado() != EstadoMedicion.DESCARTADA).toList();

        List<Double> pm25 = utiles.stream().map(Medicion::getPm25).toList();
        Resumen resumen = new Resumen(todas.size(), Estadistica.media(pm25), Estadistica.maximo(pm25), Estadistica.minimo(pm25),
                todas.stream().filter(m -> m.getEstado() == EstadoMedicion.ADVERTENCIA).count(),
                todas.stream().filter(m -> m.getEstado() == EstadoMedicion.DESCARTADA).count());

        boolean diario = ChronoUnit.DAYS.between(filtro.desde(), filtro.hasta()) > 3;
        List<PuntoGrafico> grafico = diario ? graficoDiario(utiles) : utiles.stream()
                .map(m -> new PuntoGrafico(m.getFechaMedicion(), m.getPm25(), m.getPm10())).toList();

        int tamano = Math.min(Math.max(tamanoPagina, 5), 100);
        int totalPaginas = Math.max(1, (int) Math.ceil(todas.size() / (double) tamano));
        int paginaSegura = Math.min(Math.max(1, pagina), totalPaginas);
        List<Fila> filas = todas.reversed().stream()
                .skip((long) (paginaSegura - 1) * tamano)
                .limit(tamano)
                .map(HistoricoService::aFila)
                .toList();
        return new PaginaHistorico(estacion.getNombre(), filtro.desde(), filtro.hasta(), resumen,
                diario ? Granularidad.DIARIA : Granularidad.HORARIA, grafico, filas, paginaSegura, tamano, totalPaginas);
    }

    public String exportarCsv(Filtro filtro) {
        StringBuilder csv = new StringBuilder("Fecha y hora,PM2.5 (µg/m³),PM10 (µg/m³),Temperatura (°C),Humedad (%),ICA,Estado,Fuente\n");
        for (Medicion m : cargar(filtro)) {
            Fila f = aFila(m);
            csv.append(FECHA_CSV.format(f.fecha())).append(',')
                    .append(texto(f.pm25())).append(',').append(texto(f.pm10())).append(',')
                    .append(texto(f.temperatura())).append(',').append(texto(f.humedad())).append(',')
                    .append(f.ica() == null ? "" : f.ica()).append(',')
                    .append(f.estado()).append(',').append(f.fuente()).append('\n');
        }
        return csv.toString();
    }

    public String nombreArchivoExportacion(Filtro filtro) {
        String estacion = estacionService.buscar(filtro.estacionId()).getNombre().toLowerCase()
                .replaceAll("[^a-z0-9]+", "_");
        return "historico_%s_%s-%s.csv".formatted(estacion, filtro.desde(), filtro.hasta());
    }

    private List<Medicion> cargar(Filtro filtro) {
        LocalDateTime desde = filtro.desde().atStartOfDay();
        LocalDateTime hasta = filtro.hasta().plusDays(1).atStartOfDay().minusSeconds(1);
        return mediciones.buscarPorEstacionYRango(filtro.estacionId(), desde, hasta).stream()
                .filter(m -> filtro.incluirAdvertencias() || m.getEstado() != EstadoMedicion.ADVERTENCIA)
                .filter(m -> filtro.incluirDescartadas() || m.getEstado() != EstadoMedicion.DESCARTADA)
                .toList();
    }

    private static List<PuntoGrafico> graficoDiario(List<Medicion> utiles) {
        Map<LocalDate, List<Medicion>> porDia = utiles.stream()
                .collect(Collectors.groupingBy(m -> m.getFechaMedicion().toLocalDate(), TreeMap::new, Collectors.toList()));
        return porDia.entrySet().stream()
                .map(e -> new PuntoGrafico(e.getKey().atStartOfDay(),
                        Estadistica.media(e.getValue().stream().map(Medicion::getPm25).toList()),
                        Estadistica.media(e.getValue().stream().map(Medicion::getPm10).toList())))
                .toList();
    }

    private static Fila aFila(Medicion m) {
        Integer ica = m.getPm25() == null ? null : m.ica().valor();
        return new Fila(m.getId(), m.getFechaMedicion(), m.getPm25(), m.getPm10(), m.getTemperatura(), m.getHumedad(),
                ica, m.getEstado(), m.getFuente());
    }

    private static String texto(Double valor) {
        return valor == null ? "" : String.valueOf(valor);
    }
}
