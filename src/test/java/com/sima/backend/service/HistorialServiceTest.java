package com.sima.backend.service;

import com.sima.backend.dto.response.HistorialEventoResponse;
import com.sima.backend.entity.AdultoMayor;
import com.sima.backend.entity.Alerta;
import com.sima.backend.entity.HorarioMedicamento;
import com.sima.backend.entity.Medicamento;
import com.sima.backend.entity.RegistroToma;
import com.sima.backend.exception.UnauthorizedException;
import com.sima.backend.repository.AlertaRepository;
import com.sima.backend.repository.EventoIotRepository;
import com.sima.backend.repository.RegistroTomaRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HistorialServiceTest {

    @Mock
    private RegistroTomaRepository registroRepository;

    @Mock
    private AlertaRepository alertaRepository;

    @Mock
    private EventoIotRepository eventoIotRepository;

    @Mock
    private RelacionUsuarioAdultoRepository relacionRepository;

    @Mock
    private com.sima.backend.repository.HorarioMedicamentoRepository horarioRepository;

    @InjectMocks
    private HistorialService historialService;

    private Integer idUsuario = 1;
    private Integer idAdulto = 2;
    private Pageable pageable = PageRequest.of(0, 10);

    @Test
    void obtenerHistorial_UsuarioSinAcceso_LanzaException() {
        when(relacionRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> {
            historialService.obtenerHistorial(idUsuario, idAdulto, null, null, null, pageable);
        });
    }

    @Test
    void obtenerHistorial_UsuarioConAcceso_RetornaEventosOrdenados() {
        when(relacionRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(true);

        AdultoMayor adulto = new AdultoMayor();
        adulto.setIdAdulto(idAdulto);

        // Mock de toma
        Medicamento med = new Medicamento();
        med.setNombre("Paracetamol");
        med.setDosis("500mg");
        HorarioMedicamento hm = new HorarioMedicamento();
        hm.setMedicamento(med);

        RegistroToma toma = new RegistroToma();
        toma.setIdRegistro(101);
        toma.setAdulto(adulto);
        toma.setHorario(hm);
        toma.setEstado("tomado");
        toma.setFechaHoraProgramada(LocalDateTime.now().minusHours(2));

        // Mock de alerta
        Alerta alerta = new Alerta();
        alerta.setIdAlerta(201);
        alerta.setAdulto(adulto);
        alerta.setTipoAlerta("omision_medicacion");
        alerta.setMensaje("Se omitió toma");
        alerta.setCreadoEn(LocalDateTime.now().minusHours(1));
        alerta.setResuelta(false);

        when(registroRepository.findTomasDelDia(eq(idAdulto), any(), any()))
                .thenReturn(List.of(toma));
        when(registroRepository.findHistorialByAdultoAndRango(eq(idAdulto), any(), any()))
                .thenReturn(List.of(toma));
        when(alertaRepository.findByAdultoAndRango(eq(idAdulto), any(), any()))
                .thenReturn(List.of(alerta));
        when(eventoIotRepository.findByAdultoAndRango(eq(idAdulto), any(), any()))
                .thenReturn(Collections.emptyList());

        Page<HistorialEventoResponse> res = historialService.obtenerHistorial(
                idUsuario, idAdulto, null, null, null, pageable);

        assertNotNull(res);
        assertEquals(2, res.getTotalElements());
        
        // El primer elemento debe ser la alerta (más reciente, hace 1 hora)
        assertEquals("alerta", res.getContent().get(0).getTipo());
        // El segundo la toma (hace 2 horas)
        assertEquals("toma", res.getContent().get(1).getTipo());
    }
}
