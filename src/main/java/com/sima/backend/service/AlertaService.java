package com.sima.backend.service;

import com.sima.backend.dto.response.AlertaResponse;
import java.util.List;

public interface AlertaService {
    // Retrieve all active (not resolved) alerts
    List<AlertaResponse> getAlertasActivas();

    // Count active alerts for a specific adult
    long contarAlertasActivas(Integer idAdulto);

    // Resolve an alert
    void resolverAlerta(Integer idAlerta);
}
