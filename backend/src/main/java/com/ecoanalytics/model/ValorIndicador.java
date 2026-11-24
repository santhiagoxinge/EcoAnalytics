package com.ecoanalytics.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "valores_indicador")
@CompoundIndex(name = "idx_estacion_tipo_fecha", def = "{'estacionId': 1, 'tipo': 1, 'fechaCalculo': -1}")
public class ValorIndicador implements ConIdSecuencial {

    @Id
    private Long id;
    private Long estacionId;
    private TipoIndicador tipo;
    private double valor;
    private LocalDateTime fechaCalculo;

    protected ValorIndicador() {
    }

    public ValorIndicador(Long estacionId, TipoIndicador tipo, double valor) {
        this.estacionId = estacionId;
        this.tipo = tipo;
        this.valor = valor;
        this.fechaCalculo = LocalDateTime.now();
    }

    @Override
    public void asignarId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() { return id; }
    public Long getEstacionId() { return estacionId; }
    public TipoIndicador getTipo() { return tipo; }
    public double getValor() { return valor; }
    public LocalDateTime getFechaCalculo() { return fechaCalculo; }
}
