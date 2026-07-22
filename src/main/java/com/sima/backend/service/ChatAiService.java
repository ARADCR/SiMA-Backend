package com.sima.backend.service;

import com.sima.backend.config.AiRateLimiter;
import com.sima.backend.dto.request.ChatRequest;
import com.sima.backend.dto.response.ChatResponse;
import com.sima.backend.entity.Alerta;
import com.sima.backend.entity.EventoIot;
import com.sima.backend.entity.Medicamento;
import com.sima.backend.entity.ObservacionCuidador;
import com.sima.backend.entity.RegistroToma;
import com.sima.backend.exception.TooManyRequestsException;
import com.sima.backend.exception.UnauthorizedException;
import com.sima.backend.repository.AlertaRepository;
import com.sima.backend.repository.EventoIotRepository;
import com.sima.backend.repository.MedicamentoRepository;
import com.sima.backend.repository.ObservacionCuidadorRepository;
import com.sima.backend.repository.RegistroTomaRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ChatAiService {

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String SYSTEM_PROMPT_FAMILIAR = """
            Sos el asistente de IA de SiMA, una plataforma de monitoreo de adultos mayores.
            Estás hablando con un FAMILIAR. Tu tono es cálido, tranquilizador y en español simple, sin jerga médica.
            Respondé de forma concisa usando ÚNICAMENTE los datos del paciente que se te proveen a continuación.
            No inventes datos médicos que no estén en el contexto. No des diagnósticos ni indiques cambios de dosis.
            Si la consulta requiere criterio médico, sugerí consultar al médico o profesional de salud correspondiente.
            """;

    private static final String SYSTEM_PROMPT_CUIDADOR = """
            Sos el asistente de IA de SiMA, una plataforma de monitoreo de adultos mayores.
            Estás hablando con un CUIDADOR profesional. Tu tono es técnico y profesional.
            Respondé de forma concisa usando ÚNICAMENTE los datos del paciente que se te proveen a continuación.
            Podés dar guía sobre protocolos de cuidado y contextualizar signos vitales, pero NO prescribas
            medicación ni des diagnósticos definitivos — para eso siempre referí al médico tratante.
            """;

    private final RelacionUsuarioAdultoRepository relacionUsuarioAdultoRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final RegistroTomaRepository registroTomaRepository;
    private final AlertaRepository alertaRepository;
    private final ObservacionCuidadorRepository observacionCuidadorRepository;
    private final EventoIotRepository eventoIotRepository;
    private final AiService aiService;
    private final AiRateLimiter rateLimiter;

    public ChatAiService(RelacionUsuarioAdultoRepository relacionUsuarioAdultoRepository,
            MedicamentoRepository medicamentoRepository,
            RegistroTomaRepository registroTomaRepository,
            AlertaRepository alertaRepository,
            ObservacionCuidadorRepository observacionCuidadorRepository,
            EventoIotRepository eventoIotRepository,
            AiService aiService,
            AiRateLimiter rateLimiter) {
        this.relacionUsuarioAdultoRepository = relacionUsuarioAdultoRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.registroTomaRepository = registroTomaRepository;
        this.alertaRepository = alertaRepository;
        this.observacionCuidadorRepository = observacionCuidadorRepository;
        this.eventoIotRepository = eventoIotRepository;
        this.aiService = aiService;
        this.rateLimiter = rateLimiter;
    }

    public ChatResponse chat(Integer idUsuario, String rol, ChatRequest request) {
        if (!rateLimiter.allowRequest(idUsuario)) {
            throw new TooManyRequestsException("Límite de mensajes de IA excedido. Intentá nuevamente en un minuto.");
        }

        if (!relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, request.getIdAdulto())) {
            throw new UnauthorizedException("No tenés acceso a este adulto mayor");
        }

        String systemPrompt = construirSystemPrompt(rol, request.getIdAdulto());
        String respuesta = aiService.chat(systemPrompt, request.getMensaje());

        return new ChatResponse(respuesta);
    }

    private String construirSystemPrompt(String rol, Integer idAdulto) {
        String base = "Cuidador".equals(rol) ? SYSTEM_PROMPT_CUIDADOR : SYSTEM_PROMPT_FAMILIAR;
        return base + "\n\n" + construirContexto(idAdulto);
    }

    private String construirContexto(Integer idAdulto) {
        StringBuilder sb = new StringBuilder("=== CONTEXTO DEL PACIENTE ===\n\n");

        sb.append("Medicamentos activos:\n");
        List<Medicamento> medicamentos = medicamentoRepository.findByAdulto_IdAdultoAndActivoTrue(idAdulto);
        if (medicamentos.isEmpty()) {
            sb.append("- Sin medicamentos activos registrados.\n");
        } else {
            for (Medicamento m : medicamentos) {
                sb.append("- %s (%s), cada %d horas, principio activo: %s, stock: %s\n".formatted(
                        m.getNombre(), m.getDosis(), m.getFrecuenciaHoras(),
                        m.getPrincipioActivo() != null ? m.getPrincipioActivo() : "no especificado",
                        m.getStockActual() != null ? m.getStockActual() : "no especificado"));
            }
        }

        sb.append("\nÚltimas tomas registradas:\n");
        List<RegistroToma> tomas = registroTomaRepository.findTop10ByAdulto_IdAdultoOrderByFechaHoraProgramadaDesc(idAdulto);
        if (tomas.isEmpty()) {
            sb.append("- Sin tomas registradas.\n");
        } else {
            for (RegistroToma t : tomas) {
                String medicamento = t.getHorario() != null && t.getHorario().getMedicamento() != null
                        ? t.getHorario().getMedicamento().getNombre()
                        : "medicamento no especificado";
                sb.append("- %s, programada %s, estado: %s%s\n".formatted(
                        medicamento,
                        t.getFechaHoraProgramada().format(FECHA_FMT),
                        t.getEstado(),
                        t.getMetodoConfirmacion() != null ? " (" + t.getMetodoConfirmacion() + ")" : ""));
            }
        }

        sb.append("\nAlertas activas no resueltas:\n");
        List<Alerta> alertas = alertaRepository.findByAdulto_IdAdultoAndResueltaFalseOrderByCreadoEnDesc(idAdulto);
        if (alertas.isEmpty()) {
            sb.append("- Sin alertas activas.\n");
        } else {
            for (Alerta a : alertas) {
                sb.append("- [%s] %s (%s)\n".formatted(a.getTipoAlerta(), a.getMensaje(), a.getCreadoEn().format(FECHA_FMT)));
            }
        }

        sb.append("\nÚltimas observaciones del cuidador:\n");
        List<ObservacionCuidador> observaciones = observacionCuidadorRepository.findTop5ByAdulto_IdAdultoOrderByFechaHoraDesc(idAdulto);
        if (observaciones.isEmpty()) {
            sb.append("- Sin observaciones registradas.\n");
        } else {
            for (ObservacionCuidador o : observaciones) {
                sb.append("- %s: \"%s\" (urgencia: %s)\n".formatted(
                        o.getFechaHora().format(FECHA_FMT), o.getTexto(), o.getUrgencia()));
            }
        }

        sb.append("\nÚltimos eventos IoT:\n");
        List<EventoIot> eventos = eventoIotRepository.findTop10ByDispositivo_Adulto_IdAdultoOrderByFechaHoraDesc(idAdulto);
        if (eventos.isEmpty()) {
            sb.append("- Sin eventos IoT registrados.\n");
        } else {
            for (EventoIot e : eventos) {
                sb.append("- %s: %s (%s)\n".formatted(
                        e.getTipoEvento(),
                        e.getValor() != null ? e.getValor().toString() : "sin valor",
                        e.getFechaHora().format(FECHA_FMT)));
            }
        }

        return sb.toString();
    }
}
