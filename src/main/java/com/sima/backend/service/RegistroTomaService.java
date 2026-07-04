package com.sima.backend.service;

import com.sima.backend.dto.request.RegistroTomaRequest;
import com.sima.backend.dto.response.RegistroTomaResponse;
import com.sima.backend.entity.RegistroToma;
import com.sima.backend.exception.BadRequestException;
import com.sima.backend.exception.ResourceNotFoundException;
import com.sima.backend.exception.UnauthorizedException;
import com.sima.backend.repository.AlertaRepository;
import com.sima.backend.repository.HorarioMedicamentoRepository;
import com.sima.backend.repository.RegistroTomaRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * HU-01: Recibir recordatorio de medicamento (receptor: Familiar/Cuidador).
 * HU-02: Confirmar toma — responsabilidad del Familiar o Cuidador.
 * HU-13: Registrar manualmente toma (cuidador).
 */
@Service
public class RegistroTomaService {

    private final RegistroTomaRepository registroRepository;
    private final RelacionUsuarioAdultoRepository relacionRepository;
    private final AlertaRepository alertaRepository;

    private final HorarioMedicamentoRepository horarioRepository;

    public RegistroTomaService(RegistroTomaRepository registroRepository,
            RelacionUsuarioAdultoRepository relacionRepository,
            AlertaRepository alertaRepository,
            HorarioMedicamentoRepository horarioRepository) {
        this.registroRepository = registroRepository;
        this.relacionRepository = relacionRepository;
        this.alertaRepository = alertaRepository;
        this.horarioRepository = horarioRepository;
    }

    // Obtener tomas del día para un adulto (HU-01 - base del recordatorio)
    @Transactional
    public List<RegistroTomaResponse> listarTomasDelDia(Integer idAdulto, Integer idUsuario) {
        validarAcceso(idUsuario, idAdulto);

        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia = inicioDia.plusDays(1);

        List<RegistroToma> tomasHoy = registroRepository.findTomasDelDia(idAdulto, inicioDia, finDia);

        // DEMO AUTOGENERATOR: Si no hay tomas para hoy, las generamos al vuelo.
        if (tomasHoy.isEmpty()) {
            List<com.sima.backend.entity.HorarioMedicamento> horarios = horarioRepository.findHorariosActivosByAdulto(idAdulto);
            for (com.sima.backend.entity.HorarioMedicamento h : horarios) {
                RegistroToma nuevaToma = new RegistroToma();
                nuevaToma.setHorario(h);
                nuevaToma.setAdulto(h.getMedicamento().getAdulto());
                nuevaToma.setEstado("pendiente");
                nuevaToma.setFechaHoraProgramada(LocalDateTime.of(LocalDate.now(), h.getHoraProgramada()));
                registroRepository.save(nuevaToma);
            }
            if (!horarios.isEmpty()) {
                tomasHoy = registroRepository.findTomasDelDia(idAdulto, inicioDia, finDia);
            }
        }

        return tomasHoy.stream().map(RegistroTomaResponse::from).toList();
    }

    // Obtener próxima toma pendiente de un adulto (HU-04 - chatbot)
    @Transactional(readOnly = true)
    public RegistroTomaResponse obtenerProximaToma(Integer idAdulto, Integer idUsuario) {
        validarAcceso(idUsuario, idAdulto);

        return registroRepository.findProximaToma(idAdulto, LocalDateTime.now())
                .map(RegistroTomaResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay tomas pendientes programadas para el adulto con id: " + idAdulto));
    }

    // Historial de tomas de un adulto en un rango de fechas
    @Transactional(readOnly = true)
    public List<RegistroTomaResponse> listarHistorial(Integer idAdulto,
            Integer idUsuario,
            LocalDateTime desde,
            LocalDateTime hasta) {
        validarAcceso(idUsuario, idAdulto);

        return registroRepository.findHistorialByAdultoAndRango(idAdulto, desde, hasta)
                .stream()
                .map(RegistroTomaResponse::from)
                .toList();
    }

    // Confirmar toma de medicamento (HU-02 / HU-13)
    @Transactional
    public RegistroTomaResponse confirmarToma(RegistroTomaRequest request, Integer idUsuario) {
        RegistroToma registro = registroRepository.findById(request.getIdRegistro())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registro de toma", "id", request.getIdRegistro()));

        // Validar acceso al adulto del registro
        validarAcceso(idUsuario, registro.getAdulto().getIdAdulto());

        // Solo se puede confirmar una toma pendiente
        if (!"pendiente".equals(registro.getEstado())) {
            throw new BadRequestException(
                    "La toma ya fue registrada con estado: " + registro.getEstado());
        }

        // Actualizar registro
        int filasAfectadas = registroRepository.confirmarToma(
                request.getIdRegistro(),
                "tomado",
                request.getMetodoConfirmacion(),
                LocalDateTime.now(),
                idUsuario);

        if (filasAfectadas == 0) {
            throw new BadRequestException("No se pudo confirmar la toma. Puede que ya fue procesada.");
        }

        // Resolver alerta activa si existe para este registro
        if (alertaRepository.existsByRegistro_IdRegistroAndResueltaFalse(request.getIdRegistro())) {
            alertaRepository.findByAdulto_IdAdultoAndTipoAlertaAndResueltaFalse(
                    registro.getAdulto().getIdAdulto(), "omision_medicacion")
                    .forEach(a -> alertaRepository.resolverAlerta(a.getIdAlerta()));
        }

        return RegistroTomaResponse.from(
                registroRepository.findById(request.getIdRegistro()).orElseThrow());
    }

    // ---------------------------------------------------------------
    // Validación RBAC a nivel de datos
    // Solo permite acceso si el usuario (Familiar o Cuidador) tiene
    // una relación directa con el adulto en relacion_usuario_adulto.
    // ---------------------------------------------------------------
    private void validarAcceso(Integer idUsuario, Integer idAdulto) {
        if (!relacionRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)) {
            throw new UnauthorizedException(
                    "No tienes acceso a los registros del adulto con id: " + idAdulto);
        }
    }
}