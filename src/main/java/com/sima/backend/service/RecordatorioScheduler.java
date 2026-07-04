package com.sima.backend.service;

import com.sima.backend.entity.Alerta;
import com.sima.backend.entity.HorarioMedicamento;
import com.sima.backend.entity.RelacionUsuarioAdulto;
import com.sima.backend.repository.AlertaRepository;
import com.sima.backend.repository.HorarioMedicamentoRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
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
    private final RelacionUsuarioAdultoRepository relacionRepository;

    public RecordatorioScheduler(HorarioMedicamentoRepository horarioRepository,
                                 NotificationService notificationService,
                                 AlertaRepository alertaRepository,
                                 RelacionUsuarioAdultoRepository relacionRepository) {
        this.horarioRepository = horarioRepository;
        this.notificationService = notificationService;
        this.alertaRepository = alertaRepository;
        this.relacionRepository = relacionRepository;
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
        Integer idAdulto = horario.getMedicamento().getAdulto().getIdAdulto();

        // 1. Crear registro de Alerta en DB
        Alerta alerta = new Alerta();
        alerta.setTipoAlerta("RECORDATORIO_MEDICAMENTO");
        alerta.setMensaje("Hora de tomar: " + horario.getMedicamento().getNombre()
                + " - Dosis: " + horario.getMedicamento().getDosis());
        alerta.setAdulto(horario.getMedicamento().getAdulto());
        alertaRepository.save(alerta);

        // 2. Construir payload de la notificación
        Map<String, Object> payload = new HashMap<>();
        payload.put("idMedicamento", horario.getMedicamento().getIdMedicamento());
        payload.put("nombre", horario.getMedicamento().getNombre());
        payload.put("dosis", horario.getMedicamento().getDosis());
        payload.put("hora", horario.getHoraProgramada().toString());
        payload.put("idAlerta", alerta.getIdAlerta());
        payload.put("idAdulto", idAdulto);

        // 3. Enviar notificación SSE a todos los Familiares y Cuidadores vinculados al adulto.
        //    El adulto mayor ya no es un usuario del sistema, por lo que la notificación
        //    se dirige a quienes gestionan su atención.
        List<RelacionUsuarioAdulto> relaciones = relacionRepository
                .findByAdulto_IdAdultoAndTipoRelacion(idAdulto, "familiar");
        relaciones.addAll(relacionRepository
                .findByAdulto_IdAdultoAndTipoRelacion(idAdulto, "cuidador_asignado"));

        if (relaciones.isEmpty()) {
            log.warn("No hay Familiares ni Cuidadores vinculados al adulto {} para enviar el recordatorio.", idAdulto);
            return;
        }

        for (RelacionUsuarioAdulto relacion : relaciones) {
            Integer idUsuario = relacion.getUsuario().getIdUsuario();
            notificationService.sendNotification(idUsuario, "RECORDATORIO_MEDICAMENTO", payload);
            log.info("Recordatorio enviado al usuario {} (adulto {}) para el medicamento {}",
                    idUsuario, idAdulto, horario.getMedicamento().getNombre());
        }
    }
}
