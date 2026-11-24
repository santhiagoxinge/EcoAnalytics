package com.ecoanalytics.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Indicadores agregados (HU-10). Cada tipo sabe calcularse a partir de las mediciones del último mes
 * (Polimorfismo): agregar un indicador es agregar una constante, sin tocar el servicio que los recorre.
 */
public enum TipoIndicador {

    PROMEDIO_MOVIL_PM25_24H("Cada hora") {
        @Override
        public Double calcular(List<Medicion> mes, LocalDateTime ahora) {
            return Estadistica.media(pm25Desde(mes, ahora.minusHours(24)));
        }
    },
    DIAS_EXCEDENCIA_OMS("Diario 00:00") {
        @Override
        public Double calcular(List<Medicion> mes, LocalDateTime ahora) {
            LocalDate inicioMes = ahora.toLocalDate().withDayOfMonth(1);
            Map<LocalDate, List<Double>> porDia = mes.stream()
                    .filter(m -> !m.getFechaMedicion().toLocalDate().isBefore(inicioMes))
                    .collect(Collectors.groupingBy(m -> m.getFechaMedicion().toLocalDate(),
                            Collectors.mapping(Medicion::getPm25, Collectors.toList())));
            return (double) porDia.values().stream()
                    .filter(valores -> Estadistica.media(valores) > IndiceCalidadAire.LIMITE_DIARIO_OMS_PM25)
                    .count();
        }
    },
    VARIACION_SEMANAL("Semanal (lunes)") {
        @Override
        public Double calcular(List<Medicion> mes, LocalDateTime ahora) {
            Double actual = Estadistica.media(pm25Entre(mes, ahora.minusDays(7), ahora));
            Double anterior = Estadistica.media(pm25Entre(mes, ahora.minusDays(14), ahora.minusDays(7)));
            return Estadistica.variacionPorcentual(anterior, actual);
        }
    },
    MAXIMO_DIARIO("Cada hora") {
        @Override
        public Double calcular(List<Medicion> mes, LocalDateTime ahora) {
            return Estadistica.maximo(pm25Desde(mes, ahora.minusHours(24)));
        }
    };

    private final String programacion;

    TipoIndicador(String programacion) {
        this.programacion = programacion;
    }

    /** Valor del indicador, o null cuando no hay datos suficientes para calcularlo. */
    public abstract Double calcular(List<Medicion> mes, LocalDateTime ahora);

    public String programacion() {
        return programacion;
    }

    private static List<Double> pm25Desde(List<Medicion> datos, LocalDateTime desde) {
        return datos.stream().filter(m -> m.getFechaMedicion().isAfter(desde)).map(Medicion::getPm25).toList();
    }

    private static List<Double> pm25Entre(List<Medicion> datos, LocalDateTime desde, LocalDateTime hasta) {
        return datos.stream()
                .filter(m -> m.getFechaMedicion().isAfter(desde) && !m.getFechaMedicion().isAfter(hasta))
                .map(Medicion::getPm25).toList();
    }
}
