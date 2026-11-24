package com.ecoanalytics.controller;

import com.ecoanalytics.dto.Solicitudes.ReporteSolicitud;
import com.ecoanalytics.model.reporte.DocumentoReporte;
import com.ecoanalytics.model.reporte.ExportadorReporte;
import com.ecoanalytics.service.ReporteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Generación y exportación de reportes (HU-15). */
@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ANALISTA')")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @PostMapping("/vista-previa")
    public DocumentoReporte vistaPrevia(@Valid @RequestBody ReporteSolicitud s) {
        return reporteService.vistaPrevia(solicitud(s));
    }

    @PostMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@Valid @RequestBody ReporteSolicitud s,
                                           @RequestParam ExportadorReporte.Formato formato) {
        ReporteService.ReporteExportado reporte = reporteService.exportar(solicitud(s), formato);
        return Descargas.archivo(reporte.nombreArchivo(), reporte.tipoContenido(), reporte.contenido());
    }

    private static ReporteService.SolicitudReporte solicitud(ReporteSolicitud s) {
        return new ReporteService.SolicitudReporte(s.tipo(), s.estacionId(), s.desde(), s.hasta());
    }
}
