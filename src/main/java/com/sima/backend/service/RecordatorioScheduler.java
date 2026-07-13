package com.sima.backend.service;

import com.sima.backend.entity.Alerta;
import com.sima.backend.entity.HorarioMedicamento;
import com.sima.backend.entity.RelacionUsuarioAdulto;
import com.sima.backend.entity.RegistroToma;
import com.sima.backend.repository.AlertaRepository;
import com.sima.backend.repository.HorarioMedicamentoRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import com.sima.backend.repository.RegistroTomaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final RegistroTomaRepository registroTomaRepository;

    public RecordatorioScheduler(HorarioMedicamentoRepository horarioRepository,
                                 NotificationService notificationService,
                                 AlertaRepository alertaRepository,
                                 RelacionUsuarioAdultoRepository relacionRepository,
                                 RegistroTomaRepository registroTomaRepository) {
        this.horarioRepository = horarioRepository;
        this.notificationService = notificationService;
        this.alertaRepository = alertaRepository;
        this.relacionRepository = relacionRepository;
        this.registroTomaRepository = registroTomaRepository;
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

    @Scheduled(cron = "0 0/15 * * * *") // Se ejecuta cada 15 minutos
    @Transactional
    public void verificarTomasOmitidas() {
        // Tolerancia de 30 minutos (aprobada en HU-07)
        LocalDateTime limite = LocalDateTime.now().minusMinutes(30);
        log.debug("Verificando tomas omitidas anteriores a: {}", limite);

        List<RegistroToma> tomasVencidas = registroTomaRepository.findTomasPendientesVencidas(limite);

        for (RegistroToma toma : tomasVencidas) {
            try {
                // Marcar como omitida
                toma.setEstado("omitido");
                registroTomaRepository.save(toma);

                enviarAlertaOmitida(toma);
            } catch (Exception e) {
                log.error("Error al procesar la toma omitida con ID {}", toma.getIdRegistro(), e);
            }
        }
    }

    private void enviarAlertaOmitida(RegistroToma toma) {
        Integer idAdulto = toma.getAdulto().getIdAdulto();

        // 1. Crear registro de Alerta en DB
        Alerta alerta = new Alerta();
        alerta.setTipoAlerta("DOSIS_NO_TOMADA");
        alerta.setMensaje("Toma omitida: " + toma.getHorario().getMedicamento().getNombre()
                + " programada para las " + toma.getFechaHoraProgramada().toLocalTime());
        alerta.setAdulto(toma.getAdulto());
        alerta.setRegistro(toma);
        alertaRepository.save(alerta);

        // 2. Construir payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("idRegistro", toma.getIdRegistro());
        payload.put("nombre", toma.getHorario().getMedicamento().getNombre());
        payload.put("horaProgramada", toma.getFechaHoraProgramada().toString());
        payload.put("idAlerta", alerta.getIdAlerta());
        payload.put("idAdulto", idAdulto);

        // 3. Enviar a familiares
        List<RelacionUsuarioAdulto> relaciones = relacionRepository
                .findByAdulto_IdAdultoAndTipoRelacion(idAdulto, "familiar");

        if (relaciones.isEmpty()) {
            log.warn("No hay Familiares vinculados al adulto {} para enviar alerta de omisión.", idAdulto);
            return;
        }

        for (RelacionUsuarioAdulto relacion : relaciones) {
            Integer idUsuario = relacion.getUsuario().getIdUsuario();
            notificationService.sendNotification(idUsuario, "DOSIS_NO_TOMADA", payload);
            log.info("Alerta de dosis omitida enviada al usuario {} (adulto {}) para {}",
                    idUsuario, idAdulto, toma.getHorario().getMedicamento().getNombre());
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
