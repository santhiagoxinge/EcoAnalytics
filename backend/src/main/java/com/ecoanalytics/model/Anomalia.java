package com.ecoanalytics.model;

import com.ecoanalytics.exception.ExcepcionNegocio;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "anomalias")
public class Anomalia implements ConIdSecuencial {

    public enum Severidad { MODERADA, ALTA, CRITICA }

    public enum Estado { PENDIENTE, NOTIFICADA, DESCARTADA }

    @Id
    private Long id;
    private Long estacionId;
    private String estacionNombre;
    private LocalDateTime fechaMedicion;
    private VariableAmbiental variable;
    private double valorObservado;
    private double mediaEsperada;
    private double desviacionEstandar;
    private double puntajeZ;
    private Severidad severidad;
    private Estado estado;
    private LocalDateTime fechaDeteccion;

    protected Anomalia() {
    }

    public Anomalia(Estacion estacion, LocalDateTime fechaMedicion, VariableAmbiental variable,
                    double valorObservado, double mediaEsperada, double desviacionEstandar, double puntajeZ) {
        this.estacionId = estacion.getId();
        this.estacionNombre = estacion.getNombre();
        this.fechaMedicion = fechaMedicion;
        this.variable = variable;
        this.valorObservado = valorObservado;
        this.mediaEsperada = mediaEsperada;
        this.desviacionEstandar = desviacionEstandar;
        this.puntajeZ = puntajeZ;
        this.severidad = clasificar(puntajeZ);
        this.estado = Estado.PENDIENTE;
        this.fechaDeteccion = LocalDateTime.now();
    }

    private static Severidad clasificar(double puntajeZ) {
        if (puntajeZ >= 4.0) return Severidad.CRITICA;
        if (puntajeZ >= 3.5) return Severidad.ALTA;
        return Severidad.MODERADA;
    }

    /** Solo una anomalía pendiente puede gestionarse; la regla vive en la propia anomalía. */
    public void notificar() {
        verificarPendiente();
        this.estado = Estado.NOTIFICADA;
    }

    public void descartar() {
        verificarPendiente();
        this.estado = Estado.DESCARTADA;
    }

    private void verificarPendiente() {
        if (estado != Estado.PENDIENTE) {
            throw ExcepcionNegocio.conflicto("La anomalía ya fue gestionada.");
        }
    }

    @Override
    public void asignarId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() { return id; }
    public Long getEstacionId() { return estacionId; }
    public String getEstacionNombre() { return estacionNombre; }
    public LocalDateTime getFechaMedicion() { return fechaMedicion; }
    public VariableAmbiental getVariable() { return variable; }
    public double getValorObservado() { return valorObservado; }
    public double getMediaEsperada() { return mediaEsperada; }
    public double getDesviacionEstandar() { return desviacionEstandar; }
    public double getPuntajeZ() { return puntajeZ; }
    public Severidad getSeveridad() { return severidad; }
    public Estado getEstado() { return estado; }
    public LocalDateTime getFechaDeteccion() { return fechaDeteccion; }
}
