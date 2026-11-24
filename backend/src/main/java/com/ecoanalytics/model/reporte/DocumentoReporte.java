package com.ecoanalytics.model.reporte;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Contenido de un reporte, independiente del formato. Todos los exportadores dibujan este mismo documento. */
public record DocumentoReporte(
        String reporteId,
        TipoReporte tipo,
        String titulo,
        String estacionNombre,
        String zona,
        LocalDate desde,
        LocalDate hasta,
        LocalDateTime fechaEmision,
        String emitidoPor,
        Double mediaPm25,
        Double porcentajeCumplimiento,
        int totalMediciones,
        List<FilaVariable> variables,
        List<FilaDiaria> diario,
        List<FilaPronostico> pronostico
) {
    public enum TipoReporte {
        RESUMEN_EJECUTIVO("Resumen ejecutivo de calidad del aire"),
        DETALLE_HISTORICO("Detalle histórico diario"),
        PRONOSTICO("Perspectiva y pronóstico 48 h");

        private final String nombre;

        TipoReporte(String nombre) {
            this.nombre = nombre;
        }

        public String nombre() {
            return nombre;
        }
    }

    public record FilaVariable(String variable, String unidad, Double media, Double minimo, Double maximo, String evaluacion) {
    }

    public record FilaDiaria(LocalDate fecha, Double mediaPm25, Double maxPm25, Double mediaPm10, Integer ica, String categoria) {
    }

    public record FilaPronostico(LocalDateTime fecha, double pm25, double inferior, double superior, int ica) {
    }
}
