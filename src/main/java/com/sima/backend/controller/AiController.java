package com.sima.backend.controller;

import com.sima.backend.config.AiRateLimiter;
import com.sima.backend.dto.request.ActualizarPerfilCuidadorRequest;
import com.sima.backend.dto.request.AnalisisPerfilRequest;
import com.sima.backend.dto.request.BusquedaCuidadorIARequest;
import com.sima.backend.dto.request.ChatRequest;
import com.sima.backend.dto.request.EvaluarUrgenciaRequest;
import com.sima.backend.dto.response.AnalisisIotIAResponse;
import com.sima.backend.dto.response.AnalisisPerfilResponse;
import com.sima.backend.dto.response.ApiResponse;
import com.sima.backend.dto.response.BriefingIAResponse;
import com.sima.backend.dto.response.BusquedaCuidadorIAResponse;
import com.sima.backend.dto.response.ChatResponse;
import com.sima.backend.dto.response.EvaluacionUrgenciaResponse;
import com.sima.backend.dto.response.MatchCuidadorResponse;
import com.sima.backend.dto.response.PatronesAdherenciaResponse;
import com.sima.backend.dto.response.PerfilCuidadorResponse;
import com.sima.backend.dto.response.ResumenAlertasIAResponse;
import com.sima.backend.dto.response.ResumenObservacionesResponse;
import com.sima.backend.dto.response.ResumenReporteIAResponse;
import com.sima.backend.exception.TooManyRequestsException;
import com.sima.backend.security.CustomUserDetails;
import com.sima.backend.service.AdherenciaAiService;
import com.sima.backend.service.AlertaAiService;
import com.sima.backend.service.ChatAiService;
import com.sima.backend.service.DashboardAiService;
import com.sima.backend.service.IotAiService;
import com.sima.backend.service.ObservacionAiService;
import com.sima.backend.service.PerfilAiService;
import com.sima.backend.service.RecomendacionAiService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Value("${spring.ai.openai.chat.options.model}")
    private String modelo;

    private final AiRateLimiter rateLimiter;
    private final ChatAiService chatAiService;
    private final RecomendacionAiService recomendacionAiService;
    private final PerfilAiService perfilAiService;
    private final AdherenciaAiService adherenciaAiService;
    private final AlertaAiService alertaAiService;
    private final ObservacionAiService observacionAiService;
    private final DashboardAiService dashboardAiService;
    private final IotAiService iotAiService;

    public AiController(AiRateLimiter rateLimiter, ChatAiService chatAiService,
            RecomendacionAiService recomendacionAiService, PerfilAiService perfilAiService,
            AdherenciaAiService adherenciaAiService, AlertaAiService alertaAiService,
            ObservacionAiService observacionAiService, DashboardAiService dashboardAiService,
            IotAiService iotAiService) {
        this.rateLimiter = rateLimiter;
        this.chatAiService = chatAiService;
        this.recomendacionAiService = recomendacionAiService;
        this.perfilAiService = perfilAiService;
        this.adherenciaAiService = adherenciaAiService;
        this.alertaAiService = alertaAiService;
        this.observacionAiService = observacionAiService;
        this.dashboardAiService = dashboardAiService;
        this.iotAiService = iotAiService;
    }

    // GET /api/ai/health -> verifica conectividad y modelo configurado
    @GetMapping("/health")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> health(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!rateLimiter.allowRequest(userDetails.getIdUsuario())) {
            throw new TooManyRequestsException("Límite de solicitudes de IA excedido. Intentá nuevamente en un minuto.");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("estado", "conectado");
        data.put("modelo", modelo);
        data.put("proveedor", "DeepSeek");

        return ApiResponse.ok("Infraestructura de IA operativa", data);
    }

    // POST /api/ai/chat -> chatbot IA contextualizado (Familiar/Cuidador)
    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('Familiar', 'Cuidador')")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ChatResponse respuesta = chatAiService.chat(userDetails.getIdUsuario(), userDetails.getRol(), request);
        return ApiResponse.ok("Respuesta generada", respuesta);
    }

    // POST /api/ai/buscar-cuidador -> búsqueda de cuidadores con lenguaje natural
    @PostMapping("/buscar-cuidador")
    @PreAuthorize("hasRole('Familiar')")
    public ApiResponse<BusquedaCuidadorIAResponse> buscarCuidador(@Valid @RequestBody BusquedaCuidadorIARequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!rateLimiter.allowRequest(userDetails.getIdUsuario())) {
            throw new TooManyRequestsException("Límite de solicitudes de IA excedido. Intentá nuevamente en un minuto.");
        }
        BusquedaCuidadorIAResponse respuesta = recomendacionAiService.buscarConIA(userDetails.getIdUsuario(), request);
        return ApiResponse.ok("Búsqueda procesada", respuesta);
    }

    // GET /api/ai/match-cuidador/{idCuidador}?idAdulto={id} -> score de compatibilidad
    @GetMapping("/match-cuidador/{idCuidador}")
    @PreAuthorize("hasRole('Familiar')")
    public ApiResponse<MatchCuidadorResponse> matchCuidador(@PathVariable Integer idCuidador,
            @RequestParam Integer idAdulto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!rateLimiter.allowRequest(userDetails.getIdUsuario())) {
            throw new TooManyRequestsException("Límite de solicitudes de IA excedido. Intentá nuevamente en un minuto.");
        }
        MatchCuidadorResponse respuesta = recomendacionAiService.calcularMatch(userDetails.getIdUsuario(), idCuidador, idAdulto);
        return ApiResponse.ok("Match calculado", respuesta);
    }

    // POST /api/ai/analizar-perfil -> extracción de datos estructurados del perfil del Cuidador
    @PostMapping("/analizar-perfil")
    @PreAuthorize("hasRole('Cuidador')")
    public ApiResponse<AnalisisPerfilResponse> analizarPerfil(@Valid @RequestBody AnalisisPerfilRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!rateLimiter.allowRequest(userDetails.getIdUsuario())) {
            throw new TooManyRequestsException("Límite de solicitudes de IA excedido. Intentá nuevamente en un minuto.");
        }
        AnalisisPerfilResponse respuesta = perfilAiService.analizarPerfil(userDetails.getIdUsuario(), request);
        return ApiResponse.ok("Perfil analizado", respuesta);
    }

    // GET /api/ai/perfil-cuidador -> devuelve el perfil analizado y persistido del cuidador autenticado
    @GetMapping("/perfil-cuidador")
    @PreAuthorize("hasRole('Cuidador')")
    public ApiResponse<PerfilCuidadorResponse> obtenerPerfilCuidador(@AuthenticationPrincipal CustomUserDetails userDetails) {
        PerfilCuidadorResponse respuesta = perfilAiService.obtenerPerfil(userDetails.getIdUsuario());
        return ApiResponse.ok("Perfil obtenido", respuesta);
    }

    // PUT /api/ai/perfil-cuidador -> confirma la revisión del cuidador sobre el análisis (ej. tags editados)
    @PutMapping("/perfil-cuidador")
    @PreAuthorize("hasRole('Cuidador')")
    public ApiResponse<Void> actualizarPerfilCuidador(@RequestBody ActualizarPerfilCuidadorRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        perfilAiService.actualizarRevision(userDetails.getIdUsuario(), request);
        return ApiResponse.ok("Perfil actualizado");
    }

    // GET /api/ai/reporte/{idAdulto}/resumen -> resumen narrativo del reporte semanal de adherencia
    @GetMapping("/reporte/{idAdulto}/resumen")
    @PreAuthorize("hasAnyRole('Familiar', 'Cuidador')")
    public ApiResponse<ResumenReporteIAResponse> resumenReporte(@PathVariable Integer idAdulto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!rateLimiter.allowRequest(userDetails.getIdUsuario())) {
            throw new TooManyRequestsException("Límite de solicitudes de IA excedido. Intentá nuevamente en un minuto.");
        }
        ResumenReporteIAResponse respuesta = adherenciaAiService.generarResumen(userDetails.getIdUsuario(), idAdulto);
        return ApiResponse.ok("Resumen generado", respuesta);
    }

    // GET /api/ai/reporte/{idAdulto}/patrones -> patrones de omisión detectados sobre las últimas 4 semanas
    @GetMapping("/reporte/{idAdulto}/patrones")
    @PreAuthorize("hasAnyRole('Familiar', 'Cuidador')")
    public ApiResponse<PatronesAdherenciaResponse> patronesAdherencia(@PathVariable Integer idAdulto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!rateLimiter.allowRequest(userDetails.getIdUsuario())) {
            throw new TooManyRequestsException("Límite de solicitudes de IA excedido. Intentá nuevamente en un minuto.");
        }
        PatronesAdherenciaResponse respuesta = adherenciaAiService.detectarPatrones(userDetails.getIdUsuario(), idAdulto);
        return ApiResponse.ok("Patrones detectados", respuesta);
    }

    // GET /api/ai/alertas/{idAdulto}/resumen -> resumen priorizado y agrupado de las alertas del día
    @GetMapping("/alertas/{idAdulto}/resumen")
    @PreAuthorize("hasAnyRole('Familiar', 'Cuidador')")
    public ApiResponse<ResumenAlertasIAResponse> resumenAlertas(@PathVariable Integer idAdulto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!rateLimiter.allowRequest(userDetails.getIdUsuario())) {
            throw new TooManyRequestsException("Límite de solicitudes de IA excedido. Intentá nuevamente en un minuto.");
        }
        ResumenAlertasIAResponse respuesta = alertaAiService.resumirAlertas(userDetails.getIdUsuario(), idAdulto);
        return ApiResponse.ok("Resumen de alertas generado", respuesta);
    }

    // GET /api/ai/observaciones/{idAdulto}/resumen -> resumen consolidado de observaciones recientes
    @GetMapping("/observaciones/{idAdulto}/resumen")
    @PreAuthorize("hasAnyRole('Familiar', 'Cuidador')")
    public ApiResponse<ResumenObservacionesResponse> resumenObservaciones(@PathVariable Integer idAdulto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!rateLimiter.allowRequest(userDetails.getIdUsuario())) {
            throw new TooManyRequestsException("Límite de solicitudes de IA excedido. Intentá nuevamente en un minuto.");
        }
        ResumenObservacionesResponse respuesta = observacionAiService.resumir(userDetails.getIdUsuario(), idAdulto);
        return ApiResponse.ok("Resumen generado", respuesta);
    }

    // POST /api/ai/observaciones/evaluar-urgencia -> evaluación de urgencia de signos vitales (solo Cuidador)
    @PostMapping("/observaciones/evaluar-urgencia")
    @PreAuthorize("hasRole('Cuidador')")
    public ApiResponse<EvaluacionUrgenciaResponse> evaluarUrgencia(@Valid @RequestBody EvaluarUrgenciaRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!rateLimiter.allowRequest(userDetails.getIdUsuario())) {
            throw new TooManyRequestsException("Límite de solicitudes de IA excedido. Intentá nuevamente en un minuto.");
        }
        EvaluacionUrgenciaResponse respuesta = observacionAiService.evaluarUrgencia(userDetails.getIdUsuario(), request);
        return ApiResponse.ok("Urgencia evaluada", respuesta);
    }

    // GET /api/ai/dashboard/briefing -> briefing diario inteligente adaptado al rol (HU-25)
    @GetMapping("/dashboard/briefing")
    @PreAuthorize("hasAnyRole('Familiar', 'Cuidador')")
    public ApiResponse<BriefingIAResponse> obtenerBriefing(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!rateLimiter.allowRequest(userDetails.getIdUsuario())) {
            throw new TooManyRequestsException("Límite de solicitudes de IA excedido. Intentá nuevamente en un minuto.");
        }
        BriefingIAResponse respuesta = dashboardAiService.generarBriefing(userDetails.getIdUsuario());
        return ApiResponse.ok("Briefing generado", respuesta);
    }

    // POST /api/ai/dashboard/briefing/refresh -> fuerza regeneración del briefing (invalida cache)
    @PostMapping("/dashboard/briefing/refresh")
    @PreAuthorize("hasAnyRole('Familiar', 'Cuidador')")
    public ApiResponse<BriefingIAResponse> refrescarBriefing(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!rateLimiter.allowRequest(userDetails.getIdUsuario())) {
            throw new TooManyRequestsException("Límite de solicitudes de IA excedido. Intentá nuevamente en un minuto.");
        }
        BriefingIAResponse respuesta = dashboardAiService.refrescarBriefing(userDetails.getIdUsuario());
        return ApiResponse.ok("Briefing regenerado", respuesta);
    }

    // GET /api/ai/iot/{idAdulto}/analisis -> detección de anomalías IoT contextualizada con IA (HU-26)
    @GetMapping("/iot/{idAdulto}/analisis")
    @PreAuthorize("hasAnyRole('Familiar', 'Cuidador')")
    public ApiResponse<AnalisisIotIAResponse> analisisIot(@PathVariable Integer idAdulto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AnalisisIotIAResponse respuesta = iotAiService.obtenerUltimoAnalisis(userDetails.getIdUsuario(), idAdulto);
        return ApiResponse.ok("Análisis IoT obtenido", respuesta);
    }
}
