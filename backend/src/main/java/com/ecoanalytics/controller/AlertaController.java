package com.ecoanalytics.controller;

import com.ecoanalytics.dto.Respuestas.AlertaRespuesta;
import com.ecoanalytics.dto.Respuestas.CanalRespuesta;
import com.ecoanalytics.dto.Solicitudes.AlertaExtraordinaria;
import com.ecoanalytics.dto.Solicitudes.CambioHabilitado;
import com.ecoanalytics.model.CanalAlerta;
import com.ecoanalytics.service.AlertaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Centro de alertas (HU-14). El banner es público para el portal ciudadano. */
@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    private final AlertaService alertaService;

    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    public record CentroAlertas(List<AlertaRespuesta> alertas, AlertaService.EstadisticasAlertas estadisticas,
                                List<CanalRespuesta> canales) {
    }

    @GetMapping
    public CentroAlertas centro() {
        return new CentroAlertas(alertaService.historial().stream().map(AlertaRespuesta::de).toList(),
                alertaService.estadisticas(), alertaService.canales().stream().map(CanalRespuesta::de).toList());
    }

    @GetMapping("/banner")
    public List<AlertaRespuesta> banner() {
        return alertaService.banner().stream().map(AlertaRespuesta::de).toList();
    }

    @PatchMapping("/canales/{codigo}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public CanalRespuesta cambiarCanal(@PathVariable CanalAlerta.Codigo codigo, @RequestBody CambioHabilitado solicitud) {
        return CanalRespuesta.de(alertaService.cambiarCanal(codigo, solicitud.habilitado()));
    }

    @PostMapping("/extraordinarias")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AlertaRespuesta emitirExtraordinaria(@Valid @RequestBody AlertaExtraordinaria s) {
        return AlertaRespuesta.de(alertaService.emitirExtraordinaria(s.titulo(), s.mensaje(), s.nivel(), s.estacionId()));
    }

    @PostMapping("/{id}/resolver")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
    public AlertaRespuesta resolver(@PathVariable Long id) {
        return AlertaRespuesta.de(alertaService.resolver(id));
    }
}
