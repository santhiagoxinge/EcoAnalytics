package com.ecoanalytics.model;

/**
 * Documento cuyo identificador es un número consecutivo (1, 2, 3...) en lugar del ObjectId de MongoDB.
 * El {@code GeneradorSecuencias} le asigna el id justo antes de guardarlo por primera vez.
 */
public interface ConIdSecuencial {

    Long getId();

    void asignarId(Long id);
}
