package com.ecoanalytics.infraestructura.ml;

import com.ecoanalytics.model.prediccion.AlgoritmoModelo;
import com.ecoanalytics.model.prediccion.ModeloPredictivo;
import com.ecoanalytics.model.prediccion.MuestraEntrenamiento;

import java.util.List;

/**
 * Línea base: combina la última hora, la media de 6 horas y la misma hora del día anterior.
 * El entrenamiento solo busca los pesos de la combinación que minimizan el error cuadrático.
 */
public class ModeloMediaMovil implements ModeloPredictivo {

    private static final int REZAGO1 = 0, REZAGO24 = 3, MEDIA6 = 4;

    private double pesoRezago1 = 0.6;
    private double pesoMedia6 = 0.2;

    @Override
    public AlgoritmoModelo algoritmo() {
        return AlgoritmoModelo.MEDIA_MOVIL;
    }

    @Override
    public void entrenar(List<MuestraEntrenamiento> muestras) {
        double mejorError = Double.MAX_VALUE;
        for (double a = 0; a <= 1.0001; a += 0.05) {
            for (double b = 0; a + b <= 1.0001; b += 0.05) {
                double error = 0;
                for (MuestraEntrenamiento m : muestras) {
                    double diferencia = combinar(m.caracteristicas(), a, b) - m.objetivo();
                    error += diferencia * diferencia;
                }
                if (error < mejorError) {
                    mejorError = error;
                    pesoRezago1 = a;
                    pesoMedia6 = b;
                }
            }
        }
    }

    @Override
    public double predecir(double[] caracteristicas) {
        return combinar(caracteristicas, pesoRezago1, pesoMedia6);
    }

    private static double combinar(double[] f, double a, double b) {
        return a * f[REZAGO1] + b * f[MEDIA6] + (1 - a - b) * f[REZAGO24];
    }
}
