package com.ecoanalytics.model;

import com.ecoanalytics.model.prediccion.AlgoritmoModelo;
import com.ecoanalytics.model.prediccion.MetricasModelo;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Registro de un modelo predictivo entrenado. Los parámetros ajustados viven en memoria
 * (ver RegistroModelos); este documento conserva la trazabilidad: algoritmo, métricas y estado.
 */
@Document(collection = "versiones_modelo")
public class VersionModelo implements ConIdSecuencial {

    public enum Estado { PRODUCCION, ARCHIVADO, CANDIDATO }

    @Id
    private Long id;
    private String version;
    private AlgoritmoModelo algoritmo;
    private String variableObjetivo;
    private String caracteristicas;
    private double r2;
    private double rmse;
    private double mae;
    private int muestrasEntrenamiento;
    private int muestrasPrueba;
    private long duracionMs;
    private Estado estado;
    private String entrenadoPor;
    private LocalDateTime fechaEntrenamiento;

    protected VersionModelo() {
    }

    public VersionModelo(String version, AlgoritmoModelo algoritmo, String variableObjetivo, String caracteristicas,
                         MetricasModelo metricas, long duracionMs, String entrenadoPor) {
        this.version = version;
        this.algoritmo = algoritmo;
        this.variableObjetivo = variableObjetivo;
        this.caracteristicas = caracteristicas;
        this.r2 = metricas.r2();
        this.rmse = metricas.rmse();
        this.mae = metricas.mae();
        this.muestrasEntrenamiento = metricas.muestrasEntrenamiento();
        this.muestrasPrueba = metricas.muestrasPrueba();
        this.duracionMs = duracionMs;
        this.entrenadoPor = entrenadoPor;
        this.estado = Estado.CANDIDATO;
        this.fechaEntrenamiento = LocalDateTime.now();
    }

    public void promover() {
        this.estado = Estado.PRODUCCION;
    }

    public void archivar() {
        this.estado = Estado.ARCHIVADO;
    }

    public boolean estaEnProduccion() {
        return estado == Estado.PRODUCCION;
    }

    @Override
    public void asignarId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() { return id; }
    public String getVersion() { return version; }
    public AlgoritmoModelo getAlgoritmo() { return algoritmo; }
    public String getVariableObjetivo() { return variableObjetivo; }
    public String getCaracteristicas() { return caracteristicas; }
    public double getR2() { return r2; }
    public double getRmse() { return rmse; }
    public double getMae() { return mae; }
    public int getMuestrasEntrenamiento() { return muestrasEntrenamiento; }
    public int getMuestrasPrueba() { return muestrasPrueba; }
    public long getDuracionMs() { return duracionMs; }
    public Estado getEstado() { return estado; }
    public String getEntrenadoPor() { return entrenadoPor; }
    public LocalDateTime getFechaEntrenamiento() { return fechaEntrenamiento; }
}
