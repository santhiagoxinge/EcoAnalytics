package com.ecoanalytics.model.evento;

import java.util.List;

/**
 * Se publica cada vez que se guardan mediciones nuevas. Los observadores (detección de anomalías,
 * alertas) reaccionan sin que la ingesta sepa que existen (Indirección / patrón Observer).
 */
public record MedicionesRegistradasEvento(List<Long> idsMediciones) {
}
