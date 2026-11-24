package com.ecoanalytics.repository;

import com.ecoanalytics.model.ConIdSecuencial;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Inserta muchos documentos de una vez: reserva un bloque de ids con una sola consulta al contador
 * y luego envía un único insertMany. Lo usan la ingesta, la semilla y el simulador de sensores.
 */
@Component
public class InsercionMasiva {

    private final MongoTemplate mongo;
    private final GeneradorSecuencias secuencias;

    public InsercionMasiva(MongoTemplate mongo, GeneradorSecuencias secuencias) {
        this.mongo = mongo;
        this.secuencias = secuencias;
    }

    public <T extends ConIdSecuencial> List<T> insertar(List<T> documentos, Class<T> tipo) {
        if (documentos.isEmpty()) return documentos;
        String coleccion = mongo.getCollectionName(tipo);
        long id = secuencias.reservar(coleccion, documentos.size());
        for (T documento : documentos) documento.asignarId(id++);
        mongo.insert(documentos, tipo);
        return documentos;
    }
}
