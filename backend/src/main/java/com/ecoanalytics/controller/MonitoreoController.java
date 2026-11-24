package com.ecoanalytics.controller;

import com.ecoanalytics.dto.Respuestas.AuditoriaRespuesta;
import com.ecoanalytics.model.EventoAuditoria;
import com.ecoanalytics.service.AuditoriaService;
import com.ecoanalytics.service.MonitoreoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Consola técnica y bitácora de auditoría (HU-16). */
@RestController
@RequestMapping("/api/monitoreo")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class MonitoreoController {

    private final MonitoreoService monitoreoService;
    private final AuditoriaService auditoriaService;

    public MonitoreoController(MonitoreoService monitoreoService, AuditoriaService auditoriaService) {
        this.monitoreoService = monitoreoService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public MonitoreoService.Panorama panorama() {
        return monitoreoService.panorama();
    }

    @GetMapping("/auditoria")
    public List<AuditoriaRespuesta> auditoria(@RequestParam(required = false) EventoAuditoria.Nivel nivel) {
        return auditoriaService.recientes(nivel).stream().map(AuditoriaRespuesta::de).toList();
    }
}
