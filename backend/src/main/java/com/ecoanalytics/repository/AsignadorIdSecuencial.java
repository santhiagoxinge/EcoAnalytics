package com.ecoanalytics.repository;

import com.ecoanalytics.model.ConIdSecuencial;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

/**
 * Antes de guardar un documento nuevo le asigna el siguiente id de su colección (Indirección):
 * los servicios llaman a {@code save()} como siempre y no conocen el {@link GeneradorSecuencias}.
 */
@Component
public class AsignadorIdSecuencial implements BeforeConvertCallback<Object> {

    /**
     * Se resuelve de forma diferida: el generador depende de MongoTemplate, y MongoTemplate
     * registra este callback al construirse; inyectarlo directo crearía un ciclo de beans.
     */
    private final ObjectProvider<GeneradorSecuencias> secuencias;

    public AsignadorIdSecuencial(ObjectProvider<GeneradorSecuencias> secuencias) {
        this.secuencias = secuencias;
    }

    @Override
    public Object onBeforeConvert(Object entidad, String coleccion) {
        if (entidad instanceof ConIdSecuencial documento && documento.getId() == null) {
            documento.asignarId(secuencias.getObject().siguiente(coleccion));
        }
        return entidad;
    }
}
