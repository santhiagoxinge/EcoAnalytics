package com.ecoanalytics.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/** Estado de un indicador: si se calcula y cuándo fue su última ejecución. Su id es el nombre del tipo. */
@Document(collection = "configuracion_indicadores")
public class ConfiguracionIndicador {

    @Id
    private String id;
    private TipoIndicador tipo;
    private boolean habilitado;
    private String programacion;
    private LocalDateTime ultimaEjecucion;
    private int registrosProcesados;

    protected ConfiguracionIndicador() {
    }

    public ConfiguracionIndicador(TipoIndicador tipo) {
        this.id = tipo.name();
        this.tipo = tipo;
        this.programacion = tipo.programacion();
        this.habilitado = true;
    }

    public void cambiarHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }

    public void registrarEjecucion(int registrosProcesados) {
        this.registrosProcesados = registrosProcesados;
        this.ultimaEjecucion = LocalDateTime.now();
    }

    public String getId() { return id; }
    public TipoIndicador getTipo() { return tipo; }
    public boolean isHabilitado() { return habilitado; }
    public String getProgramacion() { return programacion; }
    public LocalDateTime getUltimaEjecucion() { return ultimaEjecucion; }
    public int getRegistrosProcesados() { return registrosProcesados; }
}
