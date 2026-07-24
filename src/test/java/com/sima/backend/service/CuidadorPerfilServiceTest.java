package com.sima.backend.service;

import com.sima.backend.dto.request.ActualizarDatosContactoCuidadorRequest;
import com.sima.backend.dto.response.DatosContactoCuidadorResponse;
import com.sima.backend.entity.PerfilCuidador;
import com.sima.backend.entity.Usuario;
import com.sima.backend.exception.BadRequestException;
import com.sima.backend.exception.ResourceNotFoundException;
import com.sima.backend.repository.PerfilCuidadorRepository;
import com.sima.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CuidadorPerfilServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilCuidadorRepository perfilCuidadorRepository;

    @InjectMocks
    private CuidadorPerfilServiceImpl cuidadorPerfilService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setCorreo("cuidador@sima.com");
    }

    @Test
    void actualizarPerfil_CorreoDeOtroUsuario_LanzaException() {
        Usuario otroUsuario = new Usuario();
        otroUsuario.setIdUsuario(2);
        otroUsuario.setCorreo("nuevo@sima.com");

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByCorreo("nuevo@sima.com")).thenReturn(Optional.of(otroUsuario));

        ActualizarDatosContactoCuidadorRequest request = new ActualizarDatosContactoCuidadorRequest();
        request.setCorreo("nuevo@sima.com");

        assertThrows(BadRequestException.class,
                () -> cuidadorPerfilService.actualizarPerfil(1, request));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void actualizarPerfil_MismoCorreoPropio_ActualizaSinError() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByCorreo("cuidador@sima.com")).thenReturn(Optional.of(usuario));
        when(perfilCuidadorRepository.findByIdUsuario(1)).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(perfilCuidadorRepository.save(any(PerfilCuidador.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarDatosContactoCuidadorRequest request = new ActualizarDatosContactoCuidadorRequest();
        request.setCorreo("cuidador@sima.com");
        request.setTelefono("123456");

        DatosContactoCuidadorResponse response = cuidadorPerfilService.actualizarPerfil(1, request);

        assertEquals("cuidador@sima.com", response.getCorreo());
        assertEquals("123456", response.getTelefono());
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
        verify(perfilCuidadorRepository, times(1)).save(any(PerfilCuidador.class));
    }

    @Test
    void actualizarPerfil_PerfilNoExisteAun_LoCreaAlPrimerPut() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByCorreo("cuidador@sima.com")).thenReturn(Optional.of(usuario));
        when(perfilCuidadorRepository.findByIdUsuario(1)).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(perfilCuidadorRepository.save(any(PerfilCuidador.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarDatosContactoCuidadorRequest request = new ActualizarDatosContactoCuidadorRequest();
        request.setCorreo("cuidador@sima.com");
        request.setTarifaHora(new BigDecimal("25.50"));

        assertDoesNotThrow(() -> cuidadorPerfilService.actualizarPerfil(1, request));
        verify(perfilCuidadorRepository, times(1)).save(any(PerfilCuidador.class));
    }

    @Test
    void obtenerPerfil_UsuarioInexistente_LanzaResourceNotFound() {
        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cuidadorPerfilService.obtenerPerfil(99));
    }
}
