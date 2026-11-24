package com.ecoanalytics.model.ingesta;

import com.ecoanalytics.model.TipoFuente;
import com.ecoanalytics.model.validacion.RegistroMedicionCrudo;

import java.io.InputStream;
import java.util.List;

/**
 * Contrato común de toda fuente de datos (caso de estudio, sección 7.3). Cada adaptador traduce su
 * formato y sus nombres de columna a {@link RegistroMedicionCrudo}; el resto del sistema no nota la diferencia.
 */
public interface AdaptadorFuenteMediciones {

    TipoFuente tipo();

    boolean admite(String nombreArchivo);

    FuenteLeida leer(InputStream entrada);

    record FuenteLeida(List<String> columnas, List<RegistroMedicionCrudo> registros) {
    }
}
