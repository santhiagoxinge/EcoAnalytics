package com.ecoanalytics.infraestructura.ml;

import com.ecoanalytics.model.prediccion.ModeloPredictivo;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda en memoria los modelos ajustados, por versión. Los metadatos se persisten aparte
 * (VersionModelo), así que si falta una versión tras un reinicio simplemente se reentrena.
 */
@Component
public class RegistroModelos {

    private final Map<String, ModeloPredictivo> ajustados = new ConcurrentHashMap<>();

    public void registrar(String version, ModeloPredictivo modelo) {
        ajustados.put(version, modelo);
    }

    public Optional<ModeloPredictivo> buscar(String version) {
        return Optional.ofNullable(ajustados.get(version));
    }
}
