package com.sima.backend.service;

import com.sima.backend.dto.request.RegistroTomaRequest;
import com.sima.backend.dto.response.RegistroTomaResponse;
import com.sima.backend.entity.RegistroToma;
import com.sima.backend.exception.BadRequestException;
import com.sima.backend.exception.ResourceNotFoundException;
import com.sima.backend.exception.UnauthorizedException;
import com.sima.backend.repository.AlertaRepository;
import com.sima.backend.repository.RegistroTomaRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * HU-01: Recibir recordatorio de medicamento.
 * HU-02: Confirmar toma desde la app o chatbot.
 * HU-13: Registrar manualmente toma (cuidador).
 */
@Service
public class RegistroTomaService {

    private final RegistroTomaRepository registroRepository;
    private final RelacionUsuarioAdultoRepository relacionRepository;
    private final AlertaRepository alertaRepository;

    public RegistroTomaService(RegistroTomaRepository registroRepository,
            RelacionUsuarioAdultoRepository relacionRepository,
            AlertaRepository alertaRepository) {
        this.registroRepository = registroRepository;
        this.relacionRepository = relacionRepository;
        this.alertaRepository = alertaRepository;
    }

    // Obtener tomas del día para un adulto (HU-01 - base del recordatorio)
    @Transactional(readOnly = true)
    public List<RegistroTomaResponse> listarTomasDelDia(Integer idAdulto, Integer idUsuario) {
        validarAcceso(idUsuario, idAdulto);

        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia = inicioDia.plusDays(1);

        return registroRepository.findTomasDelDia(idAdulto, inicioDia, finDia)
                .stream()
                .map(RegistroTomaResponse::from)
                .toList();
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
    // ---------------------------------------------------------------
    private void validarAcceso(Integer idUsuario, Integer idAdulto) {
        if (!relacionRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)) {
            throw new UnauthorizedException(
                    "No tienes acceso a los registros del adulto con id: " + idAdulto);
        }
    }
}