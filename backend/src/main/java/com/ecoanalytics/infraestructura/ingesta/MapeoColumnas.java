package com.ecoanalytics.infraestructura.ingesta;

import com.ecoanalytics.model.ingesta.FuenteInvalidaException;
import com.ecoanalytics.model.validacion.RegistroMedicionCrudo;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Normaliza los distintos nombres que usan las fuentes para una misma columna ("temp", "TEMP_AIRE",
 * "Temperatura"...) en los campos canónicos de una medición.
 */
final class MapeoColumnas {

    enum Campo {
        ESTACION("Estación"), FECHA("Fecha"), HORA("Hora"), PM25("PM2.5"),
        PM10("PM10"), TEMPERATURA("Temperatura"), HUMEDAD("Humedad");

        final String etiqueta;

        Campo(String etiqueta) {
            this.etiqueta = etiqueta;
        }
    }

    private static final Map<String, Campo> ALIAS = new HashMap<>();

    static {
        alias(Campo.ESTACION, "estacion", "station", "estacionmonitoreo", "fuente");
        alias(Campo.FECHA, "fecha", "date", "dia");
        alias(Campo.HORA, "hora", "time", "hour");
        alias(Campo.PM25, "pm25", "pm25ugm3");
        alias(Campo.PM10, "pm10", "pm10ugm3");
        alias(Campo.TEMPERATURA, "temperatura", "temp", "tempaire", "temperature");
        alias(Campo.HUMEDAD, "humedad", "humidity", "hum", "humedadrelativa", "hr");
    }

    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("d/M/yyyy"), DateTimeFormatter.ofPattern("d-M-yyyy"));
    private static final List<DateTimeFormatter> FORMATOS_HORA = List.of(
            DateTimeFormatter.ofPattern("H:mm"), DateTimeFormatter.ofPattern("H:mm:ss"));

    private final Map<Campo, String> columnasFuente;

    private MapeoColumnas(Map<Campo, String> columnasFuente) {
        this.columnasFuente = columnasFuente;
    }

    private static void alias(Campo campo, String... nombres) {
        for (String n : nombres) ALIAS.put(n, campo);
    }

    static String normalizar(String columna) {
        String sinTildes = Normalizer.normalize(columna, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** Construye el mapeo y falla indicando exactamente qué columna le falta a la fuente. */
    static MapeoColumnas resolver(Collection<String> columnas) {
        Map<Campo, String> encontradas = new EnumMap<>(Campo.class);
        for (String columna : columnas) {
            Campo campo = ALIAS.get(normalizar(columna));
            if (campo != null) encontradas.putIfAbsent(campo, columna);
        }
        for (Campo obligatorio : Campo.values()) {
            if (!encontradas.containsKey(obligatorio)) {
                throw new FuenteInvalidaException("Estructura no válida: La columna obligatoria " + obligatorio.etiqueta
                        + " no fue encontrada en el archivo");
            }
        }
        return new MapeoColumnas(encontradas);
    }

    RegistroMedicionCrudo aRegistro(int fila, Map<String, String> valores) {
        String fecha = valor(valores, Campo.FECHA);
        String hora = valor(valores, Campo.HORA);
        String fechaOriginal = fecha == null || hora == null ? null : fecha + " " + hora;
        return new RegistroMedicionCrudo(fila, valor(valores, Campo.ESTACION), interpretarFecha(fecha, hora),
                fechaOriginal, numero(valores, Campo.PM25), numero(valores, Campo.PM10),
                numero(valores, Campo.TEMPERATURA), numero(valores, Campo.HUMEDAD));
    }

    private String valor(Map<String, String> valores, Campo campo) {
        String v = valores.get(columnasFuente.get(campo));
        return v == null || v.isBlank() ? null : v.trim();
    }

    private Double numero(Map<String, String> valores, Campo campo) {
        String v = valor(valores, campo);
        if (v == null) return null;
        try {
            return Double.parseDouble(v.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDateTime interpretarFecha(String fecha, String hora) {
        if (fecha == null || hora == null) return null;
        LocalDate dia = null;
        for (DateTimeFormatter f : FORMATOS_FECHA) {
            try {
                dia = LocalDate.parse(fecha, f);
                break;
            } catch (DateTimeParseException ignorada) {
                // se prueba el siguiente formato
            }
        }
        if (dia == null) return null;
        for (DateTimeFormatter f : FORMATOS_HORA) {
            try {
                return LocalDateTime.of(dia, LocalTime.parse(hora, f));
            } catch (DateTimeParseException ignorada) {
                // se prueba el siguiente formato
            }
        }
        return null;
    }
}
