package com.ecoanalytics.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Parámetro general del sistema, identificado por su clave. */
@Document(collection = "configuracion_sistema")
public class ConfiguracionSistema {

    public static final String UMBRAL_Z_ANOMALIAS = "anomalias.umbral.z";
    public static final String ALERTAS_PREDICTIVAS_HABILITADAS = "alertas.predictivas.habilitadas";

    @Id
    private String clave;
    private String valor;

    protected ConfiguracionSistema() {
    }

    public ConfiguracionSistema(String clave, String valor) {
        this.clave = clave;
        this.valor = valor;
    }

    public void cambiar(String valor) {
        this.valor = valor;
    }

    public String getClave() { return clave; }
    public String getValor() { return valor; }
}
