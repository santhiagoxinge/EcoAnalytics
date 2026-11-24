package com.ecoanalytics.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/** Canal de difusión de alertas. Su identificador es el propio código (PUSH, CORREO, BANNER_WEB). */
@Document(collection = "canales_alerta")
public class CanalAlerta {

    public enum Codigo { PUSH, CORREO, BANNER_WEB }

    @Id
    private String id;
    private Codigo codigo;
    private boolean habilitado;
    private int entregas;
    private LocalDateTime ultimaEntrega;

    protected CanalAlerta() {
    }

    public CanalAlerta(Codigo codigo, boolean habilitado) {
        this.id = codigo.name();
        this.codigo = codigo;
        this.habilitado = habilitado;
    }

    public void cambiarHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }

    public void registrarEntrega() {
        this.entregas++;
        this.ultimaEntrega = LocalDateTime.now();
    }

    public String getId() { return id; }
    public Codigo getCodigo() { return codigo; }
    public boolean isHabilitado() { return habilitado; }
    public int getEntregas() { return entregas; }
    public LocalDateTime getUltimaEntrega() { return ultimaEntrega; }
}
