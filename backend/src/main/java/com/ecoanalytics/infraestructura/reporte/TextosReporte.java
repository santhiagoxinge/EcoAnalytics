package com.ecoanalytics.infraestructura.reporte;

import com.ecoanalytics.model.reporte.DocumentoReporte;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatos compartidos por todos los exportadores, para que los tres formatos se lean igual. */
final class TextosReporte {

    static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private TextosReporte() {
    }

    static String numero(Double valor) {
        return valor == null ? "—" : String.format(Locale.ROOT, "%.1f", valor);
    }

    static String periodo(DocumentoReporte d) {
        return FECHA.format(d.desde()) + " – " + FECHA.format(d.hasta());
    }
}
