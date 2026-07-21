package com.sima.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sima.backend.dto.response.PatronesAdherenciaResponse;
import com.sima.backend.dto.response.ResumenReporteIAResponse;
import com.sima.backend.entity.AdultoMayor;
import com.sima.backend.entity.HorarioMedicamento;
import com.sima.backend.entity.Medicamento;
import com.sima.backend.entity.RegistroToma;
import com.sima.backend.exception.UnauthorizedException;
import com.sima.backend.repository.AdultoMayorRepository;
import com.sima.backend.repository.MedicamentoRepository;
import com.sima.backend.repository.RegistroTomaRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdherenciaAiServiceTest {

    // ReporteService es una clase concreta: se instancia real (con sus propias dependencias mockeadas)
    // en lugar de mockearla directamente, porque el entorno de test (Java 23) no soporta el mock inline
    // de ByteBuddy sobre clases concretas. Esto también verifica que AdherenciaAiService reutiliza
    // la lógica real de ReporteService sin duplicarla.
    @Mock
    private RegistroTomaRepository registroTomaRepository;

    @Mock
    private RelacionUsuarioAdultoRepository relacionUsuarioAdultoRepository;

    @Mock
    private AdultoMayorRepository adultoMayorRepository;

    @Mock
    private MedicamentoRepository medicamentoRepository;

    @Mock
    private AiService aiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AdherenciaAiService adherenciaAiService;

    private final Integer idUsuario = 1;
    private final Integer idAdulto = 2;

    @BeforeEach
    void setUp() {
        ReporteService reporteService = new ReporteService(registroTomaRepository, relacionUsuarioAdultoRepository,
                medicamentoRepository);
        adherenciaAiService = new AdherenciaAiService(reporteService, registroTomaRepository,
                relacionUsuarioAdultoRepository, adultoMayorRepository, aiService, objectMapper);
    }

    @Test
    void generarResumen_UsuarioSinAcceso_LanzaException() {
        when(relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(false);

        assertThrows(UnauthorizedException.class,
                () -> adherenciaAiService.generarResumen(idUsuario, idAdulto));
    }

    @Test
    void generarResumen_ConDatosReales_MencionaMedicamentosOmitidos() {
        when(relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(true);

        RegistroToma tomaOmitida = construirToma("Losartan", "omitido", LocalDateTime.now().minusDays(1));
        when(registroTomaRepository.findHistorialByAdultoAndRango(eq(idAdulto), any(), any()))
                .thenReturn(List.of(tomaOmitida));

        AdultoMayor adulto = new AdultoMayor();
        adulto.setIdAdulto(idAdulto);
        adulto.setCondicionesMedicas("Hipertensión");
        when(adultoMayorRepository.findByIdAdultoAndActivoTrue(idAdulto)).thenReturn(java.util.Optional.of(adulto));

        String llmJson = """
                {
                  "resumenNarrativo": "Elena tuvo una semana regular. El principal problema fue el Losartan.",
                  "puntosClave": ["Losartan omitido 4 veces"],
                  "recomendaciones": ["Ajustar horario nocturno"]
                }
                """;
        when(aiService.chat(anyString(), anyString(), anyInt())).thenReturn(llmJson);

        ResumenReporteIAResponse response = adherenciaAiService.generarResumen(idUsuario, idAdulto);

        assertNotNull(response);
        assertTrue(response.getResumenNarrativo().contains("Losartan"));
        assertEquals(1, response.getPuntosClave().size());
        assertEquals(1, response.getRecomendaciones().size());
    }

    @Test
    void generarResumen_LlmFallaAlParsear_DevuelveFallback() {
        when(relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(true);
        when(registroTomaRepository.findHistorialByAdultoAndRango(eq(idAdulto), any(), any()))
                .thenReturn(Collections.emptyList());
        when(adultoMayorRepository.findByIdAdultoAndActivoTrue(idAdulto)).thenReturn(java.util.Optional.of(new AdultoMayor()));
        when(aiService.chat(anyString(), anyString(), anyInt())).thenReturn("respuesta invalida sin json");

        ResumenReporteIAResponse response = adherenciaAiService.generarResumen(idUsuario, idAdulto);

        assertNotNull(response);
        assertNotNull(response.getResumenNarrativo());
        assertTrue(response.getPuntosClave().isEmpty());
        assertTrue(response.getRecomendaciones().isEmpty());
    }

    @Test
    void detectarPatrones_UsuarioSinAcceso_LanzaException() {
        when(relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(false);

        assertThrows(UnauthorizedException.class,
                () -> adherenciaAiService.detectarPatrones(idUsuario, idAdulto));
    }

    @Test
    void detectarPatrones_MenosDe7DiasDeDatos_DevuelveMensajeInformativo() {
        when(relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(true);

        RegistroToma toma = construirToma("Metformina", "tomado", LocalDateTime.now().minusDays(2));
        when(registroTomaRepository.findHistorialByAdultoAndRango(eq(idAdulto), any(), any()))
                .thenReturn(List.of(toma));

        PatronesAdherenciaResponse response = adherenciaAiService.detectarPatrones(idUsuario, idAdulto);

        assertNotNull(response);
        assertNotNull(response.getMensajeInformativo());
        assertTrue(response.getMensajeInformativo().contains("7 días"));
        assertTrue(response.getPatronesDetectados() == null || response.getPatronesDetectados().isEmpty());
        verifyNoInteractions(aiService);
    }

    @Test
    void detectarPatrones_4SemanasConOmisionesNocturnas_DetectaPatronTemporal() {
        when(relacionUsuarioAdultoRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(true);

        List<RegistroToma> registros = new java.util.ArrayList<>();
        for (int i = 0; i < 28; i++) {
            registros.add(construirToma("Losartan", i % 2 == 0 ? "omitido" : "tomado",
                    LocalDateTime.now().minusDays(i).withHour(20)));
        }
        when(registroTomaRepository.findHistorialByAdultoAndRango(eq(idAdulto), any(), any()))
                .thenReturn(registros);

        String llmJson = """
                {
                  "patronesDetectados": [
                    {
                      "tipo": "temporal",
                      "descripcion": "Omite consistentemente la toma de las 8PM",
                      "severidad": "alta",
                      "recomendacion": "Configurar recordatorio adicional a las 8PM"
                    }
                  ]
                }
                """;
        when(aiService.chat(anyString(), anyString(), anyInt())).thenReturn(llmJson);

        PatronesAdherenciaResponse response = adherenciaAiService.detectarPatrones(idUsuario, idAdulto);

        assertNotNull(response);
        assertNull(response.getMensajeInformativo());
        assertEquals(1, response.getPatronesDetectados().size());
        assertEquals("temporal", response.getPatronesDetectados().get(0).getTipo());
        assertEquals("alta", response.getPatronesDetectados().get(0).getSeveridad());
    }

    private RegistroToma construirToma(String nombreMedicamento, String estado, LocalDateTime fecha) {
        Medicamento medicamento = new Medicamento();
        medicamento.setNombre(nombreMedicamento);

        HorarioMedicamento horario = new HorarioMedicamento();
        horario.setMedicamento(medicamento);

        RegistroToma toma = new RegistroToma();
        toma.setHorario(horario);
        toma.setEstado(estado);
        toma.setMetodoConfirmacion("app");
        toma.setFechaHoraProgramada(fecha);
        return toma;
    }
}
