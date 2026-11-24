package com.ecoanalytics.service;

import com.ecoanalytics.model.Estacion;
import com.ecoanalytics.model.Estadistica;
import com.ecoanalytics.model.Medicion;
import com.ecoanalytics.repository.MedicionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Series, correlación y mapa de calor para la visualización avanzada (HU-09). */
@Service
public class VisualizacionService {

    private static final int MAX_PUNTOS_DISPERSION = 500;

    private final MedicionRepository mediciones;
    private final EstacionService estacionService;

    public VisualizacionService(MedicionRepository mediciones, EstacionService estacionService) {
        this.mediciones = mediciones;
        this.estacionService = estacionService;
    }

    public record PuntoSerie(LocalDateTime fecha, Double pm25, Double pm10, Double temperatura, Double humedad) {
    }

    public record PuntoDispersion(double temperatura, double pm25) {
    }

    public record Correlacion(double pearsonR, double pendiente, double intercepto, List<PuntoDispersion> puntos) {
    }

    /** filas = lunes..domingo, columnas = hora 0..23; null cuando no hay datos para esa celda. */
    public record MapaCalor(Double[][] celdas, Double minimo, Double maximo) {
    }

    public record Visualizacion(String estacionNombre, int dias, List<PuntoSerie> serie, Correlacion correlacion,
                                MapaCalor mapaCalor) {
    }

    public Visualizacion construir(Long estacionId, int dias) {
        Estacion estacion = estacionService.buscar(estacionId);
        int diasSeguros = Math.min(Math.max(dias, 1), 90);
        LocalDateTime hasta = LocalDateTime.now();
        List<Medicion> recientes = utiles(estacionId, hasta.minusDays(diasSeguros), hasta);
        List<Medicion> cuatroSemanas = diasSeguros >= 28 ? recientes : utiles(estacionId, hasta.minusDays(28), hasta);

        List<PuntoSerie> serie = recientes.stream()
                .map(m -> new PuntoSerie(m.getFechaMedicion(), m.getPm25(), m.getPm10(), m.getTemperatura(), m.getHumedad()))
                .toList();
        return new Visualizacion(estacion.getNombre(), diasSeguros, serie, correlacion(cuatroSemanas), mapaCalor(cuatroSemanas));
    }

    private List<Medicion> utiles(Long estacionId, LocalDateTime desde, LocalDateTime hasta) {
        return mediciones.buscarUtilesPorEstacionYRango(estacionId, desde, hasta).stream()
                .filter(m -> m.getPm25() != null).toList();
    }

    /** Correlación de Pearson y recta de mínimos cuadrados de PM2.5 contra temperatura. */
    private static Correlacion correlacion(List<Medicion> datos) {
        List<double[]> pares = datos.stream().filter(m -> m.getTemperatura() != null)
                .map(m -> new double[]{m.getTemperatura(), m.getPm25()}).toList();
        int n = pares.size();
        if (n < 3) return new Correlacion(0, 0, 0, List.of());

        double sx = 0, sy = 0, sxx = 0, syy = 0, sxy = 0;
        for (double[] p : pares) {
            sx += p[0]; sy += p[1];
            sxx += p[0] * p[0]; syy += p[1] * p[1]; sxy += p[0] * p[1];
        }
        double covarianza = sxy - sx * sy / n;
        double varX = sxx - sx * sx / n;
        double varY = syy - sy * sy / n;
        double r = varX == 0 || varY == 0 ? 0 : covarianza / Math.sqrt(varX * varY);
        double pendiente = varX == 0 ? 0 : covarianza / varX;
        double intercepto = (sy - pendiente * sx) / n;

        int paso = Math.max(1, n / MAX_PUNTOS_DISPERSION);
        List<PuntoDispersion> puntos = new ArrayList<>();
        for (int i = 0; i < n; i += paso) puntos.add(new PuntoDispersion(pares.get(i)[0], pares.get(i)[1]));
        return new Correlacion(Estadistica.redondear(r, 2), Estadistica.redondear(pendiente, 3),
                Estadistica.redondear(intercepto, 2), puntos);
    }

    private static MapaCalor mapaCalor(List<Medicion> datos) {
        double[][] sumas = new double[7][24];
        int[][] conteos = new int[7][24];
        for (Medicion m : datos) {
            int dia = m.getFechaMedicion().getDayOfWeek().getValue() - 1;
            int hora = m.getFechaMedicion().getHour();
            sumas[dia][hora] += m.getPm25();
            conteos[dia][hora]++;
        }
        Double[][] celdas = new Double[7][24];
        Double minimo = null, maximo = null;
        for (int d = 0; d < 7; d++) {
            for (int h = 0; h < 24; h++) {
                if (conteos[d][h] == 0) continue;
                double v = Estadistica.redondear(sumas[d][h] / conteos[d][h]);
                celdas[d][h] = v;
                minimo = minimo == null ? v : Math.min(minimo, v);
                maximo = maximo == null ? v : Math.max(maximo, v);
            }
        }
        return new MapaCalor(celdas, minimo, maximo);
    }
}
