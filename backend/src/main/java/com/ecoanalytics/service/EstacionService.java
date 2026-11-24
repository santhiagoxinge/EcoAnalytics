package com.ecoanalytics.service;

import com.ecoanalytics.exception.ExcepcionNegocio;
import com.ecoanalytics.model.Estacion;
import com.ecoanalytics.model.EstadoEstacion;
import com.ecoanalytics.model.Medicion;
import com.ecoanalytics.model.TipoFuente;
import com.ecoanalytics.repository.EstacionRepository;
import com.ecoanalytics.repository.MedicionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Registro y gestión de estaciones de monitoreo (HU-03). */
@Service
public class EstacionService {

    private final EstacionRepository estaciones;
    private final MedicionRepository mediciones;
    private final AuditoriaService auditoria;

    public EstacionService(EstacionRepository estaciones, MedicionRepository mediciones, AuditoriaService auditoria) {
        this.estaciones = estaciones;
        this.mediciones = mediciones;
        this.auditoria = auditoria;
    }

    public record EstacionConActividad(Estacion estacion, Optional<LocalDateTime> ultimaMedicion) {
    }

    public List<EstacionConActividad> listar() {
        return estaciones.findAllByOrderByNombreAsc().stream()
                .map(e -> new EstacionConActividad(e, ultimaMedicion(e.getId())))
                .toList();
    }

    public Estacion buscar(Long id) {
        return estaciones.findById(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("Estación no encontrada."));
    }

    public Optional<LocalDateTime> ultimaMedicion(Long estacionId) {
        return mediciones.findFirstByEstacionIdOrderByFechaMedicionDesc(estacionId).map(Medicion::getFechaMedicion);
    }

    public Estacion registrar(String nombre, String zona, double latitud, double longitud, TipoFuente tipoFuente,
                              String variables) {
        verificarNombreDisponible(nombre, null);
        Estacion estacion = estaciones.save(new Estacion(nombre.trim(), zona.trim(), latitud, longitud, tipoFuente, variables));
        auditoria.info("Estaciones", "Registro de la estación " + estacion.getNombre() + " (" + estacion.getZona() + ")");
        return estacion;
    }

    public Estacion actualizar(Long id, String nombre, String zona, double latitud, double longitud,
                               TipoFuente tipoFuente, String variables) {
        Estacion estacion = buscar(id);
        verificarNombreDisponible(nombre, id);
        estacion.actualizar(nombre.trim(), zona.trim(), latitud, longitud, tipoFuente, variables);
        estaciones.save(estacion);
        auditoria.info("Estaciones", "Actualización de la estación " + estacion.getNombre());
        return estacion;
    }

    public Estacion cambiarEstado(Long id, EstadoEstacion estado) {
        Estacion estacion = buscar(id);
        EstadoEstacion anterior = estacion.getEstado();
        estacion.cambiarEstado(estado);
        estaciones.save(estacion);
        String descripcion = "Estación " + estacion.getNombre() + ": " + anterior + " → " + estado;
        if (estado == EstadoEstacion.EN_LINEA) auditoria.info("Estaciones", descripcion);
        else auditoria.advertencia("Estaciones", descripcion);
        return estacion;
    }

    /** El nombre es único sin distinguir mayúsculas; cada estación sabe si tiene un nombre dado. */
    private void verificarNombreDisponible(String nombre, Long idPropio) {
        boolean ocupado = estaciones.findAll().stream()
                .anyMatch(e -> e.tieneNombre(nombre) && !e.getId().equals(idPropio));
        if (ocupado) throw ExcepcionNegocio.conflicto("Ya existe una estación llamada " + nombre.trim() + ".");
    }
}
