package com.ecoanalytics.controller;

import com.ecoanalytics.service.VisualizacionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Visualización avanzada (HU-09). */
@RestController
@RequestMapping("/api/visualizacion")
public class VisualizacionController {

    private final VisualizacionService visualizacionService;

    public VisualizacionController(VisualizacionService visualizacionService) {
        this.visualizacionService = visualizacionService;
    }

    @GetMapping
    public VisualizacionService.Visualizacion construir(@RequestParam Long estacionId,
                                                        @RequestParam(defaultValue = "3") int dias) {
        return visualizacionService.construir(estacionId, dias);
    }
}
