package com.sima.backend.service.impl;

import com.sima.backend.dto.request.CrearCredencialRequest;
import com.sima.backend.dto.response.CredencialResponse;
import com.sima.backend.entity.CredencialCuidador;
import com.sima.backend.entity.Usuario;
import com.sima.backend.repository.CredencialCuidadorRepository;
import com.sima.backend.repository.UsuarioRepository;
import com.sima.backend.service.CredencialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CredencialServiceImpl implements CredencialService {

    private final CredencialCuidadorRepository credencialRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    public List<CredencialResponse> obtenerCredencialesPorCuidador(Integer idCuidador) {
        return credencialRepository.findByCuidadorIdUsuarioOrderByFechaSubidaDesc(idCuidador)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CredencialResponse crearCredencial(Integer idCuidador, CrearCredencialRequest request) {
        Usuario cuidador = usuarioRepository.findById(idCuidador)
                .orElseThrow(() -> new RuntimeException("Cuidador no encontrado"));

        CredencialCuidador credencial = new CredencialCuidador();
        credencial.setCuidador(cuidador);
        credencial.setTipo(request.getTipo());
        credencial.setNombre(request.getNombre());
        credencial.setFechaSubida(LocalDateTime.now());
        credencial.setEstado("pendiente");
        
        // Si el frontend envía un nombre de archivo falso, lo usamos, si no ponemos uno genérico
        String url = request.getArchivoFalsoNombre() != null && !request.getArchivoFalsoNombre().isEmpty() 
                ? request.getArchivoFalsoNombre() 
                : "documento-adjunto.pdf";
        credencial.setArchivoUrl(url);

        CredencialCuidador guardada = credencialRepository.save(credencial);
        return mapToResponse(guardada);
    }

    private CredencialResponse mapToResponse(CredencialCuidador credencial) {
        CredencialResponse response = new CredencialResponse();
        response.setId(credencial.getIdCredencial());
        response.setTipo(credencial.getTipo());
        response.setNombre(credencial.getNombre());
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        response.setFecha(credencial.getFechaSubida().format(formatter));
        
        response.setEstado(credencial.getEstado());
        response.setArchivoUrl(credencial.getArchivoUrl());
        return response;
    }
}
