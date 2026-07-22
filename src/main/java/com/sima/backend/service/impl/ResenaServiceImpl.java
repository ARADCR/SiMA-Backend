package com.sima.backend.service.impl;

import com.sima.backend.dto.request.CrearResenaRequest;
import com.sima.backend.dto.response.ResenaResponse;
import com.sima.backend.entity.Resena;
import com.sima.backend.entity.Usuario;
import com.sima.backend.exception.BadRequestException;
import com.sima.backend.exception.ResourceNotFoundException;
import com.sima.backend.repository.ResenaRepository;
import com.sima.backend.repository.UsuarioRepository;
import com.sima.backend.service.ResenaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResenaServiceImpl implements ResenaService {

    private final ResenaRepository resenaRepository;
    private final UsuarioRepository usuarioRepository;

    public ResenaServiceImpl(ResenaRepository resenaRepository, UsuarioRepository usuarioRepository) {
        this.resenaRepository = resenaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public ResenaResponse crearResena(Integer idFamiliar, CrearResenaRequest request) {
        if (resenaRepository.existsByCuidador_IdUsuarioAndFamiliar_IdUsuario(request.getIdCuidador(), idFamiliar)) {
            throw new BadRequestException("Ya dejaste una reseña para este cuidador");
        }

        Usuario familiar = usuarioRepository.findById(idFamiliar)
                .orElseThrow(() -> new ResourceNotFoundException("Familiar", "id", idFamiliar));
        
        Usuario cuidador = usuarioRepository.findById(request.getIdCuidador())
                .orElseThrow(() -> new ResourceNotFoundException("Cuidador", "id", request.getIdCuidador()));

        Resena resena = new Resena();
        resena.setCuidador(cuidador);
        resena.setFamiliar(familiar);
        resena.setPuntos(request.getPuntos());
        resena.setTexto(request.getTexto());
        resena.setFechaCreacion(LocalDateTime.now());

        resena = resenaRepository.save(resena);
        return ResenaResponse.from(resena);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResenaResponse> obtenerResenasCuidador(Integer idCuidador) {
        return resenaRepository.findByCuidador_IdUsuarioOrderByFechaCreacionDesc(idCuidador)
                .stream()
                .map(ResenaResponse::from)
                .collect(Collectors.toList());
    }
}
