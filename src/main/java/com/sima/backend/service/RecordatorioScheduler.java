package com.sima.backend.service;

import com.sima.backend.entity.Alerta;
import com.sima.backend.entity.HorarioMedicamento;
import com.sima.backend.repository.AlertaRepository;
import com.sima.backend.repository.HorarioMedicamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RecordatorioScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioScheduler.class);

    private final HorarioMedicamentoRepository horarioRepository;
    private final NotificationService notificationService;
    private final AlertaRepository alertaRepository;

    public RecordatorioScheduler(HorarioMedicamentoRepository horarioRepository,
                                 NotificationService notificationService,
                                 AlertaRepository alertaRepository) {
        this.horarioRepository = horarioRepository;
        this.notificationService = notificationService;
        this.alertaRepository = alertaRepository;
    }

    // Se ejecuta cada minuto en el segundo 0
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void verificarRecordatorios() {
        LocalTime ahora = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        log.debug("Verificando recordatorios de medicamentos para la hora: {}", ahora);

        List<HorarioMedicamento> horariosActuales = horarioRepository.findByHoraProgramada(ahora);

        for (HorarioMedicamento horario : horariosActuales) {
            try {
                enviarRecordatorio(horario);
            } catch (Exception e) {
                log.error("Error al procesar el recordatorio para el horario {}", horario.getIdHorario(), e);
            }
        }
    }

    private void enviarRecordatorio(HorarioMedicamento horario) {
        // 1. Crear registro de Alerta en DB
        Alerta alerta = new Alerta();
        alerta.setTipoAlerta("RECORDATORIO_MEDICAMENTO");
        alerta.setMensaje("Hora de tomar: " + horario.getMedicamento().getNombre() + " - Dosis: " + horario.getMedicamento().getDosis());
        alerta.setAdulto(horario.getMedicamento().getAdulto());
        // alerta.setMedicamento(horario.getMedicamento()); // si existe la relación en Alerta
        
        alertaRepository.save(alerta);

        // 2. Enviar notificación push (SSE) al adulto mayor
        Map<String, Object> payload = new HashMap<>();
        payload.put("idMedicamento", horario.getMedicamento().getIdMedicamento());
        payload.put("nombre", horario.getMedicamento().getNombre());
        payload.put("dosis", horario.getMedicamento().getDosis());
        payload.put("hora", horario.getHoraProgramada().toString());
        payload.put("idAlerta", alerta.getIdAlerta());

        notificationService.sendNotification(
                horario.getMedicamento().getAdulto().getIdAdulto(),
                "RECORDATORIO_MEDICAMENTO",
                payload
        );
        
        log.info("Recordatorio enviado al adulto {} para el medicamento {}",
                horario.getMedicamento().getAdulto().getIdAdulto(),
                horario.getMedicamento().getNombre());
    }
}
