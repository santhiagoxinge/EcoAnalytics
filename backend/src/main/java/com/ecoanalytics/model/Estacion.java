package com.ecoanalytics.model;

import com.ecoanalytics.exception.ExcepcionNegocio;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Document(collection = "estaciones")
public class Estacion implements ConIdSecuencial {

    /** Una lectura más antigua no se presenta como "actual" (HU-06). */
    private static final Duration VIGENCIA_LECTURA = Duration.ofHours(6);

    @Id
    private Long id;
    @Indexed(unique = true)
    private String nombre;
    private String zona;
    private double latitud;
    private double longitud;
    private TipoFuente tipoFuente;
    private String variables;
    private EstadoEstacion estado;
    private LocalDateTime fechaInstalacion;

    protected Estacion() {
    }

    public Estacion(String nombre, String zona, double latitud, double longitud, TipoFuente tipoFuente, String variables) {
        validarCoordenadas(latitud, longitud);
        this.nombre = nombre;
        this.zona = zona;
        this.latitud = latitud;
        this.longitud = longitud;
        this.tipoFuente = tipoFuente;
        this.variables = variables;
        this.estado = EstadoEstacion.EN_LINEA;
        this.fechaInstalacion = LocalDateTime.now();
    }

    public void actualizar(String nombre, String zona, double latitud, double longitud, TipoFuente tipoFuente, String variables) {
        validarCoordenadas(latitud, longitud);
        this.nombre = nombre;
        this.zona = zona;
        this.latitud = latitud;
        this.longitud = longitud;
        this.tipoFuente = tipoFuente;
        this.variables = variables;
    }

    public void cambiarEstado(EstadoEstacion estado) {
        this.estado = estado;
    }

    public boolean estaTransmitiendo() {
        return estado == EstadoEstacion.EN_LINEA;
    }

    public boolean tieneNombre(String otroNombre) {
        return nombre.equalsIgnoreCase(otroNombre.trim());
    }

    /**
     * Motivo por el que no se pueden mostrar condiciones actuales, o vacío si la última lectura es válida y reciente.
     * La estación conoce su estado y es quien decide (Experto en información).
     */
    public Optional<String> motivoNoDisponible(Optional<Medicion> ultima, LocalDateTime ahora) {
        if (estado == EstadoEstacion.MANTENIMIENTO) {
            return Optional.of("Estación en mantenimiento — datos no disponibles actualmente");
        }
        if (estado == EstadoEstacion.FUERA_DE_LINEA) {
            return Optional.of("Estación fuera de línea — datos no disponibles actualmente");
        }
        if (ultima.isEmpty() || ultima.get().getPm25() == null) {
            return Optional.of("La estación aún no tiene mediciones válidas registradas");
        }
        if (ultima.get().getFechaMedicion().isBefore(ahora.minus(VIGENCIA_LECTURA))) {
            return Optional.of("Sin lecturas recientes — la última medición tiene más de 6 horas");
        }
        return Optional.empty();
    }

    /** Estado de sincronización según los minutos transcurridos desde la última medición. */
    public EstadoSincronizacion sincronizacion(Long minutosDesdeUltima) {
        if (estado == EstadoEstacion.MANTENIMIENTO) return EstadoSincronizacion.MANTENIMIENTO;
        if (estado == EstadoEstacion.FUERA_DE_LINEA || minutosDesdeUltima == null) return EstadoSincronizacion.CAIDA;
        if (minutosDesdeUltima <= 90) return EstadoSincronizacion.SINCRONIZADA;
        return minutosDesdeUltima <= 360 ? EstadoSincronizacion.RETRASADA : EstadoSincronizacion.CAIDA;
    }

    private static void validarCoordenadas(double latitud, double longitud) {
        if (latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180) {
            throw ExcepcionNegocio.invalido("Coordenadas fuera de rango: la latitud debe estar entre -90 y 90 "
                    + "y la longitud entre -180 y 180.");
        }
    }

    @Override
    public void asignarId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getZona() { return zona; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
    public TipoFuente getTipoFuente() { return tipoFuente; }
    public String getVariables() { return variables; }
    public EstadoEstacion getEstado() { return estado; }
    public LocalDateTime getFechaInstalacion() { return fechaInstalacion; }
}
