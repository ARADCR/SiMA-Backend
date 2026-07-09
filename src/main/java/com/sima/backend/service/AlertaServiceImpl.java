package com.sima.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.sima.backend.repository.AlertaRepository;
import com.sima.backend.dto.response.AlertaResponse;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlertaServiceImpl implements AlertaService {
    @Autowired
    private AlertaRepository alertaRepository;

    @Override
    public List<AlertaResponse> getAlertasActivas() {
        List<com.sima.backend.entity.Alerta> alerts = alertaRepository.findByResueltaFalseOrderByCreadoEnDesc();
        return alerts.stream()
                .map(AlertaResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public long contarAlertasActivas(Integer idAdulto) {
        if (idAdulto == null) {
            // total for all adults
            return alertaRepository.findByResueltaFalseOrderByCreadoEnDesc().size();
        }
        return alertaRepository.countByAdulto_IdAdultoAndResueltaFalse(idAdulto);
    }
}
