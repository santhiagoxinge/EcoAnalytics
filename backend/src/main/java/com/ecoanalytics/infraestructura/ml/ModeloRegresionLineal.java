package com.ecoanalytics.infraestructura.ml;

import com.ecoanalytics.model.prediccion.AlgoritmoModelo;
import com.ecoanalytics.model.prediccion.ModeloPredictivo;
import com.ecoanalytics.model.prediccion.MuestraEntrenamiento;

import java.util.List;

/**
 * Regresión lineal múltiple ajustada con las ecuaciones normales (X'X + λI) β = X'y.
 * Un pequeño término ridge mantiene el sistema bien condicionado cuando hay características correlacionadas.
 */
public class ModeloRegresionLineal implements ModeloPredictivo {

    private static final double RIDGE = 1e-3;

    private double[] coeficientes;

    @Override
    public AlgoritmoModelo algoritmo() {
        return AlgoritmoModelo.REGRESION_LINEAL;
    }

    @Override
    public void entrenar(List<MuestraEntrenamiento> muestras) {
        int p = muestras.getFirst().caracteristicas().length + 1;
        double[][] xtx = new double[p][p];
        double[] xty = new double[p];
        double[] fila = new double[p];

        for (MuestraEntrenamiento m : muestras) {
            fila[0] = 1;
            System.arraycopy(m.caracteristicas(), 0, fila, 1, p - 1);
            for (int i = 0; i < p; i++) {
                xty[i] += fila[i] * m.objetivo();
                for (int j = 0; j < p; j++) xtx[i][j] += fila[i] * fila[j];
            }
        }
        for (int i = 1; i < p; i++) xtx[i][i] += RIDGE;
        coeficientes = resolver(xtx, xty);
    }

    @Override
    public double predecir(double[] caracteristicas) {
        double y = coeficientes[0];
        for (int i = 0; i < caracteristicas.length; i++) y += coeficientes[i + 1] * caracteristicas[i];
        return Math.max(0, y);
    }

    /** Eliminación gaussiana con pivoteo parcial. */
    private static double[] resolver(double[][] a, double[] b) {
        int n = b.length;
        for (int col = 0; col < n; col++) {
            int pivote = col;
            for (int r = col + 1; r < n; r++) if (Math.abs(a[r][col]) > Math.abs(a[pivote][col])) pivote = r;
            double[] tmp = a[col]; a[col] = a[pivote]; a[pivote] = tmp;
            double t = b[col]; b[col] = b[pivote]; b[pivote] = t;

            for (int r = col + 1; r < n; r++) {
                double factor = a[r][col] / a[col][col];
                b[r] -= factor * b[col];
                for (int c = col; c < n; c++) a[r][c] -= factor * a[col][c];
            }
        }
        double[] x = new double[n];
        for (int r = n - 1; r >= 0; r--) {
            double suma = b[r];
            for (int c = r + 1; c < n; c++) suma -= a[r][c] * x[c];
            x[r] = suma / a[r][r];
        }
        return x;
    }
}
