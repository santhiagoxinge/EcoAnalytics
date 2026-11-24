package com.ecoanalytics.infraestructura.ingesta;

import com.ecoanalytics.model.ingesta.AdaptadorFuenteMediciones;
import com.ecoanalytics.model.ingesta.FuenteInvalidaException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Elige el adaptador capaz de leer un archivo. Un formato nuevo se admite agregando otro
 * {@link AdaptadorFuenteMediciones}; esta clase no cambia (Variaciones Protegidas).
 */
@Component
public class FabricaFuentesMediciones {

    private final List<AdaptadorFuenteMediciones> adaptadores;

    public FabricaFuentesMediciones(List<AdaptadorFuenteMediciones> adaptadores) {
        this.adaptadores = adaptadores;
    }

    public AdaptadorFuenteMediciones paraArchivo(String nombreArchivo) {
        return adaptadores.stream()
                .filter(a -> a.admite(nombreArchivo))
                .findFirst()
                .orElseThrow(() -> new FuenteInvalidaException(
                        "Formato no compatible. Únicamente se admiten archivos .csv o .json"));
    }
}
