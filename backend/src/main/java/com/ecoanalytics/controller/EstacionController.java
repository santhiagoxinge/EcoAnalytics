package com.ecoanalytics.controller;

import com.ecoanalytics.dto.Respuestas.EstacionRespuesta;
import com.ecoanalytics.dto.Solicitudes.CambioEstado;
import com.ecoanalytics.dto.Solicitudes.EstacionSolicitud;
import com.ecoanalytics.model.Estacion;
import com.ecoanalytics.service.EstacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estaciones")
public class EstacionController {

    private final EstacionService estacionService;

    public EstacionController(EstacionService estacionService) {
        this.estacionService = estacionService;
    }

    @GetMapping
    public List<EstacionRespuesta> listar() {
        return estacionService.listar().stream()
                .map(e -> EstacionRespuesta.de(e.estacion(), e.ultimaMedicion().orElse(null)))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public EstacionRespuesta registrar(@Valid @RequestBody EstacionSolicitud s) {
        Estacion e = estacionService.registrar(s.nombre(), s.zona(), s.latitud(), s.longitud(), s.tipoFuente(), s.variables());
        return EstacionRespuesta.de(e, null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public EstacionRespuesta actualizar(@PathVariable Long id, @Valid @RequestBody EstacionSolicitud s) {
        Estacion e = estacionService.actualizar(id, s.nombre(), s.zona(), s.latitud(), s.longitud(), s.tipoFuente(), s.variables());
        return conActividad(e);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public EstacionRespuesta cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstado s) {
        return conActividad(estacionService.cambiarEstado(id, s.estado()));
    }

    private EstacionRespuesta conActividad(Estacion e) {
        return EstacionRespuesta.de(e, estacionService.ultimaMedicion(e.getId()).orElse(null));
    }
}
