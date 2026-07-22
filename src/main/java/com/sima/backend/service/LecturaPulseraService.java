package com.sima.backend.service;

import com.sima.backend.dto.request.LecturaPulseraRequest;
import com.sima.backend.dto.response.LecturaPulseraResponse;
import com.sima.backend.entity.AdultoMayor;
import com.sima.backend.entity.DispositivoIot;
import com.sima.backend.entity.LecturaPulsera;
import com.sima.backend.exception.BadRequestException;
import com.sima.backend.exception.ResourceNotFoundException;
import com.sima.backend.repository.AdultoMayorRepository;
import com.sima.backend.repository.DispositivoIotRepository;
import com.sima.backend.repository.LecturaPulseraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio para recibir y consultar lecturas de pulsera inteligente.
 * Las lecturas son enviadas desde la aplicación Android vía Bluetooth → servidor.
 */
@Service
public class LecturaPulseraService {

    private final LecturaPulseraRepository lecturaRepository;
    private final DispositivoIotRepository dispositivoRepository;
    private final AdultoMayorRepository adultoRepository;

    public LecturaPulseraService(LecturaPulseraRepository lecturaRepository,
                                 DispositivoIotRepository dispositivoRepository,
                                 AdultoMayorRepository adultoRepository) {
        this.lecturaRepository = lecturaRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.adultoRepository = adultoRepository;
    }

    /**
     * Guarda una lectura de pulsera inteligente.
     * Flujo:
     * 1. Buscar dispositivo por identificador físico (MAC).
     * 2. Verificar que esté activo.
     * 3. Verificar que sea tipo "pulsera_inteligente".
     * 4. Verificar que tenga un adulto mayor asignado.
     * 5. Crear y guardar la entidad.
     * 6. Devolver DTO de respuesta.
     */
    @Transactional
    public LecturaPulseraResponse guardarLectura(LecturaPulseraRequest request) {

        // 1. Buscar dispositivo por identificador físico
        DispositivoIot dispositivo = dispositivoRepository
                .findByIdentificadorFisico(request.getIdentificadorFisico())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dispositivo no registrado con identificador: " + request.getIdentificadorFisico()));

        // 2. Verificar que el dispositivo esté activo
        if (!Boolean.TRUE.equals(dispositivo.getActivo())) {
            throw new BadRequestException(
                    "El dispositivo con identificador '" + request.getIdentificadorFisico() + "' está inactivo");
        }

        // 3. Verificar que sea una pulsera inteligente
        if (!"pulsera_inteligente".equals(dispositivo.getTipoDispositivo())) {
            throw new BadRequestException(
                    "El dispositivo no es una pulsera inteligente. Tipo actual: " + dispositivo.getTipoDispositivo());
        }

        // 4. Verificar que tenga un adulto mayor asignado
        AdultoMayor adulto = dispositivo.getAdulto();
        if (adulto == null) {
            throw new BadRequestException(
                    "La pulsera con identificador '" + request.getIdentificadorFisico()
                            + "' no tiene un adulto mayor asignado");
        }

        // 5. Crear la entidad LecturaPulsera
        LecturaPulsera lectura = new LecturaPulsera();
        lectura.setDispositivo(dispositivo);
        lectura.setAdulto(adulto);
        lectura.setFrecuenciaCardiaca(request.getFrecuenciaCardiaca());
        lectura.setSpo2(request.getSpo2());
        lectura.setPresionSistolica(request.getPresionSistolica());
        lectura.setPresionDiastolica(request.getPresionDiastolica());
        lectura.setPasosDiarios(request.getPasosDiarios());
        lectura.setNivelBateria(request.getNivelBateria());
        lectura.setFechaMedicion(request.getFechaMedicion());
        // fechaRecepcion se asigna automáticamente por @CreationTimestamp

        // 6. Guardar
        LecturaPulsera guardada = lecturaRepository.save(lectura);

        return LecturaPulseraResponse.from(guardada, "Lectura guardada correctamente");
    }

    /**
     * Obtiene el historial de lecturas de un adulto mayor,
     * ordenado de la más reciente a la más antigua.
     */
    @Transactional(readOnly = true)
    public List<LecturaPulseraResponse> obtenerHistorialPorAdulto(Integer idAdulto) {
        // Validar que el adulto mayor exista
        if (!adultoRepository.existsById(idAdulto)) {
            throw new ResourceNotFoundException("Adulto mayor", "id", idAdulto);
        }

        return lecturaRepository.findByAdulto_IdAdultoOrderByFechaMedicionDesc(idAdulto)
                .stream()
                .map(LecturaPulseraResponse::from)
                .toList();
    }

    /**
     * Obtiene la lectura más reciente de un adulto mayor.
     */
    @Transactional(readOnly = true)
    public LecturaPulseraResponse obtenerUltimaLectura(Integer idAdulto) {
        // Validar que el adulto mayor exista
        if (!adultoRepository.existsById(idAdulto)) {
            throw new ResourceNotFoundException("Adulto mayor", "id", idAdulto);
        }

        LecturaPulsera lectura = lecturaRepository
                .findFirstByAdulto_IdAdultoOrderByFechaMedicionDesc(idAdulto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontraron lecturas para el adulto mayor con id: " + idAdulto));

        return LecturaPulseraResponse.from(lectura);
    }
}
