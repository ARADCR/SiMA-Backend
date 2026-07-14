package com.sima.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import com.sima.backend.service.AlertaService;
import com.sima.backend.dto.response.AlertaResponse;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.sima.backend.dto.response.ApiResponse;

@RestController
@RequestMapping("/alertas")
public class AlertaController {

    @Autowired
    private AlertaService alertaService;

    // GET /api/alertas/activas -> list of active alerts
    @GetMapping("/activas")
    public ApiResponse<List<AlertaResponse>> getAlertasActivas() {
        return ApiResponse.ok("Alertas activas", alertaService.getAlertasActivas());
    }

    // GET /api/alertas/contador -> total number of active alerts
    @GetMapping("/contador")
    public ApiResponse<Map<String, Long>> getContadorAlertas() {
        // Simple approach: fetch list and count size
        long total = alertaService.getAlertasActivas().size();
        Map<String, Long> resp = new HashMap<>();
        resp.put("total", total);
        return ApiResponse.ok("Contador de alertas", resp);
    }

    // PATCH /alertas/{id}/resolver -> resolve an alert
    @org.springframework.web.bind.annotation.PatchMapping("/{id}/resolver")
    public ApiResponse<AlertaResponse> resolverAlerta(@org.springframework.web.bind.annotation.PathVariable("id") Integer id) {
        alertaService.resolverAlerta(id);
        return ApiResponse.ok("Alerta resuelta exitosamente");
    }
}
