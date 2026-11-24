package com.ecoanalytics.model.prediccion;

import com.ecoanalytics.model.Estadistica;

public record MetricasModelo(double r2, double rmse, double mae, int muestrasEntrenamiento, int muestrasPrueba) {

    public static MetricasModelo calcular(double[] reales, double[] predichos, int muestrasEntrenamiento) {
        int n = reales.length;
        double media = 0;
        for (double v : reales) media += v;
        media /= Math.max(1, n);

        double sumaResiduos = 0, sumaTotal = 0, errorAbsoluto = 0;
        for (int i = 0; i < n; i++) {
            double error = reales[i] - predichos[i];
            sumaResiduos += error * error;
            sumaTotal += (reales[i] - media) * (reales[i] - media);
            errorAbsoluto += Math.abs(error);
        }
        double r2 = sumaTotal == 0 ? 0 : 1 - sumaResiduos / sumaTotal;
        return new MetricasModelo(Estadistica.redondear(r2, 3),
                Estadistica.redondear(Math.sqrt(sumaResiduos / Math.max(1, n)), 2),
                Estadistica.redondear(errorAbsoluto / Math.max(1, n), 2), muestrasEntrenamiento, n);
    }
}
