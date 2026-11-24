package com.ecoanalytics.controller;

import com.ecoanalytics.service.PronosticoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Pronóstico de calidad del aire a 24 o 48 horas (HU-13). */
@RestController
@RequestMapping("/api/pronostico")
public class PronosticoController {

    private final PronosticoService pronosticoService;

    public PronosticoController(PronosticoService pronosticoService) {
        this.pronosticoService = pronosticoService;
    }

    @GetMapping
    public PronosticoService.VistaPronostico pronosticar(@RequestParam Long estacionId,
                                                         @RequestParam(defaultValue = "24") int horas) {
        return pronosticoService.pronosticar(estacionId, horas);
    }
}
