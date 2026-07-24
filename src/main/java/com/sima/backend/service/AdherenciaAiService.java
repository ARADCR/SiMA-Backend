package com.sima.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sima.backend.dto.response.PatronesAdherenciaResponse;
import com.sima.backend.dto.response.ReporteMedicionSemanalResponse;
import com.sima.backend.dto.response.ResumenReporteIAResponse;
import com.sima.backend.entity.AdultoMayor;
import com.sima.backend.entity.RegistroToma;
import com.sima.backend.exception.UnauthorizedException;
import com.sima.backend.repository.AdultoMayorRepository;
import com.sima.backend.repository.RegistroTomaRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Potencia los reportes de adherencia existentes ({@link ReporteService}) con dos capacidades de IA:
 * resumen narrativo del reporte semanal y detección de patrones de omisión sobre las últimas 4 semanas.
 * No duplica la lógica de cálculo de adherencia — siempre delega en ReporteService/RegistroTomaRepository.
 */
@Service
public class AdherenciaAiService {

    private static final Logger log = LoggerFactory.getLogger(AdherenciaAiService.class);
    private static final int MAX_TOKENS_JSON = 3000;
    private static final int DIAS_ANALISIS_PATRONES = 28;
    private static final int DIAS_MINIMOS_REQUERIDOS = 7;

    private static final String SYSTEM_PROMPT_RESUMEN = """
            Sos el analizador de reportes de adherencia a medicación de SiMA, una plataforma de monitoreo
            de adultos mayores. Recibís los datos numéricos de un reporte semanal de adherencia y las
            condiciones médicas del adulto mayor. Tu tarea es generar un resumen narrativo comprensible
            para un Familiar o Cuidador que no tiene formación médica.

            Respondé ÚNICAMENTE con un JSON válido (sin markdown, sin texto adicional) con esta forma exacta:
            {
              "resumenNarrativo": "string de máximo 200 palabras, en tono claro y empático",
              "puntosClave": ["string", ...] (máximo 5 items),
              "recomendaciones": ["string", ...] (máximo 3 items)
            }

            Mencioná medicamentos y horarios concretos cuando el dato lo permita. No inventes información
            que no esté en los datos provistos.
            """;

    private static final String SYSTEM_PROMPT_PATRONES = """
            Sos el detector de patrones de adherencia a medicación de SiMA, una plataforma de monitoreo
            de adultos mayores. Recibís una tabla resumida con el historial de tomas de las últimas 4
            semanas (fecha, hora, medicamento, estado, método de confirmación) de un adulto mayor.
            Tu tarea es identificar patrones recurrentes que los datos numéricos simples no revelan:
            patrones temporales, patrones por medicamento, patrones por método de confirmación y
            tendencias a lo largo del período.

            Respondé en formato JSON puro. Tu respuesta debe empezar con '{' y terminar con '}'.
            No incluyas explicaciones, ni bloques de código markdown, ni texto adicional.
            
            Estructura esperada:
            {
              "patronesDetectados": [
                {
                  "tipo": "temporal",
                  "descripcion": "string breve y concreto",
                  "severidad": "baja",
                  "recomendacion": "string breve y accionable"
                }
              ]
            }

            Si los datos indican que el paciente toma toda su medicación correctamente o no hay patrones claros de omisión, devolvé exactamente:
            {
              "patronesDetectados": []
            }
            """;

    private final ReporteService reporteService;
    private final RegistroTomaRepository registroTomaRepository;
    private final RelacionUsuarioAdultoRepository relacionUsuarioAdultoRepository;
    private final AdultoMayorRepository adultoMayorRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public AdherenciaAiService(ReporteService reporteService,
            RegistroTomaRepository registroTomaRepository,
            RelacionUsuarioAdultoRepository relacionUsuarioAdultoRepository,
            AdultoMayorRepository adultoMayorRepository,
            AiService aiService,
            ObjectMapper objectMapper) {
        this.reporteService = reporteService;
        this.registroTomaRepository = registroTomaRepository;
        this.relacionUsuarioAdultoRepository = relacionUsuarioAdultoRepository;
        this.adultoMayorRepository = adultoMayorRepository;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ResumenReporteIAResponse generarResumen(Integer idUsuario, Integer idAdulto) {
        // RBAC + cálculo de adherencia reutilizados de ReporteService — no se duplica lógica.
        ReporteMedicionSemanalResponse reporte = reporteService.generarReporteSemanal(idUsuario, idAdulto);

        String condiciones = adultoMayorRepository.findByIdAdultoAndActivoTrue(idAdulto)
                .map(AdultoMayor::getCondicionesMedicas)
                .filter(c -> c != null && !c.isBlank())
                .orElse("Sin condiciones médicas registradas.");

        String userMessage = """
                Condiciones médicas del adulto mayor: %s

                Reporte semanal de adherencia:
                %s
                """.formatted(condiciones, serializarReporte(reporte));

        String respuestaLlm = aiService.chat(SYSTEM_PROMPT_RESUMEN, userMessage, MAX_TOKENS_JSON);

        ResumenReporteIAResponse response = parsear(respuestaLlm, ResumenReporteIAResponse.class);
        if (response == null) {
            response = new ResumenReporteIAResponse();
            response.setResumenNarrativo(
                    "No se pudo generar el resumen narrativo en este momento. Intentá nuevamente en unos minutos.");
            response.setPuntosClave(Collections.emptyList());
            response.setRecomendaciones(Collections.emptyList());
        }
        return response;
    }

    @Transactional(readOnly = true)
    public PatronesAdherenciaResponse detectarPatrones(Integer idUsuario, Integer idAdulto) {
        if (!relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)) {
            throw new UnauthorizedException("No tenés acceso al reporte del adulto con id: " + idAdulto);
        }

        LocalDateTime hasta = LocalDateTime.now();
        LocalDateTime desde = hasta.minusDays(DIAS_ANALISIS_PATRONES).toLocalDate().atStartOfDay();

        List<RegistroToma> registros = registroTomaRepository.findHistorialByAdultoAndRango(idAdulto, desde, hasta);

        if (!hayDatosSuficientes(registros)) {
            PatronesAdherenciaResponse response = new PatronesAdherenciaResponse();
            response.setPatronesDetectados(Collections.emptyList());
            response.setMensajeInformativo(
                    "Se necesitan al menos " + DIAS_MINIMOS_REQUERIDOS + " días de historial para detectar patrones confiables.");
            return response;
        }

        String userMessage = """
                Historial de tomas del adulto mayor (últimas %d días):
                %s
                
                Analizá estos datos y respondé ÚNICAMENTE con el objeto JSON solicitado.
                """.formatted(DIAS_ANALISIS_PATRONES, serializarRegistros(registros));

        String respuestaLlm = aiService.chat(SYSTEM_PROMPT_PATRONES, userMessage, MAX_TOKENS_JSON);

        if (respuestaLlm == null || respuestaLlm.trim().isEmpty()) {
            log.warn("AdherenciaAiService: El LLM devolvió una respuesta vacía o solo espacios en blanco. Asumiendo sin patrones.");
            PatronesAdherenciaResponse emptyResponse = new PatronesAdherenciaResponse();
            emptyResponse.setPatronesDetectados(Collections.emptyList());
            return emptyResponse;
        }

        PatronesAdherenciaResponse response = parsear(respuestaLlm, PatronesAdherenciaResponse.class);
        if (response == null) {
            response = new PatronesAdherenciaResponse();
            response.setPatronesDetectados(Collections.emptyList());
            response.setMensajeInformativo(
                    "No se pudieron analizar los patrones de adherencia en este momento. Intentá nuevamente en unos minutos.");
        }
        return response;
    }

    private boolean hayDatosSuficientes(List<RegistroToma> registros) {
        Set<LocalDate> diasConDatos = new TreeSet<>();
        for (RegistroToma rt : registros) {
            diasConDatos.add(rt.getFechaHoraProgramada().toLocalDate());
        }
        return diasConDatos.size() >= DIAS_MINIMOS_REQUERIDOS;
    }

    private String serializarReporte(ReporteMedicionSemanalResponse reporte) {
        StringBuilder sb = new StringBuilder();
        sb.append("- Porcentaje de adherencia: ").append(reporte.getPorcentajeAdherencia()).append("%\n");
        sb.append("- Total programadas: ").append(reporte.getTotalProgramadas()).append("\n");
        sb.append("- Total tomadas: ").append(reporte.getTotalTomadas()).append("\n");
        sb.append("- Total omitidas: ").append(reporte.getTotalOmitidas()).append("\n");
        sb.append("- Medicamentos más omitidos:\n");
        if (reporte.getMedicamentosMasOmitidos() == null || reporte.getMedicamentosMasOmitidos().isEmpty()) {
            sb.append("  (ninguno)\n");
        } else {
            reporte.getMedicamentosMasOmitidos().forEach(m -> sb.append("  - ")
                    .append(m.getNombre()).append(": ").append(m.getCantidadOmisiones()).append(" omisiones\n"));
        }
        sb.append("- Desglose diario:\n");
        if (reporte.getDesgloseDiario() != null) {
            reporte.getDesgloseDiario().forEach(d -> sb.append("  - ")
                    .append(d.getFecha()).append(": ").append(d.getTotalTomadas())
                    .append("/").append(d.getTotalProgramadas()).append(" tomadas\n"));
        }
        return sb.toString();
    }

    private String serializarRegistros(List<RegistroToma> registros) {
        StringBuilder sb = new StringBuilder();
        sb.append("fecha | hora | medicamento | estado | método\n");
        for (RegistroToma rt : registros) {
            String nombreMedicamento = rt.getHorario() != null && rt.getHorario().getMedicamento() != null
                    ? rt.getHorario().getMedicamento().getNombre()
                    : "desconocido";
            sb.append(rt.getFechaHoraProgramada().toLocalDate()).append(" | ")
                    .append(rt.getFechaHoraProgramada().toLocalTime()).append(" | ")
                    .append(nombreMedicamento).append(" | ")
                    .append(rt.getEstado()).append(" | ")
                    .append(rt.getMetodoConfirmacion() == null ? "n/a" : rt.getMetodoConfirmacion())
                    .append("\n");
        }
        return sb.toString();
    }

    private <T> T parsear(String respuestaLlm, Class<T> tipo) {
        try {
            String json = extraerJson(respuestaLlm);
            return objectMapper.readValue(json, tipo);
        } catch (Exception ex) {
            log.error("AdherenciaAiService: no se pudo parsear la respuesta del LLM como JSON", ex);
            return null;
        }
    }

    private String extraerJson(String texto) {
        int inicio = texto.indexOf('{');
        int fin = texto.lastIndexOf('}');
        if (inicio == -1 || fin == -1 || fin < inicio) {
            throw new IllegalArgumentException("La respuesta del LLM no contiene un JSON válido. Respuesta recibida: " + texto);
        }
        return texto.substring(inicio, fin + 1);
    }
}
