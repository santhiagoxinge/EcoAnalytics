package com.ecoanalytics.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "auditoria")
public class EventoAuditoria implements ConIdSecuencial {

    public enum Nivel { INFO, ADVERTENCIA, ERROR }

    @Id
    private Long id;
    private LocalDateTime fecha;
    private Nivel nivel;
    private String modulo;
    private String descripcion;
    private String actor;

    protected EventoAuditoria() {
    }

    public EventoAuditoria(Nivel nivel, String modulo, String descripcion, String actor) {
        this.fecha = LocalDateTime.now();
        this.nivel = nivel;
        this.modulo = modulo;
        this.descripcion = descripcion;
        this.actor = actor;
    }

    @Override
    public void asignarId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() { return id; }
    public LocalDateTime getFecha() { return fecha; }
    public Nivel getNivel() { return nivel; }
    public String getModulo() { return modulo; }
    public String getDescripcion() { return descripcion; }
    public String getActor() { return actor; }
}
