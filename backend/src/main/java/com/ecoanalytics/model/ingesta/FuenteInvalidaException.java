package com.ecoanalytics.model.ingesta;

/** El archivo cargado no tiene la estructura o el formato esperados. */
public class FuenteInvalidaException extends RuntimeException {

    public FuenteInvalidaException(String mensaje) {
        super(mensaje);
    }
}
