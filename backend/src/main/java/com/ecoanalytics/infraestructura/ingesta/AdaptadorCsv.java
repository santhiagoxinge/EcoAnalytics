package com.ecoanalytics.infraestructura.ingesta;

import com.ecoanalytics.model.TipoFuente;
import com.ecoanalytics.model.ingesta.AdaptadorFuenteMediciones;
import com.ecoanalytics.model.ingesta.FuenteInvalidaException;
import com.ecoanalytics.model.validacion.RegistroMedicionCrudo;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Lee archivos CSV separados por coma o punto y coma (se detecta el separador en la cabecera). */
@Component
public class AdaptadorCsv implements AdaptadorFuenteMediciones {

    @Override
    public TipoFuente tipo() {
        return TipoFuente.CSV;
    }

    @Override
    public boolean admite(String nombreArchivo) {
        return nombreArchivo.toLowerCase().endsWith(".csv");
    }

    @Override
    public FuenteLeida leer(InputStream entrada) {
        try {
            BufferedReader lector = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
            lector.mark(4096);
            String cabecera = lector.readLine();
            if (cabecera == null) throw new FuenteInvalidaException("El archivo está vacío.");
            lector.reset();
            long puntoYComa = cabecera.chars().filter(c -> c == ';').count();
            long comas = cabecera.chars().filter(c -> c == ',').count();
            char separador = puntoYComa > comas ? ';' : ',';

            CSVFormat formato = CSVFormat.DEFAULT.builder()
                    .setDelimiter(separador)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .get();
            try (CSVParser parser = CSVParser.parse(lector, formato)) {
                List<String> columnas = parser.getHeaderNames().stream().map(c -> c.replace("﻿", "")).toList();
                MapeoColumnas mapeo = MapeoColumnas.resolver(columnas);

                List<RegistroMedicionCrudo> registros = new ArrayList<>();
                int fila = 1;
                for (CSVRecord registroCsv : parser) {
                    Map<String, String> valores = new HashMap<>();
                    for (int i = 0; i < columnas.size(); i++) {
                        valores.put(columnas.get(i), i < registroCsv.size() ? registroCsv.get(i) : null);
                    }
                    registros.add(mapeo.aRegistro(fila++, valores));
                }
                return new FuenteLeida(columnas, registros);
            }
        } catch (IOException | UncheckedIOException e) {
            throw new FuenteInvalidaException("No fue posible leer el archivo CSV: " + e.getMessage());
        }
    }
}
