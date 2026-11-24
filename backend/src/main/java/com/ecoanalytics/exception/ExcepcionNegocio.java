package com.ecoanalytics.exception;

/**
 * Una regla del negocio no se cumplió. El mensaje se muestra tal cual al usuario.
 * <p>
 * No depende de Spring: el modelo de dominio puede lanzarla sin conocer HTTP. Es el
 * {@link ManejadorExcepciones} quien traduce cada {@link Tipo} a un código de estado (Bajo Acoplamiento).
 */
public class ExcepcionNegocio extends RuntimeException {

    public enum Tipo { NO_ENCONTRADO, INVALIDO, CONFLICTO, NO_AUTORIZADO, PROHIBIDO }

    private final Tipo tipo;

    public ExcepcionNegocio(Tipo tipo, String mensaje) {
        super(mensaje);
        this.tipo = tipo;
    }

    public static ExcepcionNegocio noEncontrado(String mensaje) {
        return new ExcepcionNegocio(Tipo.NO_ENCONTRADO, mensaje);
    }

    public static ExcepcionNegocio invalido(String mensaje) {
        return new ExcepcionNegocio(Tipo.INVALIDO, mensaje);
    }

    public static ExcepcionNegocio conflicto(String mensaje) {
        return new ExcepcionNegocio(Tipo.CONFLICTO, mensaje);
    }

    public static ExcepcionNegocio noAutorizado(String mensaje) {
        return new ExcepcionNegocio(Tipo.NO_AUTORIZADO, mensaje);
    }

    public static ExcepcionNegocio prohibido(String mensaje) {
        return new ExcepcionNegocio(Tipo.PROHIBIDO, mensaje);
    }

    public Tipo tipo() {
        return tipo;
    }
}
