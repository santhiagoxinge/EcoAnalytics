package com.ecoanalytics.model;

import java.util.Collection;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Estadística descriptiva compartida por los servicios de análisis y los indicadores.
 * Fabricación Pura: no representa un concepto del dominio, pero evita repetir los cálculos.
 */
public final class Estadistica {

    private Estadistica() {
    }

    public static Double media(Collection<Double> valores) {
        OptionalDouble promedio = valores.stream().filter(Objects::nonNull).mapToDouble(Double::doubleValue).average();
        return promedio.isPresent() ? redondear(promedio.getAsDouble()) : null;
    }

    public static Double maximo(Collection<Double> valores) {
        OptionalDouble max = valores.stream().filter(Objects::nonNull).mapToDouble(Double::doubleValue).max();
        return max.isPresent() ? redondear(max.getAsDouble()) : null;
    }

    public static Double minimo(Collection<Double> valores) {
        OptionalDouble min = valores.stream().filter(Objects::nonNull).mapToDouble(Double::doubleValue).min();
        return min.isPresent() ? redondear(min.getAsDouble()) : null;
    }

    /** Variación porcentual de {@code antes} a {@code despues}; null cuando no se puede calcular. */
    public static Double variacionPorcentual(Double antes, Double despues) {
        if (antes == null || despues == null || antes == 0) return null;
        return redondear((despues - antes) / antes * 100);
    }

    public static double redondear(double valor) {
        return Math.round(valor * 10) / 10.0;
    }

    public static double redondear(double valor, int decimales) {
        double factor = Math.pow(10, decimales);
        return Math.round(valor * factor) / factor;
    }
}
