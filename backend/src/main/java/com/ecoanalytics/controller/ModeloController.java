package com.ecoanalytics.controller;

import com.ecoanalytics.dto.Respuestas.ModeloRespuesta;
import com.ecoanalytics.dto.Solicitudes.Entrenamiento;
import com.ecoanalytics.service.EntrenamientoModeloService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Entrenamiento y despliegue de modelos predictivos (HU-12). */
@RestController
@RequestMapping("/api/modelos")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
public class ModeloController {

    private final EntrenamientoModeloService entrenamientoService;

    public ModeloController(EntrenamientoModeloService entrenamientoService) {
        this.entrenamientoService = entrenamientoService;
    }

    public record EntrenamientoRespuesta(ModeloRespuesta modelo, String estacionNombre,
                                         List<EntrenamientoModeloService.PuntoComparacion> comparacion) {
    }

    @GetMapping
    public List<ModeloRespuesta> listar() {
        return entrenamientoService.versiones().stream().map(ModeloRespuesta::de).toList();
    }

    @PostMapping("/entrenar")
    public EntrenamientoRespuesta entrenar(@Valid @RequestBody Entrenamiento solicitud) {
        EntrenamientoModeloService.ResultadoEntrenamiento r = entrenamientoService.entrenar(solicitud.algoritmo());
        return new EntrenamientoRespuesta(ModeloRespuesta.de(r.modelo()), r.estacionNombre(), r.comparacion());
    }

    @PostMapping("/{id}/desplegar")
    public ModeloRespuesta desplegar(@PathVariable Long id) {
        return ModeloRespuesta.de(entrenamientoService.desplegar(id));
    }
}
