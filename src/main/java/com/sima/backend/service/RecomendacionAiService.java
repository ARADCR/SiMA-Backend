package com.sima.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sima.backend.dto.request.BusquedaCuidadorIARequest;
import com.sima.backend.dto.response.BusquedaCuidadorIAResponse;
import com.sima.backend.dto.response.CuidadorPublicResponse;
import com.sima.backend.dto.response.MatchCuidadorResponse;
import com.sima.backend.entity.AdultoMayor;
import com.sima.backend.exception.ResourceNotFoundException;
import com.sima.backend.exception.UnauthorizedException;
import com.sima.backend.repository.AdultoMayorRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecomendacionAiService {

    private static final Logger log = LoggerFactory.getLogger(RecomendacionAiService.class);
    private static final int MAX_TOKENS_JSON = 1200;

    private static final String SYSTEM_PROMPT_BUSQUEDA = """
            Sos el motor de recomendación de cuidadores de SiMA, una plataforma de monitoreo de adultos mayores.
            Recibís la consulta en lenguaje natural de un Familiar, la lista de cuidadores disponibles y las
            condiciones médicas del adulto mayor. Tu tarea es rankear los cuidadores por relevancia a la consulta
            y justificar brevemente cada recomendación.

            Respondé ÚNICAMENTE con un JSON válido (sin markdown, sin texto adicional) con esta forma exacta:
            {
              "resumenBusqueda": "string con un resumen breve de la interpretación de la búsqueda",
              "cuidadoresRankeados": [
                { "idUsuario": number, "nombre": "string", "scoreRelevancia": number (0-100), "justificacion": "string breve" }
              ]
            }

            Si la consulta es demasiado ambigua para rankear con criterio, devolvé "cuidadoresRankeados": []
            y en "resumenBusqueda" pedí al usuario más detalle sobre lo que necesita (ej. tipo de cuidado, horario, especialidad).
            No inventes cuidadores que no estén en la lista provista.
            """;

    private static final String SYSTEM_PROMPT_MATCH = """
            Sos el motor de compatibilidad cuidador-paciente de SiMA. Recibís el perfil de un cuidador y las
            condiciones médicas de un adulto mayor. Evaluá qué tan compatible es ese cuidador con ese paciente.

            Respondé ÚNICAMENTE con un JSON válido (sin markdown, sin texto adicional) con esta forma exacta:
            {
              "scoreCompatibilidad": number (0-100),
              "justificacion": "string de 2 a 3 oraciones",
              "areasFortaleza": ["string", ...],
              "areasAtencion": ["string", ...]
            }
            """;

    private final RelacionUsuarioAdultoRepository relacionUsuarioAdultoRepository;
    private final AdultoMayorRepository adultoMayorRepository;
    private final VinculacionService vinculacionService;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public RecomendacionAiService(RelacionUsuarioAdultoRepository relacionUsuarioAdultoRepository,
            AdultoMayorRepository adultoMayorRepository,
            VinculacionService vinculacionService,
            AiService aiService,
            ObjectMapper objectMapper) {
        this.relacionUsuarioAdultoRepository = relacionUsuarioAdultoRepository;
        this.adultoMayorRepository = adultoMayorRepository;
        this.vinculacionService = vinculacionService;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    public BusquedaCuidadorIAResponse buscarConIA(Integer idUsuario, BusquedaCuidadorIARequest request) {
        AdultoMayor adulto = validarAccesoYObtenerAdulto(idUsuario, request.getIdAdulto());

        List<CuidadorPublicResponse> cuidadores = vinculacionService.listarCuidadoresDisponibles();

        String userMessage = """
                Consulta del familiar: "%s"

                Condiciones médicas del adulto mayor: %s

                Cuidadores disponibles:
                %s
                """.formatted(
                request.getQuery(),
                condicionesODefault(adulto),
                serializarCuidadores(cuidadores));

        String respuestaLlm = aiService.chat(SYSTEM_PROMPT_BUSQUEDA, userMessage, MAX_TOKENS_JSON);

        BusquedaCuidadorIAResponse response = parsear(respuestaLlm, BusquedaCuidadorIAResponse.class);
        if (response == null) {
            response = new BusquedaCuidadorIAResponse();
            response.setResumenBusqueda("No pudimos procesar la búsqueda en este momento. Intentá reformular tu consulta.");
            response.setCuidadoresRankeados(Collections.emptyList());
        }
        return response;
    }

    public MatchCuidadorResponse calcularMatch(Integer idUsuario, Integer idCuidador, Integer idAdulto) {
        AdultoMayor adulto = validarAccesoYObtenerAdulto(idUsuario, idAdulto);

        CuidadorPublicResponse cuidador = vinculacionService.listarCuidadoresDisponibles().stream()
                .filter(c -> c.getIdUsuario().equals(idCuidador))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cuidador", "id", idCuidador));

        String userMessage = """
                Perfil del cuidador:
                - Nombre: %s %s
                - Especialidad: %s
                - Experiencia: %s
                - Calificación: %s

                Condiciones médicas del adulto mayor: %s
                """.formatted(
                cuidador.getNombre(), cuidador.getApellido(), cuidador.getEspecialidad(),
                cuidador.getExperiencia(), cuidador.getCalificacion(),
                condicionesODefault(adulto));

        String respuestaLlm = aiService.chat(SYSTEM_PROMPT_MATCH, userMessage, MAX_TOKENS_JSON);

        MatchCuidadorResponse response = parsear(respuestaLlm, MatchCuidadorResponse.class);
        if (response == null) {
            response = new MatchCuidadorResponse();
            response.setScoreCompatibilidad(0);
            response.setJustificacion("No se pudo calcular el score de compatibilidad en este momento.");
            response.setAreasFortaleza(Collections.emptyList());
            response.setAreasAtencion(Collections.emptyList());
        }
        return response;
    }

    private AdultoMayor validarAccesoYObtenerAdulto(Integer idUsuario, Integer idAdulto) {
        if (!relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)) {
            throw new UnauthorizedException("No tenés acceso a este adulto mayor");
        }
        return adultoMayorRepository.findByIdAdultoAndActivoTrue(idAdulto)
                .orElseThrow(() -> new ResourceNotFoundException("Adulto Mayor", "id", idAdulto));
    }

    private String condicionesODefault(AdultoMayor adulto) {
        return adulto.getCondicionesMedicas() != null && !adulto.getCondicionesMedicas().isBlank()
                ? adulto.getCondicionesMedicas()
                : "Sin condiciones médicas registradas.";
    }

    private String serializarCuidadores(List<CuidadorPublicResponse> cuidadores) {
        return cuidadores.stream()
                .map(c -> "- idUsuario: %d, nombre: %s %s, especialidad: %s, experiencia: %s, calificación: %s, precio: %s".formatted(
                        c.getIdUsuario(), c.getNombre(), c.getApellido(), c.getEspecialidad(),
                        c.getExperiencia(), c.getCalificacion(), c.getPrecio()))
                .collect(Collectors.joining("\n"));
    }

    private <T> T parsear(String respuestaLlm, Class<T> tipo) {
        try {
            String json = extraerJson(respuestaLlm);
            return objectMapper.readValue(json, tipo);
        } catch (Exception ex) {
            log.error("RecomendacionAiService: no se pudo parsear la respuesta del LLM como JSON", ex);
            return null;
        }
    }

    private String extraerJson(String texto) {
        int inicio = texto.indexOf('{');
        int fin = texto.lastIndexOf('}');
        if (inicio == -1 || fin == -1 || fin < inicio) {
            throw new IllegalArgumentException("La respuesta del LLM no contiene un JSON válido");
        }
        return texto.substring(inicio, fin + 1);
    }
}
