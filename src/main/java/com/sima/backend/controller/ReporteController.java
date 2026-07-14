package com.sima.backend.controller;

import com.sima.backend.dto.response.ApiResponse;
import com.sima.backend.dto.response.ReporteMedicionSemanalResponse;
import com.sima.backend.security.CustomUserDetails;
import com.sima.backend.service.ReporteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reportes/medicacion")
@PreAuthorize("hasAnyRole('Cuidador', 'Familiar', 'Administrador')")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/{idAdulto}/semanal")
    public ResponseEntity<ApiResponse<ReporteMedicionSemanalResponse>> reporteSemanal(
            @PathVariable Integer idAdulto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ReporteMedicionSemanalResponse reporte = reporteService.generarReporteSemanal(
                userDetails.getIdUsuario(), idAdulto);

        return ResponseEntity.ok(ApiResponse.ok("Reporte semanal generado correctamente", reporte));
    }
}
