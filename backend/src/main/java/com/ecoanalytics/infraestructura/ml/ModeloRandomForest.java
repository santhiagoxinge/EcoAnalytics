package com.ecoanalytics.infraestructura.ml;

import com.ecoanalytics.model.prediccion.AlgoritmoModelo;
import com.ecoanalytics.model.prediccion.ModeloPredictivo;
import com.ecoanalytics.model.prediccion.MuestraEntrenamiento;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Conjunto (bagging) de árboles de regresión. Cada árbol ve una muestra bootstrap y un subconjunto
 * aleatorio de características por división; los umbrales candidatos salen de cuantiles para entrenar rápido.
 */
public class ModeloRandomForest implements ModeloPredictivo {

    private static final int ARBOLES = 40;
    private static final int PROFUNDIDAD_MAXIMA = 9;
    private static final int MIN_HOJA = 15;
    private static final int UMBRALES_CANDIDATOS = 16;

    private final Random aleatorio = new Random(42);
    private final List<Nodo> bosque = new ArrayList<>();

    @Override
    public AlgoritmoModelo algoritmo() {
        return AlgoritmoModelo.RANDOM_FOREST;
    }

    @Override
    public void entrenar(List<MuestraEntrenamiento> muestras) {
        bosque.clear();
        int n = muestras.size();
        int p = muestras.getFirst().caracteristicas().length;
        double[][] x = new double[n][];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = muestras.get(i).caracteristicas();
            y[i] = muestras.get(i).objetivo();
        }
        int caracteristicasPorDivision = Math.max(3, (int) Math.round(p / 2.0));
        for (int t = 0; t < ARBOLES; t++) {
            int[] bootstrap = new int[n];
            for (int i = 0; i < n; i++) bootstrap[i] = aleatorio.nextInt(n);
            bosque.add(crecer(x, y, bootstrap, 0, caracteristicasPorDivision));
        }
    }

    @Override
    public double predecir(double[] caracteristicas) {
        double suma = 0;
        for (Nodo arbol : bosque) suma += arbol.predecir(caracteristicas);
        return suma / bosque.size();
    }

    private Nodo crecer(double[][] x, double[] y, int[] indices, int profundidad, int caracteristicasPorDivision) {
        double media = 0;
        for (int i : indices) media += y[i];
        media /= indices.length;
        if (profundidad >= PROFUNDIDAD_MAXIMA || indices.length < 2 * MIN_HOJA) return Nodo.hoja(media);

        int p = x[0].length;
        int mejorCaracteristica = -1;
        double mejorUmbral = 0, mejorPuntaje = Double.MAX_VALUE;

        for (int f : caracteristicasAleatorias(p, caracteristicasPorDivision)) {
            double[] valores = new double[indices.length];
            for (int i = 0; i < indices.length; i++) valores[i] = x[indices[i]][f];
            Arrays.sort(valores);
            for (int q = 1; q < UMBRALES_CANDIDATOS; q++) {
                double umbral = valores[q * valores.length / UMBRALES_CANDIDATOS];
                double sumaIzq = 0, sumaDer = 0, cuadIzq = 0, cuadDer = 0;
                int nIzq = 0, nDer = 0;
                for (int i : indices) {
                    double v = y[i];
                    if (x[i][f] <= umbral) { sumaIzq += v; cuadIzq += v * v; nIzq++; }
                    else { sumaDer += v; cuadDer += v * v; nDer++; }
                }
                if (nIzq < MIN_HOJA || nDer < MIN_HOJA) continue;
                double puntaje = (cuadIzq - sumaIzq * sumaIzq / nIzq) + (cuadDer - sumaDer * sumaDer / nDer);
                if (puntaje < mejorPuntaje) {
                    mejorPuntaje = puntaje;
                    mejorCaracteristica = f;
                    mejorUmbral = umbral;
                }
            }
        }
        if (mejorCaracteristica < 0) return Nodo.hoja(media);

        int cantidadIzq = 0;
        for (int i : indices) if (x[i][mejorCaracteristica] <= mejorUmbral) cantidadIzq++;
        int[] izquierda = new int[cantidadIzq];
        int[] derecha = new int[indices.length - cantidadIzq];
        int l = 0, r = 0;
        for (int i : indices) {
            if (x[i][mejorCaracteristica] <= mejorUmbral) izquierda[l++] = i;
            else derecha[r++] = i;
        }
        return Nodo.division(mejorCaracteristica, mejorUmbral,
                crecer(x, y, izquierda, profundidad + 1, caracteristicasPorDivision),
                crecer(x, y, derecha, profundidad + 1, caracteristicasPorDivision));
    }

    private int[] caracteristicasAleatorias(int p, int cantidad) {
        int[] todas = new int[p];
        for (int i = 0; i < p; i++) todas[i] = i;
        for (int i = p - 1; i > 0; i--) {
            int j = aleatorio.nextInt(i + 1);
            int tmp = todas[i]; todas[i] = todas[j]; todas[j] = tmp;
        }
        return Arrays.copyOf(todas, cantidad);
    }

    private record Nodo(int caracteristica, double umbral, double valor, Nodo izquierdo, Nodo derecho) {

        static Nodo hoja(double valor) {
            return new Nodo(-1, 0, valor, null, null);
        }

        static Nodo division(int caracteristica, double umbral, Nodo izquierdo, Nodo derecho) {
            return new Nodo(caracteristica, umbral, 0, izquierdo, derecho);
        }

        double predecir(double[] f) {
            Nodo nodo = this;
            while (nodo.caracteristica >= 0) nodo = f[nodo.caracteristica] <= nodo.umbral ? nodo.izquierdo : nodo.derecho;
            return nodo.valor;
        }
    }
}
