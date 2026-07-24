package com.sima.backend.service;

import com.sima.backend.dto.request.AdultoMayorRequest;
import com.sima.backend.dto.response.AdultoMayorResponse;
import com.sima.backend.entity.AdultoMayor;
import com.sima.backend.entity.RelacionUsuarioAdulto;
import com.sima.backend.entity.Usuario;
import com.sima.backend.exception.ResourceNotFoundException;
import com.sima.backend.exception.UnauthorizedException;
import com.sima.backend.repository.AdultoMayorRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import com.sima.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * HU-12: Registrar o actualizar datos personales del adulto mayor.
 * El Familiar valida que el adulto esté bajo su tutela antes de modificar.
 */
@Service
public class AdultoMayorService {

    private final AdultoMayorRepository adultoRepository;
    private final RelacionUsuarioAdultoRepository relacionRepository;
    private final UsuarioRepository usuarioRepository;

    public AdultoMayorService(AdultoMayorRepository adultoRepository,
            RelacionUsuarioAdultoRepository relacionRepository,
            UsuarioRepository usuarioRepository) {
        this.adultoRepository = adultoRepository;
        this.relacionRepository = relacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // Listar adultos asignados al usuario autenticado
    @Transactional(readOnly = true)
    public List<AdultoMayorResponse> listarPorUsuario(Integer idUsuario) {
        return adultoRepository.findByUsuarioId(idUsuario)
                .stream()
                .map(AdultoMayorResponse::from)
                .toList();
    }

    // Listar todos los adultos mayores con info de familiar (para uso de Administradores)
    @Transactional(readOnly = true)
    public List<AdultoMayorResponse> listarTodos() {
        return adultoRepository.findAllWithRelaciones()
                .stream()
                .map(AdultoMayorResponse::from)
                .toList();
    }

    // Buscar adulto por ID validando acceso del usuario (RBAC a nivel de dato)
    @Transactional(readOnly = true)
    public AdultoMayorResponse buscarPorId(Integer idAdulto, Integer idUsuario) {
        validarAcceso(idUsuario, idAdulto);
        AdultoMayor adulto = adultoRepository.findByIdAdultoAndActivoTrue(idAdulto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Adulto mayor", "id", idAdulto));
        return AdultoMayorResponse.from(adulto);
    }

    // Registrar nuevo adulto mayor y vincularlo al familiar (HU-12)
    @Transactional
    public AdultoMayorResponse registrar(AdultoMayorRequest request, Integer idUsuario) {
        AdultoMayor adulto = new AdultoMayor();
        adulto.setNombre(request.getNombre());
        adulto.setApellido(request.getApellido());
        adulto.setFechaNacimiento(request.getFechaNacimiento());
        adulto.setCondicionesMedicas(request.getCondicionesMedicas());
        adulto.setContactoMedico(request.getContactoMedico());
        adulto.setActivo(true);

        adulto = adultoRepository.save(adulto);

        // Vincular automáticamente al familiar que lo registra
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario", "id", idUsuario));

        RelacionUsuarioAdulto relacion = new RelacionUsuarioAdulto();
        RelacionUsuarioAdulto.RelacionUsuarioAdultoId relacionId = new RelacionUsuarioAdulto.RelacionUsuarioAdultoId();
        relacionId.setIdUsuario(idUsuario);
        relacionId.setIdAdulto(adulto.getIdAdulto());

        relacion.setId(relacionId);
        relacion.setUsuario(usuario);
        relacion.setAdulto(adulto);
        relacion.setTipoRelacion("familiar");
        relacion.setEsContactoEmergencia(true); // El familiar que registra es contacto de emergencia

        relacionRepository.save(relacion);

        return AdultoMayorResponse.from(adulto);
    }

    // Actualizar datos del adulto mayor (HU-12)
    @Transactional
    public AdultoMayorResponse actualizar(Integer idAdulto,
            AdultoMayorRequest request,
            Integer idUsuario) {
        validarAcceso(idUsuario, idAdulto);

        AdultoMayor adulto = adultoRepository.findByIdAdultoAndActivoTrue(idAdulto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Adulto mayor", "id", idAdulto));

        adulto.setNombre(request.getNombre());
        adulto.setApellido(request.getApellido());
        adulto.setFechaNacimiento(request.getFechaNacimiento());
        adulto.setCondicionesMedicas(request.getCondicionesMedicas());
        adulto.setContactoMedico(request.getContactoMedico());

        return AdultoMayorResponse.from(adultoRepository.save(adulto));
    }

    // Eliminar (soft delete) adulto mayor — solo Admin
    @Transactional
    public void eliminar(Integer idAdulto) {
        AdultoMayor adulto = adultoRepository.findById(idAdulto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Adulto mayor", "id", idAdulto));
        adulto.setActivo(false);
        adultoRepository.save(adulto);
    }

    // Reactivar adulto mayor — solo Admin
    @Transactional
    public void reactivar(Integer idAdulto) {
        AdultoMayor adulto = adultoRepository.findById(idAdulto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Adulto mayor", "id", idAdulto));
        adulto.setActivo(true);
        adultoRepository.save(adulto);
    }

    // ---------------------------------------------------------------
    // Validación RBAC a nivel de datos:
    // verifica que el usuario tenga acceso al adulto específico
    // ---------------------------------------------------------------
    private void validarAcceso(Integer idUsuario, Integer idAdulto) {
        if (!relacionRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)) {
            throw new UnauthorizedException(
                    "No tienes acceso al adulto mayor con id: " + idAdulto);
        }
    }
}