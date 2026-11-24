package com.ecoanalytics.controller;

import com.ecoanalytics.service.CondicionesService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Condiciones actuales (HU-06). El resumen de todas las estaciones es público. */
@RestController
@RequestMapping("/api/condiciones")
public class CondicionesController {

    private final CondicionesService condicionesService;

    public CondicionesController(CondicionesService condicionesService) {
        this.condicionesService = condicionesService;
    }

    @GetMapping
    public List<CondicionesService.ResumenEstacion> resumen() {
        return condicionesService.resumen();
    }

    @GetMapping("/{estacionId}")
    public CondicionesService.CondicionesActuales actuales(@PathVariable Long estacionId) {
        return condicionesService.actuales(estacionId);
    }
}
