package com.ecoanalytics.model.prediccion;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Convierte una serie horaria en características para los modelos. Lo comparten el entrenamiento
 * y el pronóstico, así ambos ven siempre la misma representación de los datos.
 */
public final class ConstructorCaracteristicas {

    public static final int HISTORIA_REQUERIDA = 24;
    public static final String DESCRIPCION =
            "PM2.5 (t-1, t-2, t-3, t-24), media 6 h, temperatura, humedad, hora del día, fin de semana";

    /** Una observación horaria; temperatura y humedad pueden ser estimaciones al pronosticar. */
    public record PuntoHorario(LocalDateTime fecha, double pm25, double temperatura, double humedad) {
    }

    private ConstructorCaracteristicas() {
    }

    /** Características para predecir el valor en {@code indice}, usando solo puntos anteriores. */
    public static double[] caracteristicas(List<PuntoHorario> serie, int indice, double temperatura, double humedad) {
        double rezago1 = serie.get(indice - 1).pm25();
        double rezago2 = serie.get(indice - 2).pm25();
        double rezago3 = serie.get(indice - 3).pm25();
        double rezago24 = serie.get(indice - 24).pm25();
        double media6 = 0;
        for (int i = indice - 6; i < indice; i++) media6 += serie.get(i).pm25();
        media6 /= 6;

        LocalDateTime fecha = serie.get(indice - 1).fecha().plusHours(1);
        double angulo = 2 * Math.PI * fecha.getHour() / 24.0;
        DayOfWeek dia = fecha.getDayOfWeek();
        double finDeSemana = dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY ? 1 : 0;

        return new double[]{rezago1, rezago2, rezago3, rezago24, media6, temperatura, humedad,
                Math.sin(angulo), Math.cos(angulo), finDeSemana};
    }

    /** Muestras solo donde las 24 horas previas son contiguas, para que los rezagos signifiquen lo que dicen. */
    public static List<MuestraEntrenamiento> muestras(List<PuntoHorario> serie) {
        List<MuestraEntrenamiento> muestras = new ArrayList<>();
        for (int i = HISTORIA_REQUERIDA; i < serie.size(); i++) {
            if (esObjetivoValido(serie, i)) {
                PuntoHorario objetivo = serie.get(i);
                muestras.add(new MuestraEntrenamiento(
                        caracteristicas(serie, i, objetivo.temperatura(), objetivo.humedad()), objetivo.pm25()));
            }
        }
        return muestras;
    }

    /** Puntos que se convirtieron en objetivo en {@link #muestras}, en el mismo orden. */
    public static List<PuntoHorario> puntosObjetivo(List<PuntoHorario> serie) {
        List<PuntoHorario> objetivos = new ArrayList<>();
        for (int i = HISTORIA_REQUERIDA; i < serie.size(); i++) {
            if (esObjetivoValido(serie, i)) objetivos.add(serie.get(i));
        }
        return objetivos;
    }

    private static boolean esObjetivoValido(List<PuntoHorario> serie, int i) {
        return serie.get(i - HISTORIA_REQUERIDA).fecha().plusHours(HISTORIA_REQUERIDA).equals(serie.get(i).fecha());
    }
}
