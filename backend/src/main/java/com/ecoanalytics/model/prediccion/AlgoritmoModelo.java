package com.ecoanalytics.model.prediccion;

public enum AlgoritmoModelo {
    MEDIA_MOVIL("Media móvil estacional"),
    REGRESION_LINEAL("Regresión lineal múltiple"),
    K_VECINOS("K vecinos más cercanos"),
    RANDOM_FOREST("Random Forest Regressor");

    private final String nombre;

    AlgoritmoModelo(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}
