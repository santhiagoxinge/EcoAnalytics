package com.ecoanalytics.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Lectura horaria de una estación. Guarda la referencia {@code estacionId} en lugar de incrustar
 * la estación: hay decenas de miles de mediciones y la estación cambia de forma independiente.
 */
@Document(collection = "mediciones")
@CompoundIndex(name = "idx_estacion_fecha", def = "{'estacionId': 1, 'fechaMedicion': 1}")
public class Medicion implements ConIdSecuencial {

    @Id
    private Long id;
    private Long estacionId;
    private LocalDateTime fechaMedicion;
    private Double pm25;
    private Double pm10;
    private Double temperatura;
    private Double humedad;
    private TipoFuente fuente;
    private EstadoMedicion estado;
    private Long loteId;

    protected Medicion() {
    }

    public Medicion(Long estacionId, LocalDateTime fechaMedicion, Double pm25, Double pm10, Double temperatura,
                    Double humedad, TipoFuente fuente, EstadoMedicion estado, Long loteId) {
        this.estacionId = estacionId;
        this.fechaMedicion = fechaMedicion;
        this.pm25 = pm25;
        this.pm10 = pm10;
        this.temperatura = temperatura;
        this.humedad = humedad;
        this.fuente = fuente;
        this.estado = estado;
        this.loteId = loteId;
    }

    /** Valor de una variable concreta; la medición es la experta en sus propios datos. */
    public Double valor(VariableAmbiental variable) {
        return switch (variable) {
            case PM25 -> pm25;
            case PM10 -> pm10;
            case TEMPERATURA -> temperatura;
            case HUMEDAD -> humedad;
        };
    }

    public boolean esUtilizable() {
        return estado != EstadoMedicion.DESCARTADA && pm25 != null;
    }

    public IndiceCalidadAire ica() {
        return IndiceCalidadAire.combinado(pm25, pm10);
    }

    @Override
    public void asignarId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() { return id; }
    public Long getEstacionId() { return estacionId; }
    public LocalDateTime getFechaMedicion() { return fechaMedicion; }
    public Double getPm25() { return pm25; }
    public Double getPm10() { return pm10; }
    public Double getTemperatura() { return temperatura; }
    public Double getHumedad() { return humedad; }
    public TipoFuente getFuente() { return fuente; }
    public EstadoMedicion getEstado() { return estado; }
    public Long getLoteId() { return loteId; }
}
