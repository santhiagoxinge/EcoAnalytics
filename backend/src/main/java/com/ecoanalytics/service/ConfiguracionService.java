package com.ecoanalytics.service;

import com.ecoanalytics.model.ConfiguracionSistema;
import com.ecoanalytics.repository.ConfiguracionSistemaRepository;
import org.springframework.stereotype.Service;

/** Lectura y cambio de los parámetros generales del sistema. */
@Service
public class ConfiguracionService {

    private final ConfiguracionSistemaRepository repositorio;

    public ConfiguracionService(ConfiguracionSistemaRepository repositorio) {
        this.repositorio = repositorio;
    }

    public double umbralAnomalias() {
        return repositorio.findById(ConfiguracionSistema.UMBRAL_Z_ANOMALIAS)
                .map(c -> Double.parseDouble(c.getValor()))
                .orElse(3.0);
    }

    public boolean alertasPredictivasHabilitadas() {
        return repositorio.findById(ConfiguracionSistema.ALERTAS_PREDICTIVAS_HABILITADAS)
                .map(c -> Boolean.parseBoolean(c.getValor()))
                .orElse(true);
    }

    public void cambiar(String clave, String valor) {
        ConfiguracionSistema configuracion = repositorio.findById(clave)
                .orElseGet(() -> new ConfiguracionSistema(clave, valor));
        configuracion.cambiar(valor);
        repositorio.save(configuracion);
    }
}
