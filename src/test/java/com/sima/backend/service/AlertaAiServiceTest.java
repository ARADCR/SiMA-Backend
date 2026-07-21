package com.sima.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sima.backend.dto.response.ResumenAlertasIAResponse;
import com.sima.backend.entity.AdultoMayor;
import com.sima.backend.entity.Alerta;
import com.sima.backend.exception.UnauthorizedException;
import com.sima.backend.repository.AdultoMayorRepository;
import com.sima.backend.repository.AlertaRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertaAiServiceTest {

    @Mock
    private AlertaRepository alertaRepository;

    @Mock
    private RelacionUsuarioAdultoRepository relacionUsuarioAdultoRepository;

    @Mock
    private AdultoMayorRepository adultoMayorRepository;

    @Mock
    private AiService aiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AlertaAiService alertaAiService;

    private final Integer idUsuario = 1;
    private final Integer idAdulto = 2;

    @BeforeEach
    void setUp() {
        alertaAiService = new AlertaAiService(alertaRepository, relacionUsuarioAdultoRepository,
                adultoMayorRepository, aiService, objectMapper);
    }

    @Test
    void resumirAlertas_UsuarioSinAcceso_LanzaExceptionSinLlamarAlLlm() {
        when(relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> alertaAiService.resumirAlertas(idUsuario, idAdulto));
        verifyNoInteractions(aiService);
    }

    @Test
    void resumirAlertas_DosLlamadasDentroDe15Min_SegundaNoInvocaLlm() {
        when(relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(true);
        when(adultoMayorRepository.findByIdAdultoAndActivoTrue(idAdulto)).thenReturn(Optional.of(new AdultoMayor()));
        when(alertaRepository.findByAdultoAndRango(eq(idAdulto), any(), any())).thenReturn(List.of());

        String llmJson = """
                {
                  "resumenEjecutivo": "Sin novedades.",
                  "alertasCriticas": [],
                  "alertasInformativas": 0,
                  "alertasResueltas": 0,
                  "escaladas": []
                }
                """;
        when(aiService.chat(anyString(), anyString(), anyInt())).thenReturn(llmJson);

        ResumenAlertasIAResponse primera = alertaAiService.resumirAlertas(idUsuario, idAdulto);
        ResumenAlertasIAResponse segunda = alertaAiService.resumirAlertas(idUsuario, idAdulto);

        assertSame(primera, segunda);
        verify(aiService, times(1)).chat(anyString(), anyString(), anyInt());
    }

    @Test
    void resumirAlertas_ConHistorial7DiasYOmisionRepetida_ArmaMensajeYDevuelveRespuestaLlm() {
        when(relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(true);

        AdultoMayor adulto = new AdultoMayor();
        adulto.setIdAdulto(idAdulto);
        adulto.setCondicionesMedicas("Diabetes");
        when(adultoMayorRepository.findByIdAdultoAndActivoTrue(idAdulto)).thenReturn(Optional.of(adulto));

        Alerta omisionHoy = construirAlerta(10, "OMISION_MEDICAMENTO", "Omitió insulina",
                false, LocalDateTime.now().minusHours(1));
        Alerta omisionHace3Dias = construirAlerta(7, "OMISION_MEDICAMENTO", "Omitió insulina",
                false, LocalDateTime.now().minusDays(3));
        Alerta omisionHace5Dias = construirAlerta(4, "OMISION_MEDICAMENTO", "Omitió insulina",
                false, LocalDateTime.now().minusDays(5));

        when(alertaRepository.findByAdultoAndRango(eq(idAdulto), any(), any()))
                .thenReturn(List.of(omisionHoy))
                .thenReturn(List.of(omisionHoy, omisionHace3Dias, omisionHace5Dias));

        String llmJson = """
                {
                  "resumenEjecutivo": "Omisión repetida de insulina, 3ra vez esta semana.",
                  "alertasCriticas": [
                    {"idAlerta": 10, "tipo": "OMISION_MEDICAMENTO", "mensaje": "Omitió insulina", "justificacion": "3ra omisión consecutiva"}
                  ],
                  "alertasInformativas": 0,
                  "alertasResueltas": 0,
                  "escaladas": [
                    {"descripcion": "3ra omisión de insulina en la semana", "alertasRelacionadas": [10, 7, 4], "recomendacion": "Contactar al familiar"}
                  ]
                }
                """;
        when(aiService.chat(anyString(), anyString(), anyInt())).thenReturn(llmJson);

        ResumenAlertasIAResponse response = alertaAiService.resumirAlertas(idUsuario, idAdulto);

        ArgumentCaptor<String> userMessageCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService).chat(anyString(), userMessageCaptor.capture(), anyInt());
        String userMessage = userMessageCaptor.getValue();
        assertTrue(userMessage.contains("Alertas de hoy"));
        assertTrue(userMessage.contains("Historial de alertas de los últimos 7 días"));
        assertTrue(userMessage.contains("Omitió insulina"));

        assertNotNull(response);
        assertEquals("Omisión repetida de insulina, 3ra vez esta semana.", response.getResumenEjecutivo());
        assertEquals(1, response.getAlertasCriticas().size());
        assertEquals("3ra omisión consecutiva", response.getAlertasCriticas().get(0).getJustificacion());
        assertEquals(1, response.getEscaladas().size());
        assertEquals(3, response.getEscaladas().get(0).getAlertasRelacionadas().size());
    }

    @Test
    void resumirAlertas_LlmDevuelveJsonInvalido_DevuelveFallbackConConteoRealDeResueltas() {
        when(relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(true);
        when(adultoMayorRepository.findByIdAdultoAndActivoTrue(idAdulto)).thenReturn(Optional.of(new AdultoMayor()));

        Alerta resuelta1 = construirAlerta(1, "RECORDATORIO", "Tomó su medicación", true, LocalDateTime.now());
        Alerta resuelta2 = construirAlerta(2, "RECORDATORIO", "Tomó su medicación", true, LocalDateTime.now());
        Alerta noResuelta = construirAlerta(3, "OMISION_MEDICAMENTO", "Omitió su medicación", false,
                LocalDateTime.now());

        when(alertaRepository.findByAdultoAndRango(eq(idAdulto), any(), any()))
                .thenReturn(List.of(resuelta1, resuelta2, noResuelta));
        when(aiService.chat(anyString(), anyString(), anyInt())).thenReturn("respuesta invalida sin json");

        ResumenAlertasIAResponse response = alertaAiService.resumirAlertas(idUsuario, idAdulto);

        assertNotNull(response);
        assertNotNull(response.getResumenEjecutivo());
        assertTrue(response.getAlertasCriticas().isEmpty());
        assertTrue(response.getEscaladas().isEmpty());
        assertEquals(2, response.getAlertasResueltas());
        assertEquals(0, response.getAlertasInformativas());
    }

    private Alerta construirAlerta(Integer idAlerta, String tipo, String mensaje, boolean resuelta,
            LocalDateTime creadoEn) {
        Alerta alerta = new Alerta();
        alerta.setIdAlerta(idAlerta);
        alerta.setTipoAlerta(tipo);
        alerta.setMensaje(mensaje);
        alerta.setResuelta(resuelta);
        alerta.setCreadoEn(creadoEn);
        return alerta;
    }
}
