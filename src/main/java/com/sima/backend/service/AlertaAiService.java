package com.sima.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sima.backend.dto.response.ResumenAlertasIAResponse;
import com.sima.backend.entity.Alerta;
import com.sima.backend.entity.AdultoMayor;
import com.sima.backend.exception.UnauthorizedException;
import com.sima.backend.repository.AdultoMayorRepository;
import com.sima.backend.repository.AlertaRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Agrega inteligencia de lectura sobre las alertas ya generadas por el RecordatorioScheduler:
 * prioriza, agrupa y detecta escaladas cruzando las alertas del día con los últimos 7 días de
 * historial. No modifica la forma en que las alertas se crean.
 */
@Service
public class AlertaAiService {

    private static final Logger log = LoggerFactory.getLogger(AlertaAiService.class);
    private static final int MAX_TOKENS_JSON = 3000;
    private static final int DIAS_HISTORIAL_ESCALADAS = 7;
    private static final long CACHE_TTL_MILLIS = 15 * 60 * 1000L;

    private static final String SYSTEM_PROMPT = """
            Sos el priorizador de alertas de SiMA, una plataforma de monitoreo de adultos mayores.
            Recibís todas las alertas de hoy para un adulto mayor, su historial de alertas de los
            últimos 7 días (para detectar patrones de repetición) y sus condiciones médicas.

            Tu tarea es clasificar las alertas de hoy en críticas, informativas y resueltas, y
            detectar escaladas: alertas individuales que no parecen graves aisladas pero que en el
            contexto del historial reciente sí lo son (ej. la 3ra omisión del mismo medicamento en
            5 días).

            Respondé ÚNICAMENTE con un JSON válido (sin markdown, sin texto adicional) con esta
            forma exacta:
            {
              "resumenEjecutivo": "string de máximo 100 palabras resumiendo lo más importante del día",
              "alertasCriticas": [
                {
                  "idAlerta": number,
                  "tipo": "string (tipo_alerta original)",
                  "mensaje": "string (mensaje original)",
                  "justificacion": "string explicando por qué requiere atención inmediata"
                }
              ],
              "alertasInformativas": number (conteo de alertas de rutina, no críticas),
              "alertasResueltas": number (conteo de alertas ya resueltas),
              "escaladas": [
                {
                  "descripcion": "string breve del patrón detectado",
                  "alertasRelacionadas": [idAlerta, ...],
                  "recomendacion": "string breve y accionable"
                }
              ]
            }

            No inventes alertas ni ids que no estén en los datos provistos. Si no hay alertas
            críticas ni escaladas, devolvé listas vacías.
            """;

    private final AlertaRepository alertaRepository;
    private final RelacionUsuarioAdultoRepository relacionUsuarioAdultoRepository;
    private final AdultoMayorRepository adultoMayorRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

    public AlertaAiService(AlertaRepository alertaRepository,
            RelacionUsuarioAdultoRepository relacionUsuarioAdultoRepository,
            AdultoMayorRepository adultoMayorRepository,
            AiService aiService,
            ObjectMapper objectMapper) {
        this.alertaRepository = alertaRepository;
        this.relacionUsuarioAdultoRepository = relacionUsuarioAdultoRepository;
        this.adultoMayorRepository = adultoMayorRepository;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ResumenAlertasIAResponse resumirAlertas(Integer idUsuario, Integer idAdulto) {
        if (!relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)) {
            throw new UnauthorizedException("No tenés acceso a las alertas del adulto con id: " + idAdulto);
        }

        LocalDate hoy = LocalDate.now();
        String cacheKey = "alertas_" + idAdulto + "_" + hoy;

        CachedResult cacheado = cache.get(cacheKey);
        if (cacheado != null && !cacheado.estaExpirado()) {
            return cacheado.respuesta;
        }

        LocalDateTime desde = hoy.minusDays(DIAS_HISTORIAL_ESCALADAS).atStartOfDay();
        LocalDateTime hasta = LocalDateTime.now();

        List<Alerta> alertasHoy = alertaRepository.findByAdultoAndRango(idAdulto, hoy.atStartOfDay(), hasta);
        List<Alerta> historial7Dias = alertaRepository.findByAdultoAndRango(idAdulto, desde, hasta);

        String condiciones = adultoMayorRepository.findByIdAdultoAndActivoTrue(idAdulto)
                .map(AdultoMayor::getCondicionesMedicas)
                .filter(c -> c != null && !c.isBlank())
                .orElse("Sin condiciones médicas registradas.");

        String userMessage = """
                Condiciones médicas del adulto mayor: %s

                Alertas de hoy:
                %s

                Historial de alertas de los últimos %d días (para detección de escaladas):
                %s
                """.formatted(condiciones, serializarAlertas(alertasHoy), DIAS_HISTORIAL_ESCALADAS,
                serializarAlertas(historial7Dias));

        String respuestaLlm = aiService.chat(SYSTEM_PROMPT, userMessage, MAX_TOKENS_JSON);

        ResumenAlertasIAResponse response = parsear(respuestaLlm);
        if (response == null) {
            response = respuestaPorDefecto(alertasHoy);
        }

        cache.put(cacheKey, new CachedResult(response, System.currentTimeMillis() + CACHE_TTL_MILLIS));
        return response;
    }

    /**
     * Invalida todas las entradas de caché correspondientes a un adulto mayor, sin importar la
     * fecha. Debe llamarse inmediatamente después de crear una nueva alerta para ese adulto, para
     * evitar que el resumen quede desactualizado hasta que expire el TTL.
     */
    public void invalidarCache(Integer idAdulto) {
        String prefijo = "alertas_" + idAdulto + "_";
        cache.keySet().removeIf(key -> key.startsWith(prefijo));
    }

    private ResumenAlertasIAResponse respuestaPorDefecto(List<Alerta> alertasHoy) {
        ResumenAlertasIAResponse response = new ResumenAlertasIAResponse();
        response.setResumenEjecutivo(
                "No se pudo generar el resumen inteligente en este momento. Intentá nuevamente en unos minutos.");
        response.setAlertasCriticas(Collections.emptyList());
        response.setAlertasInformativas(0);
        response.setAlertasResueltas((int) alertasHoy.stream().filter(a -> Boolean.TRUE.equals(a.getResuelta())).count());
        response.setEscaladas(Collections.emptyList());
        return response;
    }

    private String serializarAlertas(List<Alerta> alertas) {
        if (alertas.isEmpty()) {
            return "(sin alertas en el período)";
        }
        return alertas.stream()
                .map(a -> "id=" + a.getIdAlerta()
                        + " | fecha=" + a.getCreadoEn()
                        + " | tipo=" + a.getTipoAlerta()
                        + " | mensaje=" + a.getMensaje()
                        + " | resuelta=" + a.getResuelta())
                .collect(Collectors.joining("\n"));
    }

    private ResumenAlertasIAResponse parsear(String respuestaLlm) {
        try {
            String json = extraerJson(respuestaLlm);
            return objectMapper.readValue(json, ResumenAlertasIAResponse.class);
        } catch (Exception ex) {
            log.error("AlertaAiService: no se pudo parsear la respuesta del LLM como JSON", ex);
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

    private static class CachedResult {
        private final ResumenAlertasIAResponse respuesta;
        private final long expiraEn;

        CachedResult(ResumenAlertasIAResponse respuesta, long expiraEn) {
            this.respuesta = respuesta;
            this.expiraEn = expiraEn;
        }

        boolean estaExpirado() {
            return System.currentTimeMillis() > expiraEn;
        }
    }
}
