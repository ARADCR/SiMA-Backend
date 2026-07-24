package com.sima.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sima.backend.dto.response.AnalisisIotIAResponse;
import com.sima.backend.entity.AdultoMayor;
import com.sima.backend.entity.Alerta;
import com.sima.backend.entity.AnalisisIotIA;
import com.sima.backend.entity.EventoIot;
import com.sima.backend.entity.Medicamento;
import com.sima.backend.entity.RegistroToma;
import com.sima.backend.exception.UnauthorizedException;
import com.sima.backend.repository.AdultoMayorRepository;
import com.sima.backend.repository.AlertaRepository;
import com.sima.backend.repository.AnalisisIotIARepository;
import com.sima.backend.repository.EventoIotRepository;
import com.sima.backend.repository.RegistroTomaRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Analiza los eventos IoT recientes de un adulto mayor (ritmo cardíaco, pasos,
 * pastillero, caídas) en el contexto de su historial médico y medicamentos,
 * usando el LLM para detectar anomalías que las reglas de umbral simples no
 * capturan (HU-26).
 */
@Service
public class IotAiService {

    private static final Logger log = LoggerFactory.getLogger(IotAiService.class);
    private static final int MAX_TOKENS_JSON = 3000;
    private static final int DIAS_ANALISIS = 7;
    private static final int DIAS_HISTORICO = 30;
    private static final long CACHE_TTL_HORAS = 24L;
    private static final String SIN_DATOS_MENSAJE = "Sin datos IoT suficientes para análisis";

    private static final String SYSTEM_PROMPT = """
            Sos el analista de anomalías IoT de SiMA, una plataforma de monitoreo de adultos
            mayores. Recibís los eventos IoT de los últimos 7 días de un paciente (ritmo cardíaco,
            pasos diarios, aperturas de pastillero, detecciones de caída), sus promedios históricos
            de los últimos 30 días, sus condiciones médicas, sus medicamentos activos (especialmente
            los que afectan signos vitales, como betabloqueantes o antihipertensivos) y sus tomas
            recientes de medicación.

            Tu tarea es detectar anomalías contextuales que las reglas de umbral simples no
            capturan, cruzando los valores recientes contra el historial del paciente y su
            situación médica. Los tipos de anomalía posibles son:
            - "vital": desviaciones de patrones de ritmo cardíaco u otros signos vitales.
            - "actividad": cambios significativos en el nivel de actividad/pasos.
            - "pastillero": patrones anómalos de uso del pastillero (no abierto en mucho tiempo,
              apertura fuera de horario, etc.).
            - "correlacion": combinación de múltiples señales (ej. ritmo cardíaco elevado + caída de
              pasos + omisiones de medicación) que en conjunto indican mayor riesgo.

            Respondé ÚNICAMENTE con un JSON válido (sin markdown, sin texto adicional) con esta
            forma exacta:
            {
              "resumenEstado": "string de máximo 100 palabras con la evaluación general del paciente",
              "anomaliasDetectadas": [
                {
                  "tipo": "vital" | "actividad" | "pastillero" | "correlacion",
                  "descripcion": "string explicando la anomalía detectada",
                  "severidad": "baja" | "media" | "alta" | "critica",
                  "datosRelevantes": { "clave": "valor" },
                  "recomendacion": "string breve y accionable"
                }
              ],
              "tendencias": [
                {
                  "descripcion": "string breve del cambio gradual detectado",
                  "direccion": "subiendo" | "bajando" | "estable",
                  "periodo": "string, ej. 'últimos 7 días'"
                }
              ]
            }

            Usá severidad "critica" solo quando exista riesgo real e inmediato para la salud del
            paciente (ej. signos vitales muy anómalos combinados con omisión de medicación crítica).
            No inventes datos que no estén en el contexto provisto. Si no hay anomalías, devolvé
            "anomaliasDetectadas" como lista vacía.
            """;

    private final EventoIotRepository eventoIotRepository;
    private final AdultoMayorRepository adultoMayorRepository;
    private final RelacionUsuarioAdultoRepository relacionUsuarioAdultoRepository;
    private final RegistroTomaRepository registroTomaRepository;
    private final AlertaRepository alertaRepository;
    private final AnalisisIotIARepository analisisIotIARepository;
    private final AlertaAiService alertaAiService;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public IotAiService(EventoIotRepository eventoIotRepository,
            AdultoMayorRepository adultoMayorRepository,
            RelacionUsuarioAdultoRepository relacionUsuarioAdultoRepository,
            RegistroTomaRepository registroTomaRepository,
            AlertaRepository alertaRepository,
            AnalisisIotIARepository analisisIotIARepository,
            AlertaAiService alertaAiService,
            AiService aiService,
            ObjectMapper objectMapper) {
        this.eventoIotRepository = eventoIotRepository;
        this.adultoMayorRepository = adultoMayorRepository;
        this.relacionUsuarioAdultoRepository = relacionUsuarioAdultoRepository;
        this.registroTomaRepository = registroTomaRepository;
        this.alertaRepository = alertaRepository;
        this.analisisIotIARepository = analisisIotIARepository;
        this.alertaAiService = alertaAiService;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    /**
     * Devuelve el último análisis persistido si tiene menos de 24hs de antigüedad;
     * de lo contrario, ejecuta un nuevo análisis y lo persiste.
     */
    @Transactional
    public AnalisisIotIAResponse obtenerUltimoAnalisis(Integer idUsuario, Integer idAdulto) {
        validarAcceso(idUsuario, idAdulto);

        return analisisIotIARepository.findTopByAdulto_IdAdultoOrderByFechaAnalisisDesc(idAdulto)
                .filter(a -> a.getFechaAnalisis() != null
                        && a.getFechaAnalisis().isAfter(LocalDateTime.now().minusHours(CACHE_TTL_HORAS)))
                .map(this::mapearDesdeCache)
                .orElseGet(() -> analizar(idUsuario, idAdulto));
    }

    /**
     * Ejecuta el análisis IA de anomalías IoT para un adulto, lo persiste y
     * genera una Alerta automática si detecta una anomalía crítica.
     */
    @Transactional
    public AnalisisIotIAResponse analizar(Integer idUsuario, Integer idAdulto) {
        validarAcceso(idUsuario, idAdulto);

        AdultoMayor adulto = adultoMayorRepository.findByIdAdultoAndActivoTrue(idAdulto)
                .orElseThrow(() -> new UnauthorizedException("No tenés acceso al adulto con id: " + idAdulto));

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime desde7Dias = ahora.minusDays(DIAS_ANALISIS);
        LocalDateTime desde30Dias = ahora.minusDays(DIAS_HISTORICO);

        List<EventoIot> eventosRecientes = eventoIotRepository.findByAdultoAndRango(idAdulto, desde7Dias, ahora);

        if (eventosRecientes.isEmpty()) {
            AnalisisIotIAResponse sinDatos = respuestaSinDatos();
            persistir(adulto, sinDatos);
            return sinDatos;
        }

        List<EventoIot> eventosHistoricos = eventoIotRepository.findByAdultoAndRango(idAdulto, desde30Dias, ahora);

        String promedios = calcularPromediosHistoricos(eventosHistoricos);
        String condiciones = condicionesMedicas(adulto);
        String medicamentos = medicamentosActivos(adulto);
        List<RegistroToma> tomasRecientes = registroTomaRepository
                .findHistorialByAdultoAndRango(idAdulto, desde7Dias, ahora);

        String userMessage = """
                Condiciones médicas del adulto mayor: %s

                Medicamentos activos: %s

                Promedios históricos (últimos %d días): %s

                Eventos IoT de los últimos %d días:
                %s

                Registros de toma de medicación de los últimos %d días (para correlación):
                %s
                """.formatted(condiciones, medicamentos, DIAS_HISTORICO, promedios, DIAS_ANALISIS,
                serializarEventos(eventosRecientes), DIAS_ANALISIS, serializarTomas(tomasRecientes));

        String respuestaLlm = aiService.chat(SYSTEM_PROMPT, userMessage, MAX_TOKENS_JSON);

        AnalisisIotIAResponse response = parsear(respuestaLlm);
        if (response == null) {
            response = respuestaPorDefecto();
        }

        persistir(adulto, response);
        crearAlertaSiCritica(adulto, response);

        return response;
    }

    private void validarAcceso(Integer idUsuario, Integer idAdulto) {
        if (!relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)) {
            throw new UnauthorizedException("No tenés acceso a los datos IoT del adulto con id: " + idAdulto);
        }
    }

    private AnalisisIotIAResponse respuestaSinDatos() {
        AnalisisIotIAResponse response = new AnalisisIotIAResponse();
        response.setResumenEstado(SIN_DATOS_MENSAJE);
        response.setAnomaliasDetectadas(Collections.emptyList());
        response.setTendencias(Collections.emptyList());
        response.setFechaAnalisis(LocalDateTime.now());
        return response;
    }

    private AnalisisIotIAResponse respuestaPorDefecto() {
        AnalisisIotIAResponse response = new AnalisisIotIAResponse();
        response.setResumenEstado(
                "No se pudo generar el análisis inteligente en este momento. Intentá nuevamente en unos minutos.");
        response.setAnomaliasDetectadas(Collections.emptyList());
        response.setTendencias(Collections.emptyList());
        response.setFechaAnalisis(LocalDateTime.now());
        return response;
    }

    private void crearAlertaSiCritica(AdultoMayor adulto, AnalisisIotIAResponse response) {
        if (response.getAnomaliasDetectadas() == null) {
            return;
        }
        for (AnalisisIotIAResponse.AnomaliaIotDTO anomalia : response.getAnomaliasDetectadas()) {
            if ("critica".equalsIgnoreCase(anomalia.getSeveridad())) {
                Alerta alerta = new Alerta();
                alerta.setAdulto(adulto);
                alerta.setTipoAlerta("anomalia_iot_ia");
                alerta.setMensaje(anomalia.getDescripcion());
                alertaRepository.save(alerta);
                alertaAiService.invalidarCache(adulto.getIdAdulto());
                log.info("IotAiService: alerta crítica creada automáticamente para adulto {} - {}",
                        adulto.getIdAdulto(), anomalia.getDescripcion());
            }
        }
    }

    private void persistir(AdultoMayor adulto, AnalisisIotIAResponse response) {
        try {
            AnalisisIotIA entidad = new AnalisisIotIA();
            entidad.setAdulto(adulto);
            entidad.setResumenEstado(response.getResumenEstado());
            entidad.setAnomaliasJson(objectMapper.writeValueAsString(
                    response.getAnomaliasDetectadas() == null ? Collections.emptyList() : response.getAnomaliasDetectadas()));
            entidad.setTendenciasJson(objectMapper.writeValueAsString(
                    response.getTendencias() == null ? Collections.emptyList() : response.getTendencias()));
            analisisIotIARepository.save(entidad);
            response.setFechaAnalisis(entidad.getFechaAnalisis());
        } catch (Exception ex) {
            log.error("IotAiService: no se pudo persistir el análisis IoT del adulto {}", adulto.getIdAdulto(), ex);
        }
    }

    private AnalisisIotIAResponse mapearDesdeCache(AnalisisIotIA entidad) {
        AnalisisIotIAResponse response = new AnalisisIotIAResponse();
        response.setResumenEstado(entidad.getResumenEstado());
        response.setFechaAnalisis(entidad.getFechaAnalisis());
        try {
            response.setAnomaliasDetectadas(entidad.getAnomaliasJson() == null
                    ? Collections.emptyList()
                    : objectMapper.readValue(entidad.getAnomaliasJson(),
                            new TypeReference<List<AnalisisIotIAResponse.AnomaliaIotDTO>>() {
                            }));
            response.setTendencias(entidad.getTendenciasJson() == null
                    ? Collections.emptyList()
                    : objectMapper.readValue(entidad.getTendenciasJson(),
                            new TypeReference<List<AnalisisIotIAResponse.TendenciaDTO>>() {
                            }));
        } catch (Exception ex) {
            log.error("IotAiService: no se pudo deserializar el análisis cacheado del adulto {}",
                    entidad.getAdulto().getIdAdulto(), ex);
            response.setAnomaliasDetectadas(Collections.emptyList());
            response.setTendencias(Collections.emptyList());
        }
        return response;
    }

    private String calcularPromediosHistoricos(List<EventoIot> eventos) {
        List<BigDecimal> bpm = valoresPorTipo(eventos, "ritmo_cardiaco");
        List<BigDecimal> pasos = valoresPorTipo(eventos, "pasos_diarios");

        String bpmPromedio = promedio(bpm);
        String pasosPromedio = promedio(pasos);

        return "BPM promedio = " + bpmPromedio + ", Pasos diarios promedio = " + pasosPromedio;
    }

    private List<BigDecimal> valoresPorTipo(List<EventoIot> eventos, String tipo) {
        return eventos.stream()
                .filter(e -> tipo.equals(e.getTipoEvento()) && e.getValor() != null)
                .map(EventoIot::getValor)
                .collect(Collectors.toList());
    }

    private String promedio(List<BigDecimal> valores) {
        if (valores.isEmpty()) {
            return "sin datos suficientes";
        }
        BigDecimal suma = valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(BigDecimal.valueOf(valores.size()), 2, RoundingMode.HALF_UP).toString();
    }

    private String condicionesMedicas(AdultoMayor adulto) {
        return adulto.getCondicionesMedicas() != null && !adulto.getCondicionesMedicas().isBlank()
                ? adulto.getCondicionesMedicas()
                : "Sin condiciones médicas registradas.";
    }

    private String medicamentosActivos(AdultoMayor adulto) {
        List<Medicamento> medicamentos = adulto.getMedicamentos();
        if (medicamentos == null || medicamentos.isEmpty()) {
            return "Sin medicamentos activos registrados.";
        }
        List<String> lista = medicamentos.stream()
                .filter(m -> Boolean.TRUE.equals(m.getActivo()))
                .map(m -> m.getNombre() + " (" + m.getDosis()
                        + (m.getPrincipioActivo() != null ? ", " + m.getPrincipioActivo() : "") + ")")
                .collect(Collectors.toCollection(ArrayList::new));
        if (lista.isEmpty()) {
            return "Sin medicamentos activos registrados.";
        }
        return String.join("; ", lista);
    }

    private String serializarEventos(List<EventoIot> eventos) {
        if (eventos.isEmpty()) {
            return "(sin eventos en el período)";
        }
        return eventos.stream()
                .map(e -> "tipo=" + e.getTipoEvento()
                        + " | valor=" + e.getValor()
                        + " | fecha=" + e.getFechaHora())
                .collect(Collectors.joining("\n"));
    }

    private String serializarTomas(List<RegistroToma> tomas) {
        if (tomas.isEmpty()) {
            return "(sin registros de toma en el período)";
        }
        return tomas.stream()
                .map(t -> "medicamento=" + t.getHorario().getMedicamento().getNombre()
                        + " | estado=" + t.getEstado()
                        + " | fecha=" + t.getFechaHoraProgramada())
                .collect(Collectors.joining("\n"));
    }

    private AnalisisIotIAResponse parsear(String respuestaLlm) {
        try {
            String json = extraerJson(respuestaLlm);
            return objectMapper.readValue(json, AnalisisIotIAResponse.class);
        } catch (Exception ex) {
            log.error("IotAiService: no se pudo parsear la respuesta del LLM como JSON", ex);
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
