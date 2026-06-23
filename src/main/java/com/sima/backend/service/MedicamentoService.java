package com.sima.backend.service;

import com.sima.backend.dto.request.MedicamentoRequest;
import com.sima.backend.dto.response.MedicamentoResponse;
import com.sima.backend.entity.AdultoMayor;
import com.sima.backend.entity.HorarioMedicamento;
import com.sima.backend.entity.Medicamento;
import com.sima.backend.exception.BadRequestException;
import com.sima.backend.exception.ResourceNotFoundException;
import com.sima.backend.exception.UnauthorizedException;
import com.sima.backend.repository.AdultoMayorRepository;
import com.sima.backend.repository.HorarioMedicamentoRepository;
import com.sima.backend.repository.MedicamentoRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * HU-08: Agregar o editar la agenda de medicamentos del adulto mayor.
 * Familiar: puede crear y desactivar medicamentos.
 * Cuidador: puede ajustar horarios.
 */
@Service
public class MedicamentoService {

    private final MedicamentoRepository medicamentoRepository;
    private final HorarioMedicamentoRepository horarioRepository;
    private final AdultoMayorRepository adultoRepository;
    private final RelacionUsuarioAdultoRepository relacionRepository;

    public MedicamentoService(MedicamentoRepository medicamentoRepository,
            HorarioMedicamentoRepository horarioRepository,
            AdultoMayorRepository adultoRepository,
            RelacionUsuarioAdultoRepository relacionRepository) {
        this.medicamentoRepository = medicamentoRepository;
        this.horarioRepository = horarioRepository;
        this.adultoRepository = adultoRepository;
        this.relacionRepository = relacionRepository;
    }

    // Listar medicamentos activos de un adulto
    @Transactional(readOnly = true)
    public List<MedicamentoResponse> listarActivosPorAdulto(Integer idAdulto, Integer idUsuario) {
        validarAcceso(idUsuario, idAdulto);
        return medicamentoRepository.findByAdulto_IdAdultoAndActivoTrue(idAdulto)
                .stream()
                .map(MedicamentoResponse::from)
                .toList();
    }

    // Crear medicamento con sus horarios (HU-08) - en una sola transacción
    @Transactional
    public MedicamentoResponse crear(MedicamentoRequest request, Integer idUsuario) {
        validarAcceso(idUsuario, request.getIdAdulto());

        AdultoMayor adulto = adultoRepository.findByIdAdultoAndActivoTrue(request.getIdAdulto())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Adulto mayor", "id", request.getIdAdulto()));

        // Crear medicamento
        Medicamento medicamento = new Medicamento();
        medicamento.setAdulto(adulto);
        medicamento.setNombre(request.getNombre());
        medicamento.setDosis(request.getDosis());
        medicamento.setFrecuenciaHoras(request.getFrecuenciaHoras());
        medicamento.setObservaciones(request.getObservaciones());
        medicamento.setActivo(true);

        medicamento = medicamentoRepository.save(medicamento);

        // Crear horarios en la misma transacción
        for (var horarioReq : request.getHorarios()) {
            // Verificar que no haya horarios duplicados en el mismo request
            boolean duplicado = horarioRepository
                    .existsByMedicamento_IdMedicamentoAndHoraProgramada(
                            medicamento.getIdMedicamento(), horarioReq.getHoraProgramada());
            if (duplicado) {
                throw new BadRequestException(
                        "Horario duplicado: " + horarioReq.getHoraProgramada());
            }

            HorarioMedicamento horario = new HorarioMedicamento();
            horario.setMedicamento(medicamento);
            horario.setHoraProgramada(horarioReq.getHoraProgramada());
            horario.setActivo(true);
            horarioRepository.save(horario);
        }

        // Recargar con horarios para la respuesta
        return MedicamentoResponse.from(
                medicamentoRepository.findById(medicamento.getIdMedicamento()).orElseThrow());
    }

    // Actualizar datos del medicamento (HU-08)
    @Transactional
    public MedicamentoResponse actualizar(Integer idMedicamento,
            MedicamentoRequest request,
            Integer idUsuario) {
        Medicamento medicamento = medicamentoRepository.findByIdMedicamentoAndActivoTrue(idMedicamento)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Medicamento", "id", idMedicamento));

        validarAcceso(idUsuario, medicamento.getAdulto().getIdAdulto());

        medicamento.setNombre(request.getNombre());
        medicamento.setDosis(request.getDosis());
        medicamento.setFrecuenciaHoras(request.getFrecuenciaHoras());
        medicamento.setObservaciones(request.getObservaciones());

        return MedicamentoResponse.from(medicamentoRepository.save(medicamento));
    }

    // Desactivar medicamento - soft delete (HU-08)
    @Transactional
    public void desactivar(Integer idMedicamento, Integer idUsuario) {
        Medicamento medicamento = medicamentoRepository.findById(idMedicamento)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Medicamento", "id", idMedicamento));

        validarAcceso(idUsuario, medicamento.getAdulto().getIdAdulto());

        // Desactivar el medicamento y todos sus horarios
        medicamentoRepository.desactivarMedicamento(idMedicamento);
        horarioRepository.desactivarHorariosByMedicamento(idMedicamento);
    }

    // ---------------------------------------------------------------
    // Validación RBAC a nivel de datos
    // ---------------------------------------------------------------
    private void validarAcceso(Integer idUsuario, Integer idAdulto) {
        if (!relacionRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)) {
            throw new UnauthorizedException(
                    "No tienes acceso a los medicamentos del adulto con id: " + idAdulto);
        }
    }
}