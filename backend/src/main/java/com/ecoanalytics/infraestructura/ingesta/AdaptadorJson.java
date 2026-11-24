package com.ecoanalytics.infraestructura.ingesta;

import com.ecoanalytics.model.TipoFuente;
import com.ecoanalytics.model.ingesta.AdaptadorFuenteMediciones;
import com.ecoanalytics.model.ingesta.FuenteInvalidaException;
import com.ecoanalytics.model.validacion.RegistroMedicionCrudo;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.*;

/** Acepta una lista JSON de objetos, o un objeto con una lista "mediciones" (o "measurements"). */
@Component
public class AdaptadorJson implements AdaptadorFuenteMediciones {

    private final ObjectMapper mapper;

    public AdaptadorJson(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TipoFuente tipo() {
        return TipoFuente.JSON;
    }

    @Override
    public boolean admite(String nombreArchivo) {
        return nombreArchivo.toLowerCase().endsWith(".json");
    }

    @Override
    @SuppressWarnings("unchecked")
    public FuenteLeida leer(InputStream entrada) {
        Object raiz;
        try {
            raiz = mapper.readValue(entrada, new TypeReference<Object>() {
            });
        } catch (JacksonException e) {
            throw new FuenteInvalidaException("El archivo JSON no tiene un formato válido.");
        }
        if (raiz instanceof Map<?, ?> mapa) {
            Object mediciones = mapa.get("mediciones");
            raiz = mediciones != null ? mediciones : mapa.get("measurements");
        }
        if (!(raiz instanceof List<?> elementos) || elementos.isEmpty()) {
            throw new FuenteInvalidaException("El archivo JSON debe contener una lista de mediciones.");
        }

        LinkedHashSet<String> columnas = new LinkedHashSet<>();
        elementos.forEach(e -> {
            if (e instanceof Map<?, ?> m) m.keySet().forEach(k -> columnas.add(String.valueOf(k)));
        });
        MapeoColumnas mapeo = MapeoColumnas.resolver(columnas);

        List<RegistroMedicionCrudo> registros = new ArrayList<>();
        int fila = 1;
        for (Object elemento : elementos) {
            Map<String, String> valores = new HashMap<>();
            if (elemento instanceof Map<?, ?> m) {
                ((Map<Object, Object>) m).forEach((k, v) -> valores.put(String.valueOf(k), v == null ? null : String.valueOf(v)));
            }
            registros.add(mapeo.aRegistro(fila++, valores));
        }
        return new FuenteLeida(List.copyOf(columnas), registros);
    }
}
