package com.ecoanalytics.infraestructura.reporte;

import com.ecoanalytics.model.reporte.DocumentoReporte;
import com.ecoanalytics.model.reporte.ExportadorReporte;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

import static com.ecoanalytics.infraestructura.reporte.TextosReporte.*;

@Component
public class ExportadorPdf implements ExportadorReporte {

    private static final Color TINTA = new Color(29, 29, 31);
    private static final Color TENUE = new Color(110, 110, 115);
    private static final Color ACENTO = new Color(36, 138, 61);
    private static final Color LINEA = new Color(229, 229, 234);
    private static final Color FONDO_ENCABEZADO = new Color(245, 245, 247);

    @Override
    public Formato formato() {
        return Formato.PDF;
    }

    @Override
    public String tipoContenido() {
        return "application/pdf";
    }

    @Override
    public String extension() {
        return "pdf";
    }

    @Override
    public byte[] exportar(DocumentoReporte d) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document pdf = new Document(PageSize.A4, 48, 48, 48, 48);
        PdfWriter.getInstance(pdf, salida);
        pdf.addTitle(d.titulo());
        pdf.addAuthor("EcoAnalytics");
        pdf.open();

        Font marca = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, ACENTO);
        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, TINTA);
        Font meta = FontFactory.getFont(FontFactory.HELVETICA, 9, TENUE);
        Font seccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, TINTA);
        Font valorIndicador = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, TINTA);

        pdf.add(new Paragraph("EcoAnalytics · Sistema de análisis y monitoreo ambiental", marca));
        Paragraph encabezado = new Paragraph(d.tipo().nombre(), titulo);
        encabezado.setSpacingBefore(6);
        pdf.add(encabezado);
        pdf.add(new Paragraph(d.estacionNombre() + " · " + d.zona() + "  |  Periodo " + periodo(d), meta));
        pdf.add(new Paragraph("Reporte " + d.reporteId() + "  |  Emitido " + FECHA_HORA.format(d.fechaEmision())
                + " por " + d.emitidoPor(), meta));

        PdfPTable indicadores = new PdfPTable(3);
        indicadores.setWidthPercentage(100);
        indicadores.setSpacingBefore(18);
        indicadores.addCell(indicador("Media PM2.5", numero(d.mediaPm25()) + " µg/m³", meta, valorIndicador));
        indicadores.addCell(indicador("Cumplimiento guía OMS", numero(d.porcentajeCumplimiento()) + " %", meta, valorIndicador));
        indicadores.addCell(indicador("Mediciones analizadas", String.format("%,d", d.totalMediciones()), meta, valorIndicador));
        pdf.add(indicadores);

        tituloSeccion(pdf, "Resumen por variable", seccion);
        PdfPTable variables = tabla(new float[]{3, 1.4f, 1.3f, 1.3f, 1.3f, 3}, "Variable", "Unidad", "Media", "Mín.", "Máx.", "Evaluación");
        for (DocumentoReporte.FilaVariable v : d.variables()) {
            celdas(variables, v.variable(), v.unidad(), numero(v.media()), numero(v.minimo()), numero(v.maximo()), v.evaluacion());
        }
        pdf.add(variables);

        if (!d.diario().isEmpty()) {
            tituloSeccion(pdf, "Comportamiento diario", seccion);
            PdfPTable diario = tabla(new float[]{2, 1.6f, 1.6f, 1.6f, 1, 3}, "Fecha", "Media PM2.5", "Máx. PM2.5", "Media PM10", "ICA", "Categoría");
            for (DocumentoReporte.FilaDiaria f : d.diario()) {
                celdas(diario, FECHA.format(f.fecha()), numero(f.mediaPm25()), numero(f.maxPm25()),
                        numero(f.mediaPm10()), f.ica() == null ? "—" : f.ica().toString(), f.categoria());
            }
            pdf.add(diario);
        }

        if (!d.pronostico().isEmpty()) {
            tituloSeccion(pdf, "Pronóstico PM2.5 (intervalo de confianza 95 %)", seccion);
            PdfPTable pronostico = tabla(new float[]{2.4f, 1.4f, 1.6f, 1.6f, 1}, "Hora", "PM2.5", "Lím. inferior", "Lím. superior", "ICA");
            for (int i = 0; i < d.pronostico().size(); i += 3) {
                DocumentoReporte.FilaPronostico p = d.pronostico().get(i);
                celdas(pronostico, FECHA_HORA.format(p.fecha()), numero(p.pm25()), numero(p.inferior()),
                        numero(p.superior()), String.valueOf(p.ica()));
            }
            pdf.add(pronostico);
        }

        Paragraph nota = new Paragraph("Información de apoyo al análisis. EcoAnalytics no reemplaza a la autoridad ambiental; "
                + "las predicciones incluyen incertidumbre y no constituyen certezas.", meta);
        nota.setSpacingBefore(24);
        pdf.add(nota);
        pdf.close();
        return salida.toByteArray();
    }

    private static PdfPCell indicador(String etiqueta, String valor, Font fuenteEtiqueta, Font fuenteValor) {
        PdfPCell celda = new PdfPCell();
        celda.setBorderColor(LINEA);
        celda.setPadding(12);
        celda.addElement(new Paragraph(etiqueta, fuenteEtiqueta));
        celda.addElement(new Paragraph(valor, fuenteValor));
        return celda;
    }

    private static void tituloSeccion(Document pdf, String texto, Font fuente) {
        Paragraph p = new Paragraph(texto, fuente);
        p.setSpacingBefore(22);
        p.setSpacingAfter(8);
        pdf.add(p);
    }

    private static PdfPTable tabla(float[] anchos, String... encabezados) {
        PdfPTable tabla = new PdfPTable(anchos);
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        Font fuente = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, TINTA);
        for (String e : encabezados) {
            PdfPCell celda = new PdfPCell(new Phrase(e, fuente));
            celda.setBackgroundColor(FONDO_ENCABEZADO);
            celda.setBorderColor(LINEA);
            celda.setPadding(6);
            tabla.addCell(celda);
        }
        return tabla;
    }

    private static void celdas(PdfPTable tabla, String... valores) {
        Font fuente = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, TINTA);
        for (String v : valores) {
            PdfPCell celda = new PdfPCell(new Phrase(v, fuente));
            celda.setBorderColor(LINEA);
            celda.setPadding(5);
            tabla.addCell(celda);
        }
    }
}
