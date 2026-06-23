package com.sima.backend.service;

import com.sima.backend.dto.request.AsignarDispositivoRequest;
import com.sima.backend.dto.request.DispositivoIotRequest;
import com.sima.backend.dto.response.DispositivoIotResponse;
import com.sima.backend.entity.AdultoMayor;
import com.sima.backend.entity.DispositivoIot;
import com.sima.backend.exception.BadRequestException;
import com.sima.backend.exception.ResourceNotFoundException;
import com.sima.backend.repository.AdultoMayorRepository;
import com.sima.backend.repository.DispositivoIotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * HU-18: Registrar y asignar dispositivos IoT a un adulto mayor.
 * Solo el Administrador puede ejecutar estas operaciones.
 */
@Service
public class DispositivoIotService {

    private final DispositivoIotRepository dispositivoRepository;
    private final AdultoMayorRepository adultoRepository;

    public DispositivoIotService(DispositivoIotRepository dispositivoRepository,
            AdultoMayorRepository adultoRepository) {
        this.dispositivoRepository = dispositivoRepository;
        this.adultoRepository = adultoRepository;
    }

    // Listar todos los dispositivos activos
    @Transactional(readOnly = true)
    public List<DispositivoIotResponse> listarTodos() {
        return dispositivoRepository.findByActivoTrue()
                .stream()
                .map(DispositivoIotResponse::from)
                .toList();
    }

    // Listar dispositivos sin asignar
    @Transactional(readOnly = true)
    public List<DispositivoIotResponse> listarSinAsignar() {
        return dispositivoRepository.findByAdultoIsNullAndActivoTrue()
                .stream()
                .map(DispositivoIotResponse::from)
                .toList();
    }

    // Listar dispositivos de un adulto mayor
    @Transactional(readOnly = true)
    public List<DispositivoIotResponse> listarPorAdulto(Integer idAdulto) {
        validarAdultoExiste(idAdulto);
        return dispositivoRepository.findByAdulto_IdAdultoAndActivoTrue(idAdulto)
                .stream()
                .map(DispositivoIotResponse::from)
                .toList();
    }

    // Registrar nuevo dispositivo (HU-18)
    @Transactional
    public DispositivoIotResponse registrar(DispositivoIotRequest request) {
        // Validar que el identificador físico sea único
        if (dispositivoRepository.existsByIdentificadorFisico(request.getIdentificadorFisico())) {
            throw new BadRequestException(
                    "Ya existe un dispositivo con el identificador: " +
                            request.getIdentificadorFisico());
        }

        DispositivoIot dispositivo = new DispositivoIot();
        dispositivo.setIdentificadorFisico(request.getIdentificadorFisico());
        dispositivo.setTipoDispositivo(request.getTipoDispositivo());
        dispositivo.setActivo(true);

        // Si viene idAdulto en el request, asignar directamente
        if (request.getIdAdulto() != null) {
            asignarAdultoADispositivo(dispositivo, request.getIdAdulto());
        }

        return DispositivoIotResponse.from(dispositivoRepository.save(dispositivo));
    }

    // Asignar dispositivo a adulto mayor (HU-18)
    @Transactional
    public DispositivoIotResponse asignar(Integer idDispositivo, AsignarDispositivoRequest request) {
        DispositivoIot dispositivo = dispositivoRepository.findById(idDispositivo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dispositivo", "id", idDispositivo));

        asignarAdultoADispositivo(dispositivo, request.getIdAdulto());

        return DispositivoIotResponse.from(dispositivoRepository.save(dispositivo));
    }

    // Desasignar dispositivo de su adulto actual
    @Transactional
    public DispositivoIotResponse desasignar(Integer idDispositivo) {
        if (!dispositivoRepository.existsById(idDispositivo)) {
            throw new ResourceNotFoundException("Dispositivo", "id", idDispositivo);
        }
        dispositivoRepository.desasignarDispositivo(idDispositivo);
        return DispositivoIotResponse.from(
                dispositivoRepository.findById(idDispositivo).orElseThrow());
    }

    // ---------------------------------------------------------------
    // Método privado: valida y asigna el adulto al dispositivo
    // ---------------------------------------------------------------
    private void asignarAdultoADispositivo(DispositivoIot dispositivo, Integer idAdulto) {
        AdultoMayor adulto = adultoRepository.findByIdAdultoAndActivoTrue(idAdulto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Adulto mayor", "id", idAdulto));

        // Validar que el adulto no tenga ya un dispositivo del mismo tipo
        dispositivoRepository.findByAdulto_IdAdultoAndTipoDispositivo(
                idAdulto, dispositivo.getTipoDispositivo())
                .ifPresent(d -> {
                    throw new BadRequestException(
                            "El adulto mayor ya tiene asignado un dispositivo de tipo: " +
                                    dispositivo.getTipoDispositivo());
                });

        dispositivo.setAdulto(adulto);
    }

    private void validarAdultoExiste(Integer idAdulto) {
        if (!adultoRepository.existsById(idAdulto)) {
            throw new ResourceNotFoundException("Adulto mayor", "id", idAdulto);
        }
    }
}