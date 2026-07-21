package com.sima.backend.service;

import com.sima.backend.dto.request.SolicitudVinculacionRequest;
import com.sima.backend.dto.response.CuidadorPublicResponse;
import com.sima.backend.dto.response.SolicitudVinculacionResponse;
import com.sima.backend.entity.AdultoMayor;
import com.sima.backend.entity.RelacionUsuarioAdulto;
import com.sima.backend.entity.SolicitudVinculacion;
import com.sima.backend.entity.Usuario;
import com.sima.backend.exception.ResourceNotFoundException;
import com.sima.backend.repository.AdultoMayorRepository;
import com.sima.backend.repository.PerfilCuidadorRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import com.sima.backend.repository.SolicitudVinculacionRepository;
import com.sima.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VinculacionService {

    private final SolicitudVinculacionRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final AdultoMayorRepository adultoRepository;
    private final RelacionUsuarioAdultoRepository relacionRepository;
    private final NotificationService notificationService;
    private final PerfilCuidadorRepository perfilCuidadorRepository;

    public VinculacionService(SolicitudVinculacionRepository solicitudRepository,
                              UsuarioRepository usuarioRepository,
                              AdultoMayorRepository adultoRepository,
                              RelacionUsuarioAdultoRepository relacionRepository,
                              NotificationService notificationService,
                              PerfilCuidadorRepository perfilCuidadorRepository) {
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
        this.adultoRepository = adultoRepository;
        this.relacionRepository = relacionRepository;
        this.notificationService = notificationService;
        this.perfilCuidadorRepository = perfilCuidadorRepository;
    }

    public List<CuidadorPublicResponse> listarCuidadoresDisponibles() {
        return usuarioRepository.findByRol_NombreRolAndActivoTrue("Cuidador").stream()
                .map(u -> CuidadorPublicResponse.from(u, perfilCuidadorRepository.findByIdUsuario(u.getIdUsuario()).orElse(null)))
                .collect(Collectors.toList());
    }

    @Transactional
    public SolicitudVinculacionResponse enviarSolicitud(SolicitudVinculacionRequest request, Integer idFamiliar) {
        Usuario familiar = usuarioRepository.findById(idFamiliar)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario (Familiar)", "id", idFamiliar));
        Usuario cuidador = usuarioRepository.findById(request.getIdCuidador())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario (Cuidador)", "id", request.getIdCuidador()));
        AdultoMayor adulto = adultoRepository.findById(request.getIdAdulto())
                .orElseThrow(() -> new ResourceNotFoundException("Adulto Mayor", "id", request.getIdAdulto()));

        // Check if there is already a pending request
        if (solicitudRepository.existsByFamiliar_IdUsuarioAndCuidador_IdUsuarioAndAdulto_IdAdultoAndEstado(
                idFamiliar, cuidador.getIdUsuario(), adulto.getIdAdulto(), "pendiente")) {
            throw new RuntimeException("Ya existe una solicitud pendiente para este cuidador y adulto mayor.");
        }

        SolicitudVinculacion solicitud = new SolicitudVinculacion();
        solicitud.setFamiliar(familiar);
        solicitud.setCuidador(cuidador);
        solicitud.setAdulto(adulto);

        solicitud = solicitudRepository.save(solicitud);

        // Notificar al cuidador
        try {
            notificationService.sendNotification(cuidador.getIdUsuario(), "NUEVA_SOLICITUD", 
                    "Tienes una nueva solicitud de cuidado para " + adulto.getNombre());
        } catch (Exception e) {
            // Log but don't fail transaction if push fails
        }

        return SolicitudVinculacionResponse.from(solicitud);
    }

    public List<SolicitudVinculacionResponse> listarSolicitudesPendientes(Integer idCuidador) {
        return solicitudRepository.findByCuidador_IdUsuarioAndEstadoOrderByFechaCreacionDesc(idCuidador, "pendiente")
                .stream()
                .map(SolicitudVinculacionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public SolicitudVinculacionResponse responderSolicitud(Integer idSolicitud, boolean aceptar, Integer idCuidador) {
        SolicitudVinculacion solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud", "id", idSolicitud));

        if (!solicitud.getCuidador().getIdUsuario().equals(idCuidador)) {
            throw new RuntimeException("No tienes permiso para responder a esta solicitud.");
        }

        if (!"pendiente".equals(solicitud.getEstado())) {
            throw new RuntimeException("Esta solicitud ya fue respondida.");
        }

        solicitud.setEstado(aceptar ? "aceptada" : "rechazada");
        solicitud.setFechaRespuesta(LocalDateTime.now());
        solicitudRepository.save(solicitud);

        if (aceptar) {
            // Crear relación
            RelacionUsuarioAdulto relacion = new RelacionUsuarioAdulto();
            RelacionUsuarioAdulto.RelacionUsuarioAdultoId relacionId = new RelacionUsuarioAdulto.RelacionUsuarioAdultoId();
            relacionId.setIdUsuario(idCuidador);
            relacionId.setIdAdulto(solicitud.getAdulto().getIdAdulto());
            
            relacion.setId(relacionId);
            relacion.setUsuario(solicitud.getCuidador());
            relacion.setAdulto(solicitud.getAdulto());
            relacion.setTipoRelacion("cuidador_asignado");
            relacion.setEsContactoEmergencia(false);

            relacionRepository.save(relacion);
        }

        // Notificar al familiar
        String mensaje = aceptar ? "El cuidador " + solicitud.getCuidador().getNombre() + " ha aceptado tu solicitud."
                                 : "El cuidador " + solicitud.getCuidador().getNombre() + " ha rechazado tu solicitud.";
        try {
            notificationService.sendNotification(solicitud.getFamiliar().getIdUsuario(), 
                    aceptar ? "SOLICITUD_ACEPTADA" : "SOLICITUD_RECHAZADA", mensaje);
        } catch (Exception e) {
            // Log but ignore
        }

        return SolicitudVinculacionResponse.from(solicitud);
    }
}
