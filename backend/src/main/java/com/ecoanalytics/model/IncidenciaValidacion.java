package com.ecoanalytics.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Problema encontrado en un registro de un lote. Se guarda en su propia colección y no dentro del lote:
 * un archivo grande puede producir miles de incidencias y un documento MongoDB tiene un límite de 16 MB.
 */
@Document(collection = "incidencias_validacion")
public class IncidenciaValidacion implements ConIdSecuencial {

    public enum Tipo { FUERA_DE_RANGO, DUPLICADO, CAMPO_FALTANTE, ESTACION_DESCONOCIDA, FECHA_INVALIDA }

    /** DESCARTADO: el registro nunca llega al análisis. MARCADO: se conserva como advertencia. */
    public enum Resolucion { DESCARTADO, MARCADO }

    @Id
    private Long id;
    @Indexed
    private Long loteId;
    private int fila;
    private String estacionNombre;
    private String variable;
    private String valorDetectado;
    private String rangoAceptable;
    private Tipo tipo;
    private Resolucion resolucion;

    protected IncidenciaValidacion() {
    }

    public IncidenciaValidacion(Long loteId, int fila, String estacionNombre, String variable,
                                String valorDetectado, String rangoAceptable, Tipo tipo, Resolucion resolucion) {
        this.loteId = loteId;
        this.fila = fila;
        this.estacionNombre = estacionNombre;
        this.variable = variable;
        this.valorDetectado = valorDetectado;
        this.rangoAceptable = rangoAceptable;
        this.tipo = tipo;
        this.resolucion = resolucion;
    }

    @Override
    public void asignarId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() { return id; }
    public Long getLoteId() { return loteId; }
    public int getFila() { return fila; }
    public String getEstacionNombre() { return estacionNombre; }
    public String getVariable() { return variable; }
    public String getValorDetectado() { return valorDetectado; }
    public String getRangoAceptable() { return rangoAceptable; }
    public Tipo getTipo() { return tipo; }
    public Resolucion getResolucion() { return resolucion; }
}
