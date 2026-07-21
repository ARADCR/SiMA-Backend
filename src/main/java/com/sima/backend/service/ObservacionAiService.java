package com.sima.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sima.backend.dto.request.EvaluarUrgenciaRequest;
import com.sima.backend.dto.response.EvaluacionUrgenciaResponse;
import com.sima.backend.dto.response.ResumenObservacionesResponse;
import com.sima.backend.entity.AdultoMayor;
import com.sima.backend.entity.Medicamento;
import com.sima.backend.entity.ObservacionCuidador;
import com.sima.backend.exception.UnauthorizedException;
import com.sima.backend.repository.AdultoMayorRepository;
import com.sima.backend.repository.MedicamentoRepository;
import com.sima.backend.repository.ObservacionCuidadorRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Potencia el flujo de observaciones del cuidador ({@link ObservacionService}) con dos capacidades de IA:
 * resumen consolidado de observaciones recientes (para el Familiar) y evaluación de urgencia de signos
 * vitales en contexto médico (para el Cuidador). No duplica la lógica de acceso a datos — siempre delega
 * en {@link ObservacionCuidadorRepository} / {@link RelacionUsuarioAdultoRepository}.
 */
@Service
public class ObservacionAiService {

    private static final Logger log = LoggerFactory.getLogger(ObservacionAiService.class);
    private static final int MAX_TOKENS_JSON = 3000;
    private static final int MAX_OBSERVACIONES = 10;
    private static final int HORAS_ANALISIS = 72;
    private static final String PERIODO_ANALIZADO = "Últimas 72 horas";

    private static final String SYSTEM_PROMPT_RESUMEN = """
            Sos el analizador de observaciones de cuidadores de SiMA, una plataforma de monitoreo de
            adultos mayores. Recibís las observaciones más recientes registradas por Cuidadores sobre un
            adulto mayor (texto, nivel de urgencia, signos vitales, fecha y cuidador que la registró) junto
            con las condiciones médicas del adulto. Tu tarea es generar un resumen consolidado para que un
            Familiar entienda el panorama sin leer cada nota individualmente.

            Respondé ÚNICAMENTE con un JSON válido (sin markdown, sin texto adicional) con esta forma exacta:
            {
              "resumen": "string de máximo 150 palabras, priorizando primero la observación más urgente si existe",
              "observacionesAnalizadas": number (cantidad de observaciones incluidas en el análisis),
              "periodoAnalizado": "string, ej. 'Últimas 72 horas'",
              "alertasIdentificadas": ["string", ...] (valores anormales o hallazgos preocupantes detectados)
            }

            No inventes información que no esté en los datos provistos.
            """;

    private static final String SYSTEM_PROMPT_URGENCIA = """
            Sos el evaluador de urgencia de signos vitales de SiMA, una plataforma de monitoreo de adultos
            mayores. Recibís signos vitales recién registrados por un Cuidador, las condiciones médicas del
            adulto mayor, sus medicamentos activos (especialmente antihipertensivos y antidiabéticos), y los
            últimos signos vitales registrados para comparación. Tu tarea es sugerir un nivel de urgencia
            clínico, actuando como un segundo par de ojos para un Cuidador que no siempre tiene formación
            médica para evaluar estos valores en contexto.

            Respondé ÚNICAMENTE con un JSON válido (sin markdown, sin texto adicional) con esta forma exacta:
            {
              "urgenciaSugerida": "normal" | "importante" | "urgente",
              "justificacion": "string de 1 a 2 oraciones con la justificación clínica breve",
              "valoresAnormales": ["string", ...] (valores fuera de rango esperado, ej. "Tensión arterial elevada: 180/110 mmHg"),
              "recomendaciones": ["string", ...] (acciones concretas y breves)
            }

            No emitas diagnósticos ni prescripciones de tratamiento — solo sugerí el nivel de urgencia y
            recomendaciones operativas (ej. verificar medicación, contactar al médico tratante). No inventes
            información que no esté en los datos provistos.
            """;

    private final ObservacionCuidadorRepository observacionRepository;
    private final RelacionUsuarioAdultoRepository relacionUsuarioAdultoRepository;
    private final AdultoMayorRepository adultoMayorRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public ObservacionAiService(ObservacionCuidadorRepository observacionRepository,
            RelacionUsuarioAdultoRepository relacionUsuarioAdultoRepository,
            AdultoMayorRepository adultoMayorRepository,
            MedicamentoRepository medicamentoRepository,
            AiService aiService,
            ObjectMapper objectMapper) {
        this.observacionRepository = observacionRepository;
        this.relacionUsuarioAdultoRepository = relacionUsuarioAdultoRepository;
        this.adultoMayorRepository = adultoMayorRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ResumenObservacionesResponse resumir(Integer idUsuario, Integer idAdulto) {
        validarAcceso(idUsuario, idAdulto);

        LocalDateTime hasta = LocalDateTime.now();
        LocalDateTime desde = hasta.minusHours(HORAS_ANALISIS);

        List<ObservacionCuidador> observaciones = observacionRepository.findByAdultoAndRango(idAdulto, desde, hasta);
        if (observaciones.size() > MAX_OBSERVACIONES) {
            observaciones = observaciones.subList(0, MAX_OBSERVACIONES);
        }

        String condiciones = adultoMayorRepository.findByIdAdultoAndActivoTrue(idAdulto)
                .map(AdultoMayor::getCondicionesMedicas)
                .filter(c -> c != null && !c.isBlank())
                .orElse("Sin condiciones médicas registradas.");

        String userMessage = """
                Condiciones médicas del adulto mayor: %s

                Observaciones registradas (%d):
                %s
                """.formatted(condiciones, observaciones.size(), serializarObservaciones(observaciones));

        String respuestaLlm = aiService.chat(SYSTEM_PROMPT_RESUMEN, userMessage, MAX_TOKENS_JSON);

        ResumenObservacionesResponse response = parsear(respuestaLlm, ResumenObservacionesResponse.class);
        if (response == null) {
            response = new ResumenObservacionesResponse();
            response.setResumen(
                    "No se pudo generar el resumen de observaciones en este momento. Intentá nuevamente en unos minutos.");
            response.setObservacionesAnalizadas(observaciones.size());
            response.setPeriodoAnalizado(PERIODO_ANALIZADO);
            response.setAlertasIdentificadas(Collections.emptyList());
        }
        return response;
    }

    @Transactional(readOnly = true)
    public EvaluacionUrgenciaResponse evaluarUrgencia(Integer idUsuario, EvaluarUrgenciaRequest request) {
        validarAcceso(idUsuario, request.getIdAdulto());

        AdultoMayor adulto = adultoMayorRepository.findByIdAdultoAndActivoTrue(request.getIdAdulto()).orElse(null);
        String condiciones = adulto != null && adulto.getCondicionesMedicas() != null
                && !adulto.getCondicionesMedicas().isBlank()
                        ? adulto.getCondicionesMedicas()
                        : "Sin condiciones médicas registradas.";

        List<Medicamento> medicamentos = medicamentoRepository.findByAdulto_IdAdultoAndActivoTrue(request.getIdAdulto());
        String medicamentosTexto = medicamentos.isEmpty()
                ? "Sin medicamentos activos registrados."
                : medicamentos.stream().map(Medicamento::getNombre).reduce((a, b) -> a + ", " + b).orElse("");

        List<ObservacionCuidador> ultimasObservaciones = observacionRepository
                .findTop5ByAdulto_IdAdultoOrderByFechaHoraDesc(request.getIdAdulto());
        String ultimosVitales = serializarUltimosVitales(ultimasObservaciones);

        String userMessage = """
                Condiciones médicas del adulto mayor: %s
                Medicamentos activos: %s
                Últimos signos vitales registrados (para comparación): %s

                Signos vitales recién registrados:
                - Tensión arterial: %s
                - Frecuencia cardíaca: %s
                - Temperatura: %s
                Texto de la observación: %s
                """.formatted(condiciones, medicamentosTexto, ultimosVitales,
                        valorOrNA(request.getTensionArterial()),
                        valorOrNA(request.getFrecuenciaCardiaca()),
                        valorOrNA(request.getTemperatura()),
                        valorOrNA(request.getTextoObservacion()));

        String respuestaLlm = aiService.chat(SYSTEM_PROMPT_URGENCIA, userMessage, MAX_TOKENS_JSON);

        EvaluacionUrgenciaResponse response = parsear(respuestaLlm, EvaluacionUrgenciaResponse.class);
        if (response == null) {
            response = new EvaluacionUrgenciaResponse();
            response.setUrgenciaSugerida("normal");
            response.setJustificacion(
                    "No se pudo evaluar la urgencia en este momento. Revisá los valores manualmente e intentá nuevamente.");
            response.setValoresAnormales(Collections.emptyList());
            response.setRecomendaciones(Collections.emptyList());
        }
        return response;
    }

    private void validarAcceso(Integer idUsuario, Integer idAdulto) {
        if (!relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)) {
            throw new UnauthorizedException("No tenés acceso al adulto mayor con id: " + idAdulto);
        }
    }

    private String valorOrNA(String valor) {
        return valor == null || valor.isBlank() ? "no registrado" : valor;
    }

    private String serializarObservaciones(List<ObservacionCuidador> observaciones) {
        if (observaciones.isEmpty()) {
            return "(sin observaciones en el periodo analizado)";
        }
        StringBuilder sb = new StringBuilder();
        for (ObservacionCuidador obs : observaciones) {
            sb.append("- [").append(obs.getFechaHora()).append("] ")
                    .append("Cuidador: ").append(obs.getCuidador() != null ? obs.getCuidador().getNombre() : "desconocido")
                    .append(" | Urgencia: ").append(obs.getUrgencia())
                    .append(" | Texto: ").append(obs.getTexto());
            if (obs.getTensionArterial() != null) sb.append(" | TA: ").append(obs.getTensionArterial());
            if (obs.getFrecuenciaCardiaca() != null) sb.append(" | FC: ").append(obs.getFrecuenciaCardiaca());
            if (obs.getTemperatura() != null) sb.append(" | Temp: ").append(obs.getTemperatura());
            sb.append("\n");
        }
        return sb.toString();
    }

    private String serializarUltimosVitales(List<ObservacionCuidador> observaciones) {
        return observaciones.stream()
                .filter(o -> o.getTensionArterial() != null || o.getFrecuenciaCardiaca() != null || o.getTemperatura() != null)
                .findFirst()
                .map(o -> "TA: " + valorOrNA(o.getTensionArterial())
                        + ", FC: " + valorOrNA(o.getFrecuenciaCardiaca())
                        + ", Temp: " + valorOrNA(o.getTemperatura())
                        + " (" + o.getFechaHora() + ")")
                .orElse("Sin registros previos de signos vitales.");
    }

    private <T> T parsear(String respuestaLlm, Class<T> tipo) {
        try {
            String json = extraerJson(respuestaLlm);
            return objectMapper.readValue(json, tipo);
        } catch (Exception ex) {
            log.error("ObservacionAiService: no se pudo parsear la respuesta del LLM como JSON", ex);
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
