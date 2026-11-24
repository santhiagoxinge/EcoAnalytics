package com.ecoanalytics.model.reporte;

/** Estrategia por formato de salida: PDF, CSV y Excel dibujan el mismo {@link DocumentoReporte}. */
public interface ExportadorReporte {

    enum Formato { PDF, CSV, EXCEL }

    Formato formato();

    String tipoContenido();

    String extension();

    byte[] exportar(DocumentoReporte documento);
}
