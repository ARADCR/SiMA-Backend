package com.sima.backend.service;

import com.sima.backend.dto.request.AsignarDispositivoRequest;
import com.sima.backend.dto.request.DispositivoIotRequest;
import com.sima.backend.dto.response.DispositivoIotResponse;
import com.sima.backend.entity.AdultoMayor;
import com.sima.backend.entity.DispositivoIot;
import com.sima.backend.exception.BadRequestException;
import com.sima.backend.repository.AdultoMayorRepository;
import com.sima.backend.repository.DispositivoIotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DispositivoIotServiceTest {

    @Mock
    private DispositivoIotRepository dispositivoRepository;

    @Mock
    private AdultoMayorRepository adultoMayorRepository;

    @InjectMocks
    private DispositivoIotService dispositivoService;

    private AdultoMayor adulto;
    private DispositivoIot dispositivo;

    @BeforeEach
    void setUp() {
        adulto = new AdultoMayor();
        adulto.setIdAdulto(1);
        adulto.setActivo(true);

        dispositivo = new DispositivoIot();
        dispositivo.setIdDispositivo(10);
        dispositivo.setIdentificadorFisico("MAC-12345");
        dispositivo.setTipoDispositivo("pastillero_esp32");
        dispositivo.setAdulto(adulto);
        dispositivo.setActivo(true);
    }

    @Test
    void registrar_MacDuplicada_LanzaException() {
        DispositivoIotRequest req = new DispositivoIotRequest();
        req.setIdentificadorFisico("MAC-12345");
        req.setTipoDispositivo("pastillero_esp32");

        when(dispositivoRepository.existsByIdentificadorFisico(anyString())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> dispositivoService.registrar(req));
        verify(dispositivoRepository, never()).save(any(DispositivoIot.class));
    }

    @Test
    void asignar_AdultoYaTieneMismoTipo_LanzaException() {
        when(dispositivoRepository.findById(10)).thenReturn(Optional.of(dispositivo));
        when(adultoMayorRepository.findByIdAdultoAndActivoTrue(2)).thenReturn(Optional.of(adulto)); // Simulamos que es otro adulto
        
        // Simulamos que el adulto 2 ya tiene un pastillero
        when(dispositivoRepository.findByAdulto_IdAdultoAndTipoDispositivo(2, "pastillero_esp32"))
                .thenReturn(Optional.of(new DispositivoIot()));

        AsignarDispositivoRequest req = new AsignarDispositivoRequest();
        req.setIdAdulto(2);
        assertThrows(BadRequestException.class, () -> dispositivoService.asignar(10, req));
    }

    @Test
    void desasignar_LimpiaIdAdulto() {
        when(dispositivoRepository.existsById(10)).thenReturn(true);
        when(dispositivoRepository.findById(10)).thenReturn(Optional.of(dispositivo));
        // En el service se llama directamente a desasignarDispositivo
        doNothing().when(dispositivoRepository).desasignarDispositivo(10);
        
        dispositivoService.desasignar(10);
        
        verify(dispositivoRepository, times(1)).desasignarDispositivo(10);
    }
}
