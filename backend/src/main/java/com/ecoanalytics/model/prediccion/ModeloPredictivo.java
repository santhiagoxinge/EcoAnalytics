package com.ecoanalytics.model.prediccion;

import java.util.List;

/**
 * Contrato común de todo modelo predictivo (patrón Strategy). El pronóstico depende solo de esta
 * abstracción, así que un algoritmo nuevo se agrega implementándola sin tocar el resto
 * (Polimorfismo y Variaciones Protegidas).
 */
public interface ModeloPredictivo {

    AlgoritmoModelo algoritmo();

    void entrenar(List<MuestraEntrenamiento> muestras);

    double predecir(double[] caracteristicas);

    default MetricasModelo evaluar(List<MuestraEntrenamiento> muestrasPrueba, int muestrasEntrenamiento) {
        double[] reales = new double[muestrasPrueba.size()];
        double[] predichos = new double[muestrasPrueba.size()];
        for (int i = 0; i < muestrasPrueba.size(); i++) {
            reales[i] = muestrasPrueba.get(i).objetivo();
            predichos[i] = predecir(muestrasPrueba.get(i).caracteristicas());
        }
        return MetricasModelo.calcular(reales, predichos, muestrasEntrenamiento);
    }
}
