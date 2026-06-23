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
        List<AdultoMayor> adultos = adultoRepository.findByUsuarioId(idUsuario);

        // DEMO FALLBACK: Si es el usuario de demostración 'Adulto Mayor' (idUsuario=4)
        // y no tiene relaciones formales, le retornamos el primer adulto mayor creado
        // para que pueda ver sus propios recordatorios en la demostración.
        if (adultos.isEmpty() && idUsuario != null && idUsuario == 4) {
            List<AdultoMayor> todos = adultoRepository.findByActivoTrue();
            if (!todos.isEmpty()) {
                adultos = List.of(todos.get(0));
            }
        }

        return adultos.stream()
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