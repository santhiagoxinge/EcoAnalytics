package com.ecoanalytics.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Alerta emitida a la ciudadanía. Guarda una copia del nombre de la estación (desnormalización
 * propia de MongoDB) para que el historial se lea sin consultar otra colección.
 */
@Document(collection = "alertas")
public class Alerta implements ConIdSecuencial {

    /** Reactiva: ya ocurrió. Predictiva: el modelo la anticipa. Extraordinaria: la declara un administrador. */
    public enum Tipo { REACTIVA, PREDICTIVA, EXTRAORDINARIA }

    public enum Nivel {
        AMARILLA("Alerta Amarilla"), NARANJA("Alerta Naranja"), ROJA("Alerta Roja"), PURPURA("Alerta Púrpura");

        private final String nombre;

        Nivel(String nombre) {
            this.nombre = nombre;
        }

        public String nombre() {
            return nombre;
        }
    }

    public enum Estado { ACTIVA, RESUELTA }

    @Id
    private Long id;
    private Tipo tipo;
    private Nivel nivel;
    private String titulo;
    private String mensaje;
    private String recomendacion;
    private Long estacionId;
    private String estacionNombre;
    private Double valorDisparador;
    private List<CanalAlerta.Codigo> canalesEntregados = new ArrayList<>();
    private String origen;
    private Estado estado;
    private LocalDateTime fechaCreacion;

    protected Alerta() {
    }

    public Alerta(Tipo tipo, Nivel nivel, String titulo, String mensaje, String recomendacion,
                  Estacion estacion, Double valorDisparador, String origen) {
        this.tipo = tipo;
        this.nivel = nivel;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.recomendacion = recomendacion;
        this.estacionId = estacion == null ? null : estacion.getId();
        this.estacionNombre = estacion == null ? null : estacion.getNombre();
        this.valorDisparador = valorDisparador;
        this.origen = origen;
        this.estado = Estado.ACTIVA;
        this.fechaCreacion = LocalDateTime.now();
    }

    public void registrarEntrega(List<CanalAlerta.Codigo> canales) {
        this.canalesEntregados = new ArrayList<>(canales);
    }

    public void resolver() {
        this.estado = Estado.RESUELTA;
    }

    public boolean seMuestraEnBanner(LocalDateTime desde) {
        return estado == Estado.ACTIVA && fechaCreacion.isAfter(desde)
                && canalesEntregados.contains(CanalAlerta.Codigo.BANNER_WEB);
    }

    @Override
    public void asignarId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() { return id; }
    public Tipo getTipo() { return tipo; }
    public Nivel getNivel() { return nivel; }
    public String getTitulo() { return titulo; }
    public String getMensaje() { return mensaje; }
    public String getRecomendacion() { return recomendacion; }
    public Long getEstacionId() { return estacionId; }
    public String getEstacionNombre() { return estacionNombre; }
    public Double getValorDisparador() { return valorDisparador; }
    public List<CanalAlerta.Codigo> getCanalesEntregados() { return List.copyOf(canalesEntregados); }
    public String getOrigen() { return origen; }
    public Estado getEstado() { return estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
