package com.ecoanalytics.controller;

import com.ecoanalytics.model.VariableAmbiental;
import com.ecoanalytics.service.ComparativaService;
import com.ecoanalytics.service.ComparativaService.Periodo;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Comparación entre dos periodos (HU-08). */
@RestController
@RequestMapping("/api/comparativa")
public class ComparativaController {

    private final ComparativaService comparativaService;

    public ComparativaController(ComparativaService comparativaService) {
        this.comparativaService = comparativaService;
    }

    @GetMapping
    public ComparativaService.Comparativa comparar(@RequestParam Long estacionId,
                                                   @RequestParam(defaultValue = "PM25") VariableAmbiental variable,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate aDesde,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate aHasta,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bDesde,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bHasta) {
        return comparativaService.comparar(estacionId, variable, new Periodo(aDesde, aHasta), new Periodo(bDesde, bHasta));
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@RequestParam Long estacionId,
                                           @RequestParam(defaultValue = "PM25") VariableAmbiental variable,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate aDesde,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate aHasta,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bDesde,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bHasta) {
        String csv = comparativaService.exportarCsv(estacionId, variable, new Periodo(aDesde, aHasta), new Periodo(bDesde, bHasta));
        return Descargas.csv("comparativa_" + aDesde + "_vs_" + bDesde + ".csv", csv);
    }
}
