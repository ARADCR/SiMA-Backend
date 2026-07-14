package com.sima.backend.service;

import com.sima.backend.dto.request.ObservacionRequest;
import com.sima.backend.dto.response.ObservacionResponse;
import com.sima.backend.entity.AdultoMayor;
import com.sima.backend.entity.ObservacionCuidador;
import com.sima.backend.entity.Usuario;
import com.sima.backend.exception.ResourceNotFoundException;
import com.sima.backend.exception.UnauthorizedException;
import com.sima.backend.repository.AdultoMayorRepository;
import com.sima.backend.repository.ObservacionCuidadorRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import com.sima.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ObservacionService {

    private final ObservacionCuidadorRepository observacionRepository;
    private final AdultoMayorRepository adultoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RelacionUsuarioAdultoRepository relacionRepository;

    public ObservacionService(ObservacionCuidadorRepository observacionRepository,
            AdultoMayorRepository adultoRepository,
            UsuarioRepository usuarioRepository,
            RelacionUsuarioAdultoRepository relacionRepository) {
        this.observacionRepository = observacionRepository;
        this.adultoRepository = adultoRepository;
        this.usuarioRepository = usuarioRepository;
        this.relacionRepository = relacionRepository;
    }

    @Transactional
    public ObservacionResponse registrarObservacion(Integer idUsuario, ObservacionRequest request) {
        validarAcceso(idUsuario, request.getIdAdulto());

        AdultoMayor adulto = adultoRepository.findByIdAdultoAndActivoTrue(request.getIdAdulto())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Adulto mayor", "id", request.getIdAdulto()));

        Usuario cuidador = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario", "id", idUsuario));

        ObservacionCuidador observacion = new ObservacionCuidador();
        observacion.setAdulto(adulto);
        observacion.setCuidador(cuidador);
        observacion.setUrgencia(request.getUrgencia());
        observacion.setTexto(request.getTexto());
        observacion.setTensionArterial(request.getTensionArterial());
        observacion.setFrecuenciaCardiaca(request.getFrecuenciaCardiaca());
        observacion.setTemperatura(request.getTemperatura());

        observacion = observacionRepository.save(observacion);
        return ObservacionResponse.from(observacion);
    }

    @Transactional(readOnly = true)
    public List<ObservacionResponse> listarPorAdulto(Integer idUsuario, Integer idAdulto) {
        validarAcceso(idUsuario, idAdulto);

        return observacionRepository.findByAdulto_IdAdultoOrderByFechaHoraDesc(idAdulto)
                .stream()
                .map(ObservacionResponse::from)
                .toList();
    }

    private void validarAcceso(Integer idUsuario, Integer idAdulto) {
        if (!relacionRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)) {
            throw new UnauthorizedException(
                    "No tienes acceso al adulto mayor con id: " + idAdulto);
        }
    }
}
