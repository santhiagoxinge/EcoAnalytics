package com.ecoanalytics.controller;

import com.ecoanalytics.dto.Respuestas.IncidenciaRespuesta;
import com.ecoanalytics.dto.Respuestas.LoteRespuesta;
import com.ecoanalytics.model.validacion.RegistroMedicionCrudo;
import com.ecoanalytics.service.IngestaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/** Carga de archivos, revisión de calidad e importación de lotes (HU-04, HU-05). */
@RestController
@RequestMapping("/api/ingesta")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
public class IngestaController {

    private final IngestaService ingestaService;

    public IngestaController(IngestaService ingestaService) {
        this.ingestaService = ingestaService;
    }

    public record CargaRespuesta(LoteRespuesta lote, List<String> columnas, List<RegistroMedicionCrudo> vistaPrevia,
                                 List<IncidenciaRespuesta> incidencias) {
    }

    public record ImportacionRespuesta(LoteRespuesta lote, int importados, int descartados, int interpolados,
                                       int yaExistentes) {
    }

    @PostMapping("/cargar")
    public CargaRespuesta cargar(@RequestParam("archivo") MultipartFile archivo) {
        IngestaService.ResultadoCarga r = ingestaService.cargar(archivo);
        return new CargaRespuesta(LoteRespuesta.de(r.lote()), r.columnas(), r.vistaPrevia(),
                r.incidencias().stream().map(IncidenciaRespuesta::de).toList());
    }

    @PostMapping("/{loteId}/confirmar")
    public ImportacionRespuesta confirmar(@PathVariable Long loteId) {
        IngestaService.ResultadoImportacion r = ingestaService.confirmar(loteId);
        return new ImportacionRespuesta(LoteRespuesta.de(r.lote()), r.importados(), r.descartados(), r.interpolados(),
                r.yaExistentes());
    }

    @PostMapping("/{loteId}/cancelar")
    public void cancelar(@PathVariable Long loteId) {
        ingestaService.cancelar(loteId);
    }

    @GetMapping("/lotes")
    public List<LoteRespuesta> lotes() {
        return ingestaService.lotesRecientes().stream().map(LoteRespuesta::de).toList();
    }

    @GetMapping("/{loteId}/incidencias/exportar")
    public ResponseEntity<byte[]> exportarIncidencias(@PathVariable Long loteId) {
        return Descargas.csv("reporte_calidad_" + LocalDate.now() + ".csv", ingestaService.exportarIncidenciasCsv(loteId));
    }
}
