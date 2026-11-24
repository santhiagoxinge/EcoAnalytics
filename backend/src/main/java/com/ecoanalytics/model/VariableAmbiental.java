package com.ecoanalytics.model;

/**
 * Variables que miden las estaciones, con el rango físicamente aceptable que usa la validación.
 */
public enum VariableAmbiental {
    PM25("PM2.5", "µg/m³", 0, 500),
    PM10("PM10", "µg/m³", 0, 600),
    TEMPERATURA("Temperatura", "°C", -10, 50),
    HUMEDAD("Humedad", "%", 0, 100);

    private final String etiqueta;
    private final String unidad;
    private final double minimo;
    private final double maximo;

    VariableAmbiental(String etiqueta, String unidad, double minimo, double maximo) {
        this.etiqueta = etiqueta;
        this.unidad = unidad;
        this.minimo = minimo;
        this.maximo = maximo;
    }

    public boolean estaEnRango(double valor) {
        return valor >= minimo && valor <= maximo;
    }

    public String descripcionRango() {
        return "%d – %d %s".formatted((long) minimo, (long) maximo, unidad);
    }

    public String etiqueta() { return etiqueta; }
    public String unidad() { return unidad; }
}
