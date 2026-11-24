package com.ecoanalytics.controller;

import com.ecoanalytics.service.HistoricoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Consulta y exportación del histórico (HU-07). */
@RestController
@RequestMapping("/api/historico")
public class HistoricoController {

    private final HistoricoService historicoService;

    public HistoricoController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    @GetMapping
    public HistoricoService.PaginaHistorico consultar(@RequestParam Long estacionId,
                                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                      @RequestParam(defaultValue = "true") boolean advertencias,
                                                      @RequestParam(defaultValue = "false") boolean descartadas,
                                                      @RequestParam(defaultValue = "1") int pagina,
                                                      @RequestParam(defaultValue = "20") int tamano) {
        HistoricoService.Filtro filtro = new HistoricoService.Filtro(estacionId, desde, hasta, advertencias, descartadas);
        return historicoService.consultar(filtro, pagina, tamano);
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@RequestParam Long estacionId,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                           @RequestParam(defaultValue = "true") boolean advertencias,
                                           @RequestParam(defaultValue = "false") boolean descartadas) {
        HistoricoService.Filtro filtro = new HistoricoService.Filtro(estacionId, desde, hasta, advertencias, descartadas);
        return Descargas.csv(historicoService.nombreArchivoExportacion(filtro), historicoService.exportarCsv(filtro));
    }
}
