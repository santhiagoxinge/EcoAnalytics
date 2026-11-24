package com.ecoanalytics.model.evento;

import com.ecoanalytics.model.prediccion.Pronostico;

/** Se publica al calcular un pronóstico nuevo; el servicio de alertas evalúa si amerita una alerta predictiva. */
public record PronosticoGeneradoEvento(Pronostico pronostico) {
}
