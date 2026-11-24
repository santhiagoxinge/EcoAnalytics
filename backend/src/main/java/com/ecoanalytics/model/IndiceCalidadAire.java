package com.ecoanalytics.model;

/**
 * Objeto valor: un ICA con su categoría. Se calcula por interpolación lineal entre los puntos de corte
 * de concentración definidos por la Resolución 2254 de 2017 (Colombia).
 */
public record IndiceCalidadAire(int valor, CategoriaIca categoria) {

    /** Guía diaria OMS 2021 para PM2.5: base de los indicadores de excedencia y del pronóstico. */
    public static final double LIMITE_DIARIO_OMS_PM25 = 15.0;
    /** Guía diaria OMS 2021 para PM10. */
    public static final double LIMITE_DIARIO_OMS_PM10 = 45.0;

    private static final double[][] CORTES_PM25 = {
            {0, 12, 0, 50}, {12.1, 37, 51, 100}, {37.1, 55, 101, 150},
            {55.1, 150, 151, 200}, {150.1, 250, 201, 300}, {250.1, 500, 301, 500}
    };

    private static final double[][] CORTES_PM10 = {
            {0, 54, 0, 50}, {55, 154, 51, 100}, {155, 254, 101, 150},
            {255, 354, 151, 200}, {355, 424, 201, 300}, {425, 604, 301, 500}
    };

    public static IndiceCalidadAire desdePm25(double concentracion) {
        return interpolar(concentracion, CORTES_PM25, 1);
    }

    public static IndiceCalidadAire desdePm10(double concentracion) {
        return interpolar(concentracion, CORTES_PM10, 0);
    }

    /** El ICA reportado es el peor de los subíndices de cada contaminante. */
    public static IndiceCalidadAire combinado(Double pm25, Double pm10) {
        IndiceCalidadAire peor = pm25 != null ? desdePm25(pm25) : null;
        if (pm10 != null) {
            IndiceCalidadAire porPm10 = desdePm10(pm10);
            if (peor == null || porPm10.valor() > peor.valor()) peor = porPm10;
        }
        return peor != null ? peor : new IndiceCalidadAire(0, CategoriaIca.BUENA);
    }

    private static IndiceCalidadAire interpolar(double concentracion, double[][] cortes, int decimales) {
        // Truncar a la precisión de los cortes garantiza que el valor nunca caiga entre dos rangos.
        double factor = Math.pow(10, decimales);
        double c = Math.floor(Math.max(0, concentracion) * factor) / factor;
        for (double[] corte : cortes) {
            if (c <= corte[1]) {
                int indice = (int) Math.round((corte[3] - corte[2]) / (corte[1] - corte[0]) * (c - corte[0]) + corte[2]);
                return new IndiceCalidadAire(indice, CategoriaIca.desdeIndice(indice));
            }
        }
        return new IndiceCalidadAire(500, CategoriaIca.PELIGROSA);
    }
}
