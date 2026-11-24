package com.ecoanalytics.model.prediccion;

import com.ecoanalytics.model.IndiceCalidadAire;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Resultado de una predicción. Siempre incluye el modelo que la produjo y su incertidumbre,
 * para que nunca se presente como una certeza (reglas de negocio 5 y 7 del caso de estudio).
 */
public record Pronostico(
        Long estacionId,
        String estacionNombre,
        String versionModelo,
        AlgoritmoModelo algoritmo,
        double rmseModelo,
        LocalDateTime fechaGeneracion,
        List<Punto> puntos
) {
    public record Punto(LocalDateTime fecha, double pm25, double inferior, double superior, IndiceCalidadAire ica) {
    }

    public Punto pico() {
        return puntos.stream().max(Comparator.comparingDouble(Punto::pm25)).orElseThrow();
    }

    public Punto picoEnLasPrimeras(int horas) {
        return puntos.stream().limit(horas).max(Comparator.comparingDouble(Punto::pm25)).orElseThrow();
    }

    public Pronostico recortado(int horas) {
        return new Pronostico(estacionId, estacionNombre, versionModelo, algoritmo, rmseModelo, fechaGeneracion,
                puntos.subList(0, horas));
    }
}
