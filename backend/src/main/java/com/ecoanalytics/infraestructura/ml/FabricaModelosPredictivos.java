package com.ecoanalytics.infraestructura.ml;

import com.ecoanalytics.model.prediccion.AlgoritmoModelo;
import com.ecoanalytics.model.prediccion.ModeloPredictivo;
import org.springframework.stereotype.Component;

/**
 * Factory Method: el único lugar que conoce las clases concretas de los modelos (Creador).
 * El servicio de entrenamiento pide un {@link ModeloPredictivo} sin acoplarse a ninguna implementación.
 */
@Component
public class FabricaModelosPredictivos {

    public ModeloPredictivo crear(AlgoritmoModelo algoritmo) {
        return switch (algoritmo) {
            case MEDIA_MOVIL -> new ModeloMediaMovil();
            case REGRESION_LINEAL -> new ModeloRegresionLineal();
            case K_VECINOS -> new ModeloKVecinos();
            case RANDOM_FOREST -> new ModeloRandomForest();
        };
    }
}
