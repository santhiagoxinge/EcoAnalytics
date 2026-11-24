package com.ecoanalytics.infraestructura.simulacion;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Produce lecturas horarias realistas para las estaciones de demostración: picos en horas punta, descenso
 * el fin de semana, las temporadas de contaminación de febrero–marzo y octubre–noviembre del Valle de Aburrá,
 * una mejora lenta año a año y ruido autocorrelacionado. Reemplaza a una red de sensores real.
 */
@Component
public class GeneradorCalidadAireSintetico {

    public record Lectura(double pm25, double pm10, double temperatura, double humedad) {
    }

    private static final Map<String, Double> NIVELES_BASE = Map.of(
            "Estación Centro", 18.0,
            "Estación Parque Norte", 14.0,
            "Estación Belén Alta", 22.0,
            "Estación Poblado Sur", 10.5,
            "Estación Robledo", 16.0);

    private final Random aleatorio = new Random(2026);
    private final Map<String, Double> estadoRuido = new ConcurrentHashMap<>();

    public Lectura siguiente(String nombreEstacion, LocalDateTime fecha) {
        double base = NIVELES_BASE.getOrDefault(nombreEstacion, 15.0);
        double ruido = estadoRuido.merge(nombreEstacion, 0.0, (previo, ignorado) -> 0.85 * previo + aleatorio.nextGaussian() * 0.12);
        double hora = fecha.getHour();
        double diasAtras = Math.max(0, Duration.between(fecha, LocalDateTime.now()).toHours() / 24.0);

        double diurno = 1 + 0.45 * gauss(hora, 8, 2.2) + 0.35 * gauss(hora, 19, 2.6) - 0.25 * gauss(hora, 3, 3);
        DayOfWeek dia = fecha.getDayOfWeek();
        double semanal = dia == DayOfWeek.SUNDAY ? 0.74 : dia == DayOfWeek.SATURDAY ? 0.86 : 1.0;
        int diaDelAno = fecha.getDayOfYear();
        double estacional = 1 + 0.25 * gauss(diaDelAno, 70, 20) + 0.15 * gauss(diaDelAno, 300, 18);
        double tendenciaAnual = 1 + 0.12 * Math.min(diasAtras, 400) / 365.0;

        double pm25 = Math.max(2, base * diurno * semanal * estacional * tendenciaAnual * Math.exp(ruido));
        double pm10 = pm25 * (1.85 + aleatorio.nextGaussian() * 0.12);
        double ciclo = Math.sin(2 * Math.PI * (hora - 9) / 24.0);
        double temperatura = 21.5 + 5 * ciclo + aleatorio.nextGaussian() * 0.8;
        double humedad = Math.min(98, Math.max(30, 70 - 15 * ciclo + aleatorio.nextGaussian() * 3.5));
        return new Lectura(redondear(pm25), redondear(pm10), redondear(temperatura), redondear(humedad));
    }

    private static double gauss(double x, double centro, double ancho) {
        return Math.exp(-Math.pow(x - centro, 2) / (2 * ancho * ancho));
    }

    private static double redondear(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
