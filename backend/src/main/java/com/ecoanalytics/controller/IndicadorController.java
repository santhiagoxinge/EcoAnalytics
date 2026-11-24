package com.ecoanalytics.controller;

import com.ecoanalytics.dto.Respuestas.ConfiguracionIndicadorRespuesta;
import com.ecoanalytics.dto.Solicitudes.CambioHabilitado;
import com.ecoanalytics.model.TipoIndicador;
import com.ecoanalytics.service.IndicadorService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Indicadores agregados (HU-10). */
@RestController
@RequestMapping("/api/indicadores")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
public class IndicadorController {

    private final IndicadorService indicadorService;

    public IndicadorController(IndicadorService indicadorService) {
        this.indicadorService = indicadorService;
    }

    public record IndicadoresRespuesta(List<ConfiguracionIndicadorRespuesta> configuraciones,
                                       List<IndicadorService.IndicadoresEstacion> estaciones) {
    }

    @GetMapping
    public IndicadoresRespuesta listar() {
        return new IndicadoresRespuesta(
                indicadorService.configuraciones().stream().map(ConfiguracionIndicadorRespuesta::de).toList(),
                indicadorService.ultimos());
    }

    @PatchMapping("/{tipo}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ConfiguracionIndicadorRespuesta cambiarHabilitado(@PathVariable TipoIndicador tipo,
                                                             @RequestBody CambioHabilitado solicitud) {
        return ConfiguracionIndicadorRespuesta.de(indicadorService.cambiarHabilitado(tipo, solicitud.habilitado()));
    }

    @PostMapping("/recalcular")
    public IndicadorService.ResultadoRecalculo recalcular() {
        return indicadorService.recalcular();
    }
}
