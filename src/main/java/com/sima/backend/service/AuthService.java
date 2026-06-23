package com.sima.backend.service;

import com.sima.backend.dto.request.LoginRequest;
import com.sima.backend.dto.response.LoginResponse;
import com.sima.backend.entity.Usuario;
import com.sima.backend.repository.UsuarioRepository;
import com.sima.backend.security.CustomUserDetails;
import com.sima.backend.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;

    public AuthService(AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // Spring Security autentica y lanza BadCredentialsException si falla
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getCorreo(), request.getPassword()));

        // Generar token JWT
        String token = jwtTokenProvider.generarToken(authentication);

        // Actualizar último acceso
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        usuarioRepository.actualizarUltimoAcceso(
                userDetails.getIdUsuario(), LocalDateTime.now());

        // Obtener datos del usuario para la respuesta
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo()).orElseThrow();

        return new LoginResponse(
                token,
                "Bearer",
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                usuario.getRol().getNombreRol());
    }
}