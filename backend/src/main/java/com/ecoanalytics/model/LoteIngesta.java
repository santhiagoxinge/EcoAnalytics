package com.ecoanalytics.model;

import com.ecoanalytics.exception.ExcepcionNegocio;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/** Archivo cargado por un analista, desde su revisión hasta que se importa o se cancela. */
@Document(collection = "lotes_ingesta")
public class LoteIngesta implements ConIdSecuencial {

    public enum Estado { PENDIENTE_REVISION, IMPORTADO, CANCELADO }

    @Id
    private Long id;
    private String nombreArchivo;
    private TipoFuente formato;
    private int totalRegistros;
    private int registrosValidos;
    private int registrosAdvertencia;
    private int registrosRechazados;
    private int registrosImportados;
    private Estado estado;
    private String cargadoPor;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaImportacion;

    protected LoteIngesta() {
    }

    public LoteIngesta(String nombreArchivo, TipoFuente formato, String cargadoPor) {
        this.nombreArchivo = nombreArchivo;
        this.formato = formato;
        this.cargadoPor = cargadoPor;
        this.estado = Estado.PENDIENTE_REVISION;
        this.fechaCreacion = LocalDateTime.now();
    }

    public void resumir(int total, int validos, int advertencias, int rechazados) {
        this.totalRegistros = total;
        this.registrosValidos = validos;
        this.registrosAdvertencia = advertencias;
        this.registrosRechazados = rechazados;
    }

    public void marcarImportado(int registrosImportados) {
        verificarPendiente();
        this.registrosImportados = registrosImportados;
        this.estado = Estado.IMPORTADO;
        this.fechaImportacion = LocalDateTime.now();
    }

    public void cancelar() {
        verificarPendiente();
        this.estado = Estado.CANCELADO;
    }

    /** Un lote ya importado o cancelado no se vuelve a procesar. */
    public void verificarPendiente() {
        if (estado != Estado.PENDIENTE_REVISION) {
            throw ExcepcionNegocio.conflicto("El lote #" + id + " ya fue procesado.");
        }
    }

    @Override
    public void asignarId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() { return id; }
    public String getNombreArchivo() { return nombreArchivo; }
    public TipoFuente getFormato() { return formato; }
    public int getTotalRegistros() { return totalRegistros; }
    public int getRegistrosValidos() { return registrosValidos; }
    public int getRegistrosAdvertencia() { return registrosAdvertencia; }
    public int getRegistrosRechazados() { return registrosRechazados; }
    public int getRegistrosImportados() { return registrosImportados; }
    public Estado getEstado() { return estado; }
    public String getCargadoPor() { return cargadoPor; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaImportacion() { return fechaImportacion; }
}
