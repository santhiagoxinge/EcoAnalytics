package com.ecoanalytics.repository;

import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.inc;

/**
 * Entrega identificadores consecutivos por colección usando la colección auxiliar "secuencias".
 * <p>
 * Fabricación Pura (GRASP): no es un concepto del dominio, existe para que las entidades conserven
 * ids numéricos legibles (1, 2, 3...) sobre MongoDB sin que ninguna de ellas sepa cómo se generan.
 * El incremento es atómico ($inc con findOneAndUpdate), así que dos peticiones nunca reciben el mismo id.
 */
@Component
public class GeneradorSecuencias {

    private static final String COLECCION = "secuencias";

    private final MongoTemplate mongo;

    public GeneradorSecuencias(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public long siguiente(String secuencia) {
        return reservar(secuencia, 1);
    }

    /**
     * Reserva un bloque de {@code cantidad} ids y devuelve el primero. Permite insertar miles de
     * documentos con una sola consulta al contador.
     */
    public long reservar(String secuencia, int cantidad) {
        Document contador = mongo.getCollection(COLECCION).findOneAndUpdate(
                eq("_id", secuencia), inc("valor", (long) cantidad),
                new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
        long ultimo = ((Number) contador.get("valor")).longValue();
        return ultimo - cantidad + 1;
    }
}
