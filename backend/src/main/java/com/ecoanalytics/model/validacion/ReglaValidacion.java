package com.ecoanalytics.model.validacion;

import java.util.List;
import java.util.Set;

/**
 * Una regla de negocio que debe cumplir una medición cruda. Las reglas son independientes y
 * combinables: agregar una regla nunca exige modificar las existentes (Polimorfismo).
 */
public interface ReglaValidacion {

    List<ViolacionRegla> verificar(RegistroMedicionCrudo registro, Contexto contexto);

    /** Si la regla debe ejecutarse según lo que ya encontraron las reglas anteriores. */
    default boolean aplicaDespuesDe(List<ViolacionRegla> anteriores) {
        return true;
    }

    /** Estado compartido entre los registros de un lote (estaciones conocidas, claves ya vistas). */
    record Contexto(Set<String> estacionesConocidas, Set<String> clavesVistas) {
    }
}
