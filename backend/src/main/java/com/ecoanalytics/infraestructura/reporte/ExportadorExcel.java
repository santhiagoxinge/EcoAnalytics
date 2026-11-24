package com.ecoanalytics.infraestructura.reporte;

import com.ecoanalytics.model.reporte.DocumentoReporte;
import com.ecoanalytics.model.reporte.ExportadorReporte;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import static com.ecoanalytics.infraestructura.reporte.TextosReporte.*;

@Component
public class ExportadorExcel implements ExportadorReporte {

    @Override
    public Formato formato() {
        return Formato.EXCEL;
    }

    @Override
    public String tipoContenido() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    public String extension() {
        return "xlsx";
    }

    @Override
    public byte[] exportar(DocumentoReporte d) {
        try (Workbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            CellStyle encabezado = libro.createCellStyle();
            Font negrita = libro.createFont();
            negrita.setBold(true);
            encabezado.setFont(negrita);
            encabezado.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Sheet resumen = libro.createSheet("Resumen");
            int f = 0;
            f = par(resumen, f, "Reporte", d.reporteId(), encabezado);
            f = par(resumen, f, "Tipo", d.tipo().nombre(), encabezado);
            f = par(resumen, f, "Estación", d.estacionNombre() + " · " + d.zona(), encabezado);
            f = par(resumen, f, "Periodo", periodo(d), encabezado);
            f = par(resumen, f, "Emitido", FECHA_HORA.format(d.fechaEmision()) + " por " + d.emitidoPor(), encabezado);
            f = par(resumen, f, "Media PM2.5 (µg/m³)", numero(d.mediaPm25()), encabezado);
            f = par(resumen, f, "Cumplimiento guía OMS (%)", numero(d.porcentajeCumplimiento()), encabezado);
            f = par(resumen, f, "Mediciones", String.valueOf(d.totalMediciones()), encabezado);
            f++;
            f = fila(resumen, f, encabezado, "Variable", "Unidad", "Media", "Mínimo", "Máximo", "Evaluación");
            for (DocumentoReporte.FilaVariable v : d.variables()) {
                f = fila(resumen, f, null, v.variable(), v.unidad(), v.media(), v.minimo(), v.maximo(), v.evaluacion());
            }
            for (int c = 0; c < 6; c++) resumen.autoSizeColumn(c);

            if (!d.diario().isEmpty()) {
                Sheet diario = libro.createSheet("Diario");
                int fd = fila(diario, 0, encabezado, "Fecha", "Media PM2.5", "Máximo PM2.5", "Media PM10", "ICA", "Categoría");
                for (DocumentoReporte.FilaDiaria x : d.diario()) {
                    fd = fila(diario, fd, null, FECHA.format(x.fecha()), x.mediaPm25(), x.maxPm25(), x.mediaPm10(),
                            x.ica() == null ? null : x.ica().doubleValue(), x.categoria());
                }
                for (int c = 0; c < 6; c++) diario.autoSizeColumn(c);
            }
            if (!d.pronostico().isEmpty()) {
                Sheet pronostico = libro.createSheet("Pronóstico");
                int fp = fila(pronostico, 0, encabezado, "Hora", "PM2.5", "Límite inferior", "Límite superior", "ICA");
                for (DocumentoReporte.FilaPronostico x : d.pronostico()) {
                    fp = fila(pronostico, fp, null, FECHA_HORA.format(x.fecha()), x.pm25(), x.inferior(), x.superior(),
                            (double) x.ica());
                }
                for (int c = 0; c < 5; c++) pronostico.autoSizeColumn(c);
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static int par(Sheet hoja, int f, String etiqueta, String valor, CellStyle estiloEtiqueta) {
        Row row = hoja.createRow(f);
        Cell celda = row.createCell(0);
        celda.setCellValue(etiqueta);
        celda.setCellStyle(estiloEtiqueta);
        row.createCell(1).setCellValue(valor);
        return f + 1;
    }

    private static int fila(Sheet hoja, int f, CellStyle estilo, Object... valores) {
        Row row = hoja.createRow(f);
        for (int c = 0; c < valores.length; c++) {
            Cell celda = row.createCell(c);
            if (valores[c] instanceof Double numero) celda.setCellValue(numero);
            else if (valores[c] != null) celda.setCellValue(valores[c].toString());
            if (estilo != null) celda.setCellStyle(estilo);
        }
        return f + 1;
    }
}
