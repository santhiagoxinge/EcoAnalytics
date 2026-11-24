package com.ecoanalytics.infraestructura.ml;

import com.ecoanalytics.model.prediccion.AlgoritmoModelo;
import com.ecoanalytics.model.prediccion.ModeloPredictivo;
import com.ecoanalytics.model.prediccion.MuestraEntrenamiento;

import java.util.List;
import java.util.PriorityQueue;

/** K vecinos más cercanos ponderados por distancia, sobre características estandarizadas. */
public class ModeloKVecinos implements ModeloPredictivo {

    private static final int K = 12;
    private static final int MAX_MUESTRAS_REFERENCIA = 6000;

    private double[][] puntos;
    private double[] objetivos;
    private double[] medias;
    private double[] escalas;

    @Override
    public AlgoritmoModelo algoritmo() {
        return AlgoritmoModelo.K_VECINOS;
    }

    @Override
    public void entrenar(List<MuestraEntrenamiento> muestras) {
        // Se conservan las muestras más recientes: representan mejor el régimen actual y acotan el costo de consulta.
        List<MuestraEntrenamiento> referencia = muestras.size() > MAX_MUESTRAS_REFERENCIA
                ? muestras.subList(muestras.size() - MAX_MUESTRAS_REFERENCIA, muestras.size())
                : muestras;
        int p = referencia.getFirst().caracteristicas().length;
        medias = new double[p];
        escalas = new double[p];
        for (MuestraEntrenamiento m : referencia) for (int j = 0; j < p; j++) medias[j] += m.caracteristicas()[j];
        for (int j = 0; j < p; j++) medias[j] /= referencia.size();
        for (MuestraEntrenamiento m : referencia) {
            for (int j = 0; j < p; j++) escalas[j] += Math.pow(m.caracteristicas()[j] - medias[j], 2);
        }
        for (int j = 0; j < p; j++) escalas[j] = Math.max(1e-9, Math.sqrt(escalas[j] / referencia.size()));

        puntos = new double[referencia.size()][];
        objetivos = new double[referencia.size()];
        for (int i = 0; i < referencia.size(); i++) {
            puntos[i] = estandarizar(referencia.get(i).caracteristicas());
            objetivos[i] = referencia.get(i).objetivo();
        }
    }

    @Override
    public double predecir(double[] caracteristicas) {
        double[] q = estandarizar(caracteristicas);
        PriorityQueue<double[]> cercanos = new PriorityQueue<>((a, b) -> Double.compare(b[0], a[0]));
        for (int i = 0; i < puntos.length; i++) {
            double d = 0;
            for (int j = 0; j < q.length; j++) d += (puntos[i][j] - q[j]) * (puntos[i][j] - q[j]);
            if (cercanos.size() < K) cercanos.add(new double[]{d, objetivos[i]});
            else if (d < cercanos.peek()[0]) {
                cercanos.poll();
                cercanos.add(new double[]{d, objetivos[i]});
            }
        }
        double sumaPesos = 0, suma = 0;
        for (double[] vecino : cercanos) {
            double peso = 1 / (Math.sqrt(vecino[0]) + 1e-6);
            sumaPesos += peso;
            suma += peso * vecino[1];
        }
        return suma / sumaPesos;
    }

    private double[] estandarizar(double[] f) {
        double[] z = new double[f.length];
        for (int j = 0; j < f.length; j++) z[j] = (f[j] - medias[j]) / escalas[j];
        return z;
    }
}
