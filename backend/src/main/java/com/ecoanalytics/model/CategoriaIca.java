package com.ecoanalytics.model;

import java.util.Optional;

/**
 * Categorías del Índice de Calidad del Aire colombiano (Resolución 2254 de 2017).
 * Cada categoría conoce su nombre, su recomendación de salud y el nivel de alerta que le corresponde.
 */
public enum CategoriaIca {
    BUENA(50, "Buena"),
    ACEPTABLE(100, "Aceptable"),
    DANINA_SENSIBLES(150, "Dañina para grupos sensibles"),
    DANINA(200, "Dañina para la salud"),
    MUY_DANINA(300, "Muy dañina para la salud"),
    PELIGROSA(500, "Peligrosa");

    private final int indiceMaximo;
    private final String nombre;

    CategoriaIca(int indiceMaximo, String nombre) {
        this.indiceMaximo = indiceMaximo;
        this.nombre = nombre;
    }

    public static CategoriaIca desdeIndice(int indice) {
        for (CategoriaIca categoria : values()) {
            if (indice <= categoria.indiceMaximo) return categoria;
        }
        return PELIGROSA;
    }

    /** Nivel de alerta que corresponde a esta categoría, si es lo bastante grave para alertar. */
    public Optional<Alerta.Nivel> nivelAlerta() {
        return switch (this) {
            case BUENA, ACEPTABLE -> Optional.empty();
            case DANINA_SENSIBLES -> Optional.of(Alerta.Nivel.AMARILLA);
            case DANINA -> Optional.of(Alerta.Nivel.NARANJA);
            case MUY_DANINA -> Optional.of(Alerta.Nivel.ROJA);
            case PELIGROSA -> Optional.of(Alerta.Nivel.PURPURA);
        };
    }

    public String recomendacionSalud() {
        return switch (this) {
            case BUENA -> "La calidad del aire es satisfactoria. Es un buen momento para actividades al aire libre.";
            case ACEPTABLE -> "Calidad aceptable. Personas inusualmente sensibles deberían limitar esfuerzos prolongados al aire libre.";
            case DANINA_SENSIBLES -> "Niños, adultos mayores y personas con enfermedades respiratorias deben reducir la actividad física prolongada al aire libre.";
            case DANINA -> "Toda la población puede presentar efectos. Evite actividades físicas intensas al aire libre y use tapabocas si debe salir.";
            case MUY_DANINA -> "Alerta sanitaria: permanezca en espacios cerrados, mantenga ventanas cerradas y siga las indicaciones de las autoridades.";
            case PELIGROSA -> "Emergencia: evite toda actividad al aire libre. Atienda los protocolos de contingencia ambiental.";
        };
    }

    public String nombre() {
        return nombre;
    }
}
