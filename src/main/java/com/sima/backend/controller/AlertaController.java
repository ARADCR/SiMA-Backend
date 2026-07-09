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

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    @Autowired
    private AlertaService alertaService;

    // GET /api/alertas/activas -> list of active alerts
    @GetMapping("/activas")
    public List<AlertaResponse> getAlertasActivas() {
        return alertaService.getAlertasActivas();
    }

    // GET /api/alertas/contador -> total number of active alerts
    @GetMapping("/contador")
    public Map<String, Long> getContadorAlertas() {
        // Simple approach: fetch list and count size
        long total = alertaService.getAlertasActivas().size();
        Map<String, Long> resp = new HashMap<>();
        resp.put("total", total);
        return resp;
    }
}
