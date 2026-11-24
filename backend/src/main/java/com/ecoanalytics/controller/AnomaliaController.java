package com.ecoanalytics.controller;

import com.ecoanalytics.dto.Respuestas.AnomaliaRespuesta;
import com.ecoanalytics.dto.Solicitudes.CambioUmbral;
import com.ecoanalytics.service.AnomaliaService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Detección y gestión de anomalías (HU-11). */
@RestController
@RequestMapping("/api/anomalias")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
public class AnomaliaController {

    private final AnomaliaService anomaliaService;

    public AnomaliaController(AnomaliaService anomaliaService) {
        this.anomaliaService = anomaliaService;
    }

    public record AnomaliasRespuesta(List<AnomaliaRespuesta> anomalias, AnomaliaService.EstadisticasAnomalias estadisticas) {
    }

    @GetMapping
    public AnomaliasRespuesta listar() {
        return new AnomaliasRespuesta(anomaliaService.recientes().stream().map(AnomaliaRespuesta::de).toList(),
                anomaliaService.estadisticas());
    }

    @GetMapping("/linea-tiempo")
    public List<AnomaliaService.PuntoLineaTiempo> lineaTiempo(@RequestParam Long estacionId,
                                                             @RequestParam(defaultValue = "72") int horas) {
        return anomaliaService.lineaTiempo(estacionId, horas);
    }

    @PostMapping("/{id}/notificar")
    public AnomaliaRespuesta notificar(@PathVariable Long id) {
        return AnomaliaRespuesta.de(anomaliaService.notificar(id));
    }

    @PostMapping("/{id}/descartar")
    public AnomaliaRespuesta descartar(@PathVariable Long id) {
        return AnomaliaRespuesta.de(anomaliaService.descartar(id));
    }

    @PutMapping("/umbral")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AnomaliaService.EstadisticasAnomalias cambiarUmbral(@Valid @RequestBody CambioUmbral solicitud) {
        anomaliaService.cambiarUmbral(solicitud.umbral());
        return anomaliaService.estadisticas();
    }
}
