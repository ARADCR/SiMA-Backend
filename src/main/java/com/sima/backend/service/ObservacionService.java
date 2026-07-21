package com.sima.backend.service;

import com.sima.backend.dto.request.ObservacionRequest;
import com.sima.backend.dto.response.ObservacionResponse;
import com.sima.backend.entity.AdultoMayor;
import com.sima.backend.entity.Alerta;
import com.sima.backend.entity.ObservacionCuidador;
import com.sima.backend.entity.RelacionUsuarioAdulto;
import com.sima.backend.entity.Usuario;
import com.sima.backend.exception.ResourceNotFoundException;
import com.sima.backend.exception.UnauthorizedException;
import com.sima.backend.repository.AdultoMayorRepository;
import com.sima.backend.repository.AlertaRepository;
import com.sima.backend.repository.ObservacionCuidadorRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import com.sima.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ObservacionService {

    private static final Logger log = LoggerFactory.getLogger(ObservacionService.class);
    private static final String TIPO_ALERTA_URGENCIA_SIGNOS_VITALES = "urgencia_signos_vitales";

    private final ObservacionCuidadorRepository observacionRepository;
    private final AdultoMayorRepository adultoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RelacionUsuarioAdultoRepository relacionRepository;
    private final AlertaRepository alertaRepository;
    private final NotificationService notificationService;
    private final AlertaAiService alertaAiService;

    public ObservacionService(ObservacionCuidadorRepository observacionRepository,
            AdultoMayorRepository adultoRepository,
            UsuarioRepository usuarioRepository,
            RelacionUsuarioAdultoRepository relacionRepository,
            AlertaRepository alertaRepository,
            NotificationService notificationService,
            AlertaAiService alertaAiService) {
        this.observacionRepository = observacionRepository;
        this.adultoRepository = adultoRepository;
        this.usuarioRepository = usuarioRepository;
        this.relacionRepository = relacionRepository;
        this.alertaRepository = alertaRepository;
        this.notificationService = notificationService;
        this.alertaAiService = alertaAiService;
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

        // La IA sugiere, el Cuidador decide: solo se genera Alerta automática cuando la urgencia
        // sugerida por la IA fue "urgente" Y el Cuidador la aceptó explícitamente al guardar.
        if (Boolean.TRUE.equals(request.getSugerenciaIaAceptada()) && "urgente".equals(request.getUrgencia())) {
            crearAlertaUrgenciaSignosVitales(observacion);
        }

        return ObservacionResponse.from(observacion);
    }

    private void crearAlertaUrgenciaSignosVitales(ObservacionCuidador observacion) {
        AdultoMayor adulto = observacion.getAdulto();

        Alerta alerta = new Alerta();
        alerta.setTipoAlerta(TIPO_ALERTA_URGENCIA_SIGNOS_VITALES);
        alerta.setMensaje("Signos vitales urgentes registrados por "
                + observacion.getCuidador().getNombre() + " " + observacion.getCuidador().getApellido()
                + ": " + observacion.getTexto());
        alerta.setAdulto(adulto);
        alertaRepository.save(alerta);
        alertaAiService.invalidarCache(adulto.getIdAdulto());

        Map<String, Object> payload = new HashMap<>();
        payload.put("idAlerta", alerta.getIdAlerta());
        payload.put("idAdulto", adulto.getIdAdulto());
        payload.put("idObservacion", observacion.getIdObservacion());
        payload.put("mensaje", alerta.getMensaje());

        List<RelacionUsuarioAdulto> familiares = relacionRepository
                .findByAdulto_IdAdultoAndTipoRelacion(adulto.getIdAdulto(), "familiar");

        if (familiares.isEmpty()) {
            log.warn("No hay Familiares vinculados al adulto {} para notificar urgencia de signos vitales.",
                    adulto.getIdAdulto());
            return;
        }

        for (RelacionUsuarioAdulto relacion : familiares) {
            Integer idFamiliar = relacion.getUsuario().getIdUsuario();
            notificationService.sendNotification(idFamiliar, TIPO_ALERTA_URGENCIA_SIGNOS_VITALES, payload);
        }
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
