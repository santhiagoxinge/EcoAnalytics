package com.ecoanalytics.model.validacion;

import com.ecoanalytics.model.IncidenciaValidacion;

public record ViolacionRegla(
        IncidenciaValidacion.Tipo tipo,
        IncidenciaValidacion.Resolucion resolucion,
        String variable,
        String valorDetectado,
        String rangoAceptable
) {
    public boolean descartaRegistro() {
        return resolucion == IncidenciaValidacion.Resolucion.DESCARTADO;
    }
}
