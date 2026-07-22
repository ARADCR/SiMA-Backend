package com.sima.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sima.backend.dto.response.BriefingIAResponse;
import com.sima.backend.dto.response.ReporteMedicionSemanalResponse;
import com.sima.backend.entity.AdultoMayor;
import com.sima.backend.entity.Alerta;
import com.sima.backend.entity.Medicamento;
import com.sima.backend.entity.ObservacionCuidador;
import com.sima.backend.entity.RegistroToma;
import com.sima.backend.entity.Usuario;
import com.sima.backend.exception.ResourceNotFoundException;
import com.sima.backend.repository.AdultoMayorRepository;
import com.sima.backend.repository.AlertaRepository;
import com.sima.backend.repository.MedicamentoRepository;
import com.sima.backend.repository.ObservacionCuidadorRepository;
import com.sima.backend.repository.RegistroTomaRepository;
import com.sima.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * HU-25: Genera el briefing diario inteligente del dashboard, consolidando en un
 * solo resumen priorizado toda la información relevante de los adultos vinculados
 * al usuario autenticado (Cuidador o Familiar). Reutiliza servicios/repositorios ya
 * existentes (MedicamentoService, ReporteService, RegistroTomaRepository, etc.) en
 * lugar de reimplementar sus consultas.
 */
@Service
public class DashboardAiService {

    private static final Logger log = LoggerFactory.getLogger(DashboardAiService.class);
    private static final int MAX_TOKENS_JSON = 4000;
    private static final int MAX_ADULTOS = 10;
    private static final long CACHE_TTL_MILLIS = 30 * 60 * 1000L;
    private static final DateTimeFormatter HORA_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String SYSTEM_PROMPT_CUIDADOR = """
            Sos el asistente de briefing diario de SiMA para Cuidadores, una plataforma de
            monitoreo de adultos mayores. Recibís el contexto consolidado de todos los pacientes
            asignados al cuidador (medicamentos, tomas del día, alertas activas, última
            observación y adherencia semanal) y debés generar un briefing accionable, con tono
            profesional y directo, priorizando por urgencia.

            Respondé ÚNICAMENTE con un JSON válido (sin markdown, sin texto adicional) con esta
            forma exacta:
            {
              "saludo": "string personalizado con el nombre del cuidador, ej: 'Buenos días, Carlos. Hoy tenés N pacientes asignados.'",
              "resumenGeneral": "string de máximo 100 palabras resumiendo lo más importante del día",
              "pacientes": [
                {
                  "idAdulto": number,
                  "nombre": "string (nombre completo del adulto)",
                  "prioridad": "alta" | "media" | "baja",
                  "resumen": "string de 2-3 oraciones con contexto accionable",
                  "proximaToma": "string describiendo la próxima toma pendiente o 'Sin tomas pendientes'",
                  "adherenciaSemana": number (porcentaje 0-100)
                }
              ]
            }

            Prioridad alta: alertas activas sin resolver, omisiones repetidas o signos vitales
            preocupantes. Prioridad media: situaciones a seguir de cerca sin urgencia inmediata.
            Prioridad baja: paciente estable, sin pendientes. No inventes datos que no estén en el
            contexto provisto.
            """;

    private static final String SYSTEM_PROMPT_FAMILIAR = """
            Sos el asistente de briefing diario de SiMA para Familiares, una plataforma de
            monitoreo de adultos mayores. Recibís el contexto consolidado de todos los adultos
            mayores vinculados al familiar (medicamentos, tomas del día, alertas activas, última
            observación del cuidador y adherencia semanal) y debés generar un resumen cálido y
            tranquilizador, fácil de leer.

            Respondé ÚNICAMENTE con un JSON válido (sin markdown, sin texto adicional) con esta
            forma exacta:
            {
              "saludo": "string cálido y personalizado con el nombre del familiar, ej: 'Buenos días, María.'",
              "resumenGeneral": "string de máximo 100 palabras con un panorama general y cálido del día",
              "pacientes": [
                {
                  "idAdulto": number,
                  "nombre": "string (nombre completo del adulto)",
                  "prioridad": "alta" | "media" | "baja",
                  "resumen": "string de 2-3 oraciones, tono cálido, mencionando la última observación del cuidador si existe",
                  "proximaToma": "string describiendo la próxima toma pendiente o 'Sin tomas pendientes'",
                  "adherenciaSemana": number (porcentaje 0-100)
                }
              ]
            }

            Prioridad alta: alertas activas sin resolver o situaciones que requieren atención.
            Prioridad media: algo para seguir de cerca. Prioridad baja: todo tranquilo. No
            inventes datos que no estén en el contexto provisto.
            """;

    private final UsuarioRepository usuarioRepository;
    private final AdultoMayorRepository adultoMayorRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final RegistroTomaRepository registroTomaRepository;
    private final AlertaRepository alertaRepository;
    private final ObservacionCuidadorRepository observacionCuidadorRepository;
    private final ReporteService reporteService;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

    public DashboardAiService(UsuarioRepository usuarioRepository,
            AdultoMayorRepository adultoMayorRepository,
            MedicamentoRepository medicamentoRepository,
            RegistroTomaRepository registroTomaRepository,
            AlertaRepository alertaRepository,
            ObservacionCuidadorRepository observacionCuidadorRepository,
            ReporteService reporteService,
            AiService aiService,
            ObjectMapper objectMapper) {
        this.usuarioRepository = usuarioRepository;
        this.adultoMayorRepository = adultoMayorRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.registroTomaRepository = registroTomaRepository;
        this.alertaRepository = alertaRepository;
        this.observacionCuidadorRepository = observacionCuidadorRepository;
        this.reporteService = reporteService;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public BriefingIAResponse generarBriefing(Integer idUsuario) {
        LocalDate hoy = LocalDate.now();
        String cacheKey = "briefing_" + idUsuario + "_" + hoy;

        CachedResult cacheado = cache.get(cacheKey);
        if (cacheado != null && !cacheado.estaExpirado()) {
            return cacheado.respuesta;
        }

        BriefingIAResponse response = construirBriefing(idUsuario, hoy);
        cache.put(cacheKey, new CachedResult(response, System.currentTimeMillis() + CACHE_TTL_MILLIS));
        return response;
    }

    /**
     * Fuerza la regeneración del briefing invalidando el cache del usuario para el día actual.
     * Usado por el botón de refresco del dashboard.
     */
    @Transactional(readOnly = true)
    public BriefingIAResponse refrescarBriefing(Integer idUsuario) {
        invalidarCache(idUsuario);
        return generarBriefing(idUsuario);
    }

    public void invalidarCache(Integer idUsuario) {
        String prefijo = "briefing_" + idUsuario + "_";
        cache.keySet().removeIf(key -> key.startsWith(prefijo));
    }

    private BriefingIAResponse construirBriefing(Integer idUsuario, LocalDate hoy) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", idUsuario));

        String rol = usuario.getRol().getNombreRol();
        boolean esCuidador = "Cuidador".equalsIgnoreCase(rol);

        List<AdultoMayor> adultos = adultoMayorRepository.findByUsuarioId(idUsuario);
        if (adultos.size() > MAX_ADULTOS) {
            adultos = adultos.subList(0, MAX_ADULTOS);
        }

        if (adultos.isEmpty()) {
            return respuestaSinAdultos(usuario, esCuidador);
        }

        LocalDateTime inicioDia = hoy.atStartOfDay();
        LocalDateTime finDia = inicioDia.plusDays(1);
        LocalDateTime ahora = LocalDateTime.now();

        String contexto = adultos.stream()
                .map(adulto -> serializarAdulto(adulto, idUsuario, inicioDia, finDia, ahora))
                .collect(Collectors.joining("\n\n"));

        String systemPrompt = esCuidador ? SYSTEM_PROMPT_CUIDADOR : SYSTEM_PROMPT_FAMILIAR;
        String userMessage = """
                Usuario autenticado: %s %s (rol: %s)
                Cantidad de adultos: %d

                Contexto por adulto mayor:
                %s
                """.formatted(usuario.getNombre(), usuario.getApellido(), rol, adultos.size(), contexto);

        String respuestaLlm = aiService.chat(systemPrompt, userMessage, MAX_TOKENS_JSON);

        BriefingIAResponse response = parsear(respuestaLlm);
        if (response == null) {
            response = respuestaPorDefecto(usuario, adultos);
        } else {
            ordenarPorPrioridad(response);
        }
        return response;
    }

    private String serializarAdulto(AdultoMayor adulto, Integer idUsuario,
            LocalDateTime inicioDia, LocalDateTime finDia, LocalDateTime ahora) {
        List<Medicamento> medicamentosActivos = medicamentoRepository
                .findByAdulto_IdAdultoAndActivoTrue(adulto.getIdAdulto());

        List<RegistroToma> tomasDelDia = registroTomaRepository
                .findTomasDelDia(adulto.getIdAdulto(), inicioDia, finDia);

        String proximaToma = registroTomaRepository.findProximaToma(adulto.getIdAdulto(), ahora)
                .map(rt -> rt.getHorario().getMedicamento().getNombre() + " a las "
                        + rt.getFechaHoraProgramada().format(HORA_FORMAT))
                .orElse("Sin tomas pendientes");

        List<Alerta> alertasActivas = alertaRepository
                .findByAdulto_IdAdultoAndResueltaFalseOrderByCreadoEnDesc(adulto.getIdAdulto());

        List<ObservacionCuidador> observaciones = observacionCuidadorRepository
                .findTop5ByAdulto_IdAdultoOrderByFechaHoraDesc(adulto.getIdAdulto());
        String ultimaObservacion = observaciones.isEmpty()
                ? "(sin observaciones registradas)"
                : "\"" + observaciones.get(0).getTexto() + "\" (registrada " + observaciones.get(0).getFechaHora() + ")";

        double adherenciaSemana;
        try {
            ReporteMedicionSemanalResponse reporte = reporteService.generarReporteSemanal(idUsuario, adulto.getIdAdulto());
            adherenciaSemana = reporte.getPorcentajeAdherencia();
        } catch (Exception ex) {
            log.warn("DashboardAiService: no se pudo generar el reporte semanal del adulto {}", adulto.getIdAdulto(), ex);
            adherenciaSemana = 0.0;
        }

        long tomasCompletadas = tomasDelDia.stream()
                .filter(rt -> "tomado".equals(rt.getEstado()) || "confirmado_manual".equals(rt.getEstado()))
                .count();
        long tomasPendientes = tomasDelDia.stream()
                .filter(rt -> "pendiente".equals(rt.getEstado()))
                .count();

        return """
                Adulto: idAdulto=%d | nombre=%s %s | condiciones médicas=%s
                Medicamentos activos: %d
                Tomas de hoy: completadas=%d, pendientes=%d
                Próxima toma: %s
                Alertas activas sin resolver: %d
                Detalle de alertas: %s
                Última observación del cuidador: %s
                Adherencia semanal: %.1f%%
                """.formatted(adulto.getIdAdulto(), adulto.getNombre(), adulto.getApellido(),
                nullSafe(adulto.getCondicionesMedicas()),
                medicamentosActivos.size(),
                tomasCompletadas, tomasPendientes,
                proximaToma,
                alertasActivas.size(),
                serializarAlertas(alertasActivas),
                ultimaObservacion,
                adherenciaSemana);
    }

    private String serializarAlertas(List<Alerta> alertas) {
        if (alertas.isEmpty()) {
            return "(sin alertas activas)";
        }
        return alertas.stream()
                .map(a -> "tipo=" + a.getTipoAlerta() + " | mensaje=" + a.getMensaje())
                .collect(Collectors.joining("; "));
    }

    private String nullSafe(String texto) {
        return (texto == null || texto.isBlank()) ? "Sin condiciones registradas" : texto;
    }

    private void ordenarPorPrioridad(BriefingIAResponse response) {
        if (response.getPacientes() == null) {
            return;
        }
        Comparator<BriefingIAResponse.PacienteBriefingDTO> porPrioridad =
                Comparator.comparingInt(p -> prioridadPeso(p.getPrioridad()));
        response.setPacientes(response.getPacientes().stream()
                .sorted(porPrioridad)
                .collect(Collectors.toList()));
    }

    private int prioridadPeso(String prioridad) {
        if (prioridad == null) {
            return 3;
        }
        return switch (prioridad.toLowerCase()) {
            case "alta" -> 0;
            case "media" -> 1;
            case "baja" -> 2;
            default -> 3;
        };
    }

    private BriefingIAResponse respuestaSinAdultos(Usuario usuario, boolean esCuidador) {
        BriefingIAResponse response = new BriefingIAResponse();
        response.setSaludo("Buenos días, " + usuario.getNombre() + ".");
        response.setResumenGeneral(esCuidador
                ? "Todavía no tenés pacientes asignados."
                : "Todavía no tenés adultos mayores vinculados a tu cuenta.");
        response.setPacientes(Collections.emptyList());
        return response;
    }

    private BriefingIAResponse respuestaPorDefecto(Usuario usuario, List<AdultoMayor> adultos) {
        BriefingIAResponse response = new BriefingIAResponse();
        response.setSaludo("Buenos días, " + usuario.getNombre() + ".");
        response.setResumenGeneral(
                "No se pudo generar el briefing inteligente en este momento. Intentá nuevamente en unos minutos.");
        response.setPacientes(adultos.stream().map(adulto -> {
            BriefingIAResponse.PacienteBriefingDTO dto = new BriefingIAResponse.PacienteBriefingDTO();
            dto.setIdAdulto(adulto.getIdAdulto());
            dto.setNombre(adulto.getNombre() + " " + adulto.getApellido());
            dto.setPrioridad("media");
            dto.setResumen("Sin resumen disponible en este momento.");
            dto.setProximaToma("No disponible");
            dto.setAdherenciaSemana(0.0);
            return dto;
        }).toList());
        return response;
    }

    private BriefingIAResponse parsear(String respuestaLlm) {
        try {
            String json = extraerJson(respuestaLlm);
            return objectMapper.readValue(json, BriefingIAResponse.class);
        } catch (Exception ex) {
            log.error("DashboardAiService: no se pudo parsear la respuesta del LLM como JSON", ex);
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
        private final BriefingIAResponse respuesta;
        private final long expiraEn;

        CachedResult(BriefingIAResponse respuesta, long expiraEn) {
            this.respuesta = respuesta;
            this.expiraEn = expiraEn;
        }

        boolean estaExpirado() {
            return System.currentTimeMillis() > expiraEn;
        }
    }
}
