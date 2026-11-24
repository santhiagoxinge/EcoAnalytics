package com.ecoanalytics.model.validacion;

import java.time.LocalDateTime;

/**
 * Una medición tal como llega de cualquier fuente, antes de validarla. Cada adaptador de fuente
 * convierte su propio formato a esta forma común.
 */
public record RegistroMedicionCrudo(
        int fila,
        String estacionNombre,
        LocalDateTime fecha,
        String fechaOriginal,
        Double pm25,
        Double pm10,
        Double temperatura,
        Double humedad
) {
    public RegistroMedicionCrudo conValores(Double pm25, Double pm10, Double temperatura, Double humedad) {
        return new RegistroMedicionCrudo(fila, estacionNombre, fecha, fechaOriginal, pm25, pm10, temperatura, humedad);
    }
}
