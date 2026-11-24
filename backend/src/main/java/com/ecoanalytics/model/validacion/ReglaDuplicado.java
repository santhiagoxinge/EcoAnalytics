package com.ecoanalytics.model.validacion;

import com.ecoanalytics.model.IncidenciaValidacion.Resolucion;
import com.ecoanalytics.model.IncidenciaValidacion.Tipo;

import java.util.List;

/** Misma estación y misma hora dos veces: solo la primera aparición utilizable queda válida. */
public class ReglaDuplicado implements ReglaValidacion {

    /** Un registro ya descartado no debe reservar su hora, o escondería uno válido posterior. */
    @Override
    public boolean aplicaDespuesDe(List<ViolacionRegla> anteriores) {
        return anteriores.stream().noneMatch(ViolacionRegla::descartaRegistro);
    }

    @Override
    public List<ViolacionRegla> verificar(RegistroMedicionCrudo registro, Contexto contexto) {
        if (registro.estacionNombre() == null || registro.fecha() == null) return List.of();
        String clave = registro.estacionNombre().trim().toLowerCase() + "|" + registro.fecha();
        if (contexto.clavesVistas().add(clave)) return List.of();
        return List.of(new ViolacionRegla(Tipo.DUPLICADO, Resolucion.DESCARTADO, "Registro",
                registro.fecha().toString().replace('T', ' '), "Único por estación y hora"));
    }
}
