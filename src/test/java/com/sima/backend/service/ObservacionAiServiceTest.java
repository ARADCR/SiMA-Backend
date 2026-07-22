package com.sima.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sima.backend.dto.request.EvaluarUrgenciaRequest;
import com.sima.backend.dto.response.EvaluacionUrgenciaResponse;
import com.sima.backend.dto.response.ResumenObservacionesResponse;
import com.sima.backend.entity.AdultoMayor;
import com.sima.backend.entity.Medicamento;
import com.sima.backend.entity.ObservacionCuidador;
import com.sima.backend.entity.Usuario;
import com.sima.backend.exception.UnauthorizedException;
import com.sima.backend.repository.AdultoMayorRepository;
import com.sima.backend.repository.MedicamentoRepository;
import com.sima.backend.repository.ObservacionCuidadorRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class ObservacionAiServiceTest {

    @Mock
    private ObservacionCuidadorRepository observacionRepository;

    @Mock
    private RelacionUsuarioAdultoRepository relacionUsuarioAdultoRepository;

    @Mock
    private AdultoMayorRepository adultoMayorRepository;

    @Mock
    private MedicamentoRepository medicamentoRepository;

    @Mock
    private AiService aiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ObservacionAiService observacionAiService;

    private final Integer idUsuario = 1;
    private final Integer idAdulto = 2;

    @BeforeEach
    void setUp() {
        observacionAiService = new ObservacionAiService(observacionRepository, relacionUsuarioAdultoRepository,
                adultoMayorRepository, medicamentoRepository, aiService, objectMapper);
    }

    // ---------- resumir ----------

    @Test
    void resumir_UsuarioSinAcceso_LanzaException() {
        when(relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> observacionAiService.resumir(idUsuario, idAdulto));
        verifyNoInteractions(aiService);
    }

    @Test
    void resumir_5ObservacionesCon1Urgente_ResumenLaDestacaPrimero() {
        when(relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(true);

        AdultoMayor adulto = new AdultoMayor();
        adulto.setIdAdulto(idAdulto);
        adulto.setCondicionesMedicas("Hipertensión");
        when(adultoMayorRepository.findByIdAdultoAndActivoTrue(idAdulto)).thenReturn(Optional.of(adulto));

        ObservacionCuidador urgente = construirObs("Rosa presentó presión elevada", "urgente",
                "145/95", null, "37.8", LocalDateTime.now().minusHours(20));
        ObservacionCuidador rutinaria1 = construirObs("Estado de ánimo estable", "normal",
                null, null, null, LocalDateTime.now().minusHours(10));
        ObservacionCuidador rutinaria2 = construirObs("Almorzó bien", "normal",
                null, null, null, LocalDateTime.now().minusHours(5));

        when(observacionRepository.findByAdultoAndRango(eq(idAdulto), any(), any()))
                .thenReturn(List.of(urgente, rutinaria1, rutinaria2));

        String llmJson = """
                {
                  "resumen": "La observación más urgente fue la presión arterial elevada de Rosa.",
                  "observacionesAnalizadas": 3,
                  "periodoAnalizado": "Últimas 72 horas",
                  "alertasIdentificadas": ["Presión arterial elevada: 145/95 mmHg"]
                }
                """;
        when(aiService.chat(anyString(), anyString(), anyInt())).thenReturn(llmJson);

        ResumenObservacionesResponse response = observacionAiService.resumir(idUsuario, idAdulto);

        assertNotNull(response);
        assertTrue(response.getResumen().contains("presión"));
        assertEquals(3, response.getObservacionesAnalizadas());
        assertEquals(1, response.getAlertasIdentificadas().size());
    }

    @Test
    void resumir_Con11Observaciones_TomaSoloLasUltimas10() {
        when(relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(true);
        when(adultoMayorRepository.findByIdAdultoAndActivoTrue(idAdulto)).thenReturn(Optional.of(new AdultoMayor()));

        List<ObservacionCuidador> observaciones = new java.util.ArrayList<>();
        for (int i = 0; i < 11; i++) {
            observaciones.add(construirObs("Obs " + i, "normal", null, null, null,
                    LocalDateTime.now().minusHours(i)));
        }
        when(observacionRepository.findByAdultoAndRango(eq(idAdulto), any(), any())).thenReturn(observaciones);

        when(aiService.chat(anyString(), anyString(), anyInt())).thenReturn("""
                {"resumen": "ok", "observacionesAnalizadas": 10, "periodoAnalizado": "Últimas 72 horas", "alertasIdentificadas": []}
                """);

        ResumenObservacionesResponse response = observacionAiService.resumir(idUsuario, idAdulto);

        assertEquals(10, response.getObservacionesAnalizadas());
    }

    @Test
    void resumir_LlmFallaAlParsear_DevuelveFallback() {
        when(relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(true);
        when(adultoMayorRepository.findByIdAdultoAndActivoTrue(idAdulto)).thenReturn(Optional.of(new AdultoMayor()));
        when(observacionRepository.findByAdultoAndRango(eq(idAdulto), any(), any())).thenReturn(List.of());
        when(aiService.chat(anyString(), anyString(), anyInt())).thenReturn("respuesta invalida sin json");

        ResumenObservacionesResponse response = observacionAiService.resumir(idUsuario, idAdulto);

        assertNotNull(response);
        assertNotNull(response.getResumen());
        assertTrue(response.getAlertasIdentificadas().isEmpty());
    }

    // ---------- evaluarUrgencia ----------

    @Test
    void evaluarUrgencia_UsuarioSinAcceso_LanzaException() {
        when(relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(false);

        EvaluarUrgenciaRequest request = new EvaluarUrgenciaRequest();
        request.setIdAdulto(idAdulto);

        assertThrows(UnauthorizedException.class, () -> observacionAiService.evaluarUrgencia(idUsuario, request));
        verifyNoInteractions(aiService);
    }

    @Test
    void evaluarUrgencia_TA180110ConHipertensionYLosartan_SugiereUrgente() {
        when(relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(true);

        AdultoMayor adulto = new AdultoMayor();
        adulto.setIdAdulto(idAdulto);
        adulto.setCondicionesMedicas("Hipertensión");
        when(adultoMayorRepository.findByIdAdultoAndActivoTrue(idAdulto)).thenReturn(Optional.of(adulto));

        Medicamento losartan = new Medicamento();
        losartan.setNombre("Losartán");
        when(medicamentoRepository.findByAdulto_IdAdultoAndActivoTrue(idAdulto)).thenReturn(List.of(losartan));

        when(observacionRepository.findTop5ByAdulto_IdAdultoOrderByFechaHoraDesc(idAdulto))
                .thenReturn(List.of());

        String llmJson = """
                {
                  "urgenciaSugerida": "urgente",
                  "justificacion": "TA 180/110 significativamente superior al rango esperado bajo tratamiento.",
                  "valoresAnormales": ["Tensión arterial elevada: 180/110 mmHg"],
                  "recomendaciones": ["Verificar toma de medicación", "Contactar al médico tratante"]
                }
                """;
        when(aiService.chat(anyString(), anyString(), anyInt())).thenReturn(llmJson);

        EvaluarUrgenciaRequest request = new EvaluarUrgenciaRequest();
        request.setIdAdulto(idAdulto);
        request.setTensionArterial("180/110");
        request.setTextoObservacion("Paciente refiere dolor de cabeza");

        EvaluacionUrgenciaResponse response = observacionAiService.evaluarUrgencia(idUsuario, request);

        assertNotNull(response);
        assertEquals("urgente", response.getUrgenciaSugerida());
        assertEquals(1, response.getValoresAnormales().size());
        assertEquals(2, response.getRecomendaciones().size());
    }

    @Test
    void evaluarUrgencia_TA120x80EnPacienteSano_SugiereNormal() {
        when(relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(true);
        when(adultoMayorRepository.findByIdAdultoAndActivoTrue(idAdulto)).thenReturn(Optional.of(new AdultoMayor()));
        when(medicamentoRepository.findByAdulto_IdAdultoAndActivoTrue(idAdulto)).thenReturn(List.of());
        when(observacionRepository.findTop5ByAdulto_IdAdultoOrderByFechaHoraDesc(idAdulto)).thenReturn(List.of());

        String llmJson = """
                {
                  "urgenciaSugerida": "normal",
                  "justificacion": "Los valores están dentro de rangos saludables.",
                  "valoresAnormales": [],
                  "recomendaciones": []
                }
                """;
        when(aiService.chat(anyString(), anyString(), anyInt())).thenReturn(llmJson);

        EvaluarUrgenciaRequest request = new EvaluarUrgenciaRequest();
        request.setIdAdulto(idAdulto);
        request.setTensionArterial("120/80");

        EvaluacionUrgenciaResponse response = observacionAiService.evaluarUrgencia(idUsuario, request);

        assertEquals("normal", response.getUrgenciaSugerida());
        assertTrue(response.getValoresAnormales().isEmpty());
    }

    @Test
    void evaluarUrgencia_LlmFallaAlParsear_DevuelveFallbackNormal() {
        when(relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(true);
        when(adultoMayorRepository.findByIdAdultoAndActivoTrue(idAdulto)).thenReturn(Optional.of(new AdultoMayor()));
        when(medicamentoRepository.findByAdulto_IdAdultoAndActivoTrue(idAdulto)).thenReturn(List.of());
        when(observacionRepository.findTop5ByAdulto_IdAdultoOrderByFechaHoraDesc(idAdulto)).thenReturn(List.of());
        when(aiService.chat(anyString(), anyString(), anyInt())).thenReturn("respuesta invalida sin json");

        EvaluarUrgenciaRequest request = new EvaluarUrgenciaRequest();
        request.setIdAdulto(idAdulto);
        request.setTensionArterial("120/80");

        EvaluacionUrgenciaResponse response = observacionAiService.evaluarUrgencia(idUsuario, request);

        assertNotNull(response);
        assertEquals("normal", response.getUrgenciaSugerida());
        assertNotNull(response.getJustificacion());
    }

    private ObservacionCuidador construirObs(String texto, String urgencia, String ta, String fc, String temp,
            LocalDateTime fecha) {
        ObservacionCuidador obs = new ObservacionCuidador();
        obs.setTexto(texto);
        obs.setUrgencia(urgencia);
        obs.setTensionArterial(ta);
        obs.setFrecuenciaCardiaca(fc);
        obs.setTemperatura(temp);
        obs.setFechaHora(fecha);

        Usuario cuidador = new Usuario();
        cuidador.setNombre("Carlos");
        obs.setCuidador(cuidador);

        return obs;
    }
}
