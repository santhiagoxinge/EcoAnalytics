package com.ecoanalytics.infraestructura.reporte;

import com.ecoanalytics.model.reporte.DocumentoReporte;
import com.ecoanalytics.model.reporte.ExportadorReporte;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import static com.ecoanalytics.infraestructura.reporte.TextosReporte.*;

@Component
public class ExportadorCsv implements ExportadorReporte {

    @Override
    public Formato formato() {
        return Formato.CSV;
    }

    @Override
    public String tipoContenido() {
        return "text/csv; charset=UTF-8";
    }

    @Override
    public String extension() {
        return "csv";
    }

    @Override
    public byte[] exportar(DocumentoReporte d) {
        // El BOM hace que Excel abra bien las tildes y la µ.
        StringBuilder csv = new StringBuilder("﻿");
        csv.append("Reporte,").append(d.reporteId()).append('\n')
                .append("Tipo,").append(d.tipo().nombre()).append('\n')
                .append("Estación,").append(d.estacionNombre()).append('\n')
                .append("Periodo,").append(periodo(d)).append('\n')
                .append("Emitido,").append(FECHA_HORA.format(d.fechaEmision())).append(" por ").append(d.emitidoPor()).append('\n')
                .append("Media PM2.5 (µg/m³),").append(numero(d.mediaPm25())).append('\n')
                .append("Cumplimiento guía OMS (%),").append(numero(d.porcentajeCumplimiento())).append('\n')
                .append("Mediciones,").append(d.totalMediciones()).append("\n\n")
                .append("Variable,Unidad,Media,Mínimo,Máximo,Evaluación\n");
        for (DocumentoReporte.FilaVariable v : d.variables()) {
            csv.append(v.variable()).append(',').append(v.unidad()).append(',').append(numero(v.media())).append(',')
                    .append(numero(v.minimo())).append(',').append(numero(v.maximo())).append(',')
                    .append(v.evaluacion()).append('\n');
        }
        if (!d.diario().isEmpty()) {
            csv.append("\nFecha,Media PM2.5,Máximo PM2.5,Media PM10,ICA,Categoría\n");
            for (DocumentoReporte.FilaDiaria f : d.diario()) {
                csv.append(FECHA.format(f.fecha())).append(',').append(numero(f.mediaPm25())).append(',')
                        .append(numero(f.maxPm25())).append(',').append(numero(f.mediaPm10())).append(',')
                        .append(f.ica() == null ? "" : f.ica()).append(',').append(f.categoria()).append('\n');
            }
        }
        if (!d.pronostico().isEmpty()) {
            csv.append("\nHora pronosticada,PM2.5,Límite inferior,Límite superior,ICA\n");
            for (DocumentoReporte.FilaPronostico p : d.pronostico()) {
                csv.append(FECHA_HORA.format(p.fecha())).append(',').append(p.pm25()).append(',')
                        .append(p.inferior()).append(',').append(p.superior()).append(',').append(p.ica()).append('\n');
            }
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
}
