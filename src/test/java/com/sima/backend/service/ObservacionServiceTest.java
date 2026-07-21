package com.sima.backend.service;

import com.sima.backend.dto.request.ObservacionRequest;
import com.sima.backend.dto.response.ObservacionResponse;
import com.sima.backend.entity.AdultoMayor;
import com.sima.backend.entity.Alerta;
import com.sima.backend.entity.ObservacionCuidador;
import com.sima.backend.entity.RelacionUsuarioAdulto;
import com.sima.backend.entity.Usuario;
import com.sima.backend.exception.UnauthorizedException;
import com.sima.backend.repository.AdultoMayorRepository;
import com.sima.backend.repository.AlertaRepository;
import com.sima.backend.repository.ObservacionCuidadorRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import com.sima.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObservacionServiceTest {

    @Mock
    private ObservacionCuidadorRepository observacionRepository;

    @Mock
    private AdultoMayorRepository adultoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RelacionUsuarioAdultoRepository relacionRepository;

    @Mock
    private AlertaRepository alertaRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AlertaAiService alertaAiService;

    private ObservacionService observacionService;

    private final Integer idUsuario = 1;
    private final Integer idAdulto = 2;

    @BeforeEach
    void setUp() {
        observacionService = new ObservacionService(observacionRepository, adultoRepository, usuarioRepository,
                relacionRepository, alertaRepository, notificationService, alertaAiService);
    }

    @Test
    void registrarObservacion_UrgenteConSugerenciaAceptada_CreaAlertaYNotifica() {
        when(relacionRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(true);

        AdultoMayor adulto = new AdultoMayor();
        adulto.setIdAdulto(idAdulto);
        when(adultoRepository.findByIdAdultoAndActivoTrue(idAdulto)).thenReturn(Optional.of(adulto));

        Usuario cuidador = new Usuario();
        cuidador.setIdUsuario(idUsuario);
        cuidador.setNombre("Carlos");
        cuidador.setApellido("Gomez");
        when(usuarioRepository.findById(idUsuario)).thenReturn(Optional.of(cuidador));

        when(observacionRepository.save(any(ObservacionCuidador.class))).thenAnswer(inv -> {
            ObservacionCuidador o = inv.getArgument(0);
            o.setIdObservacion(99);
            return o;
        });

        Usuario familiar = new Usuario();
        familiar.setIdUsuario(5);
        RelacionUsuarioAdulto relacion = new RelacionUsuarioAdulto();
        relacion.setUsuario(familiar);
        when(relacionRepository.findByAdulto_IdAdultoAndTipoRelacion(idAdulto, "familiar"))
                .thenReturn(List.of(relacion));

        ObservacionRequest request = new ObservacionRequest();
        request.setIdAdulto(idAdulto);
        request.setUrgencia("urgente");
        request.setTexto("TA muy elevada");
        request.setTensionArterial("180/110");
        request.setSugerenciaIaAceptada(true);

        ObservacionResponse response = observacionService.registrarObservacion(idUsuario, request);

        assertNotNull(response);
        verify(alertaRepository).save(argThat(a -> "urgencia_signos_vitales".equals(a.getTipoAlerta())
                && a.getAdulto().getIdAdulto().equals(idAdulto)));
        verify(notificationService).sendNotification(eq(5), anyString(), any());
    }

    @Test
    void registrarObservacion_UrgenteSinAceptarSugerencia_NoCreaAlerta() {
        when(relacionRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(true);

        AdultoMayor adulto = new AdultoMayor();
        adulto.setIdAdulto(idAdulto);
        when(adultoRepository.findByIdAdultoAndActivoTrue(idAdulto)).thenReturn(Optional.of(adulto));

        Usuario cuidador = new Usuario();
        cuidador.setIdUsuario(idUsuario);
        when(usuarioRepository.findById(idUsuario)).thenReturn(Optional.of(cuidador));

        when(observacionRepository.save(any(ObservacionCuidador.class))).thenAnswer(inv -> inv.getArgument(0));

        ObservacionRequest request = new ObservacionRequest();
        request.setIdAdulto(idAdulto);
        request.setUrgencia("urgente");
        request.setTexto("TA elevada pero cuidador no acepta sugerencia IA");
        request.setSugerenciaIaAceptada(false);

        observacionService.registrarObservacion(idUsuario, request);

        verifyNoInteractions(alertaRepository);
        verifyNoInteractions(notificationService);
    }

    @Test
    void registrarObservacion_UrgenciaNormalConSugerenciaAceptada_NoCreaAlerta() {
        when(relacionRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(true);

        AdultoMayor adulto = new AdultoMayor();
        adulto.setIdAdulto(idAdulto);
        when(adultoRepository.findByIdAdultoAndActivoTrue(idAdulto)).thenReturn(Optional.of(adulto));

        Usuario cuidador = new Usuario();
        cuidador.setIdUsuario(idUsuario);
        when(usuarioRepository.findById(idUsuario)).thenReturn(Optional.of(cuidador));

        when(observacionRepository.save(any(ObservacionCuidador.class))).thenAnswer(inv -> inv.getArgument(0));

        ObservacionRequest request = new ObservacionRequest();
        request.setIdAdulto(idAdulto);
        request.setUrgencia("normal");
        request.setTexto("Todo bien");
        request.setSugerenciaIaAceptada(true);

        observacionService.registrarObservacion(idUsuario, request);

        verifyNoInteractions(alertaRepository);
        verifyNoInteractions(notificationService);
    }

    @Test
    void registrarObservacion_UsuarioSinAcceso_LanzaException() {
        when(relacionRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)).thenReturn(false);

        ObservacionRequest request = new ObservacionRequest();
        request.setIdAdulto(idAdulto);
        request.setUrgencia("normal");
        request.setTexto("texto");

        assertThrows(UnauthorizedException.class, () -> observacionService.registrarObservacion(idUsuario, request));
        verifyNoInteractions(observacionRepository);
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
