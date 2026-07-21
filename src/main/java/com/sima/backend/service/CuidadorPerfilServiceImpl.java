package com.sima.backend.service;

import com.sima.backend.dto.request.ActualizarDatosContactoCuidadorRequest;
import com.sima.backend.dto.response.DatosContactoCuidadorResponse;
import com.sima.backend.entity.PerfilCuidador;
import com.sima.backend.entity.Usuario;
import com.sima.backend.exception.BadRequestException;
import com.sima.backend.exception.ResourceNotFoundException;
import com.sima.backend.repository.PerfilCuidadorRepository;
import com.sima.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CuidadorPerfilServiceImpl implements CuidadorPerfilService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilCuidadorRepository perfilCuidadorRepository;

    public CuidadorPerfilServiceImpl(UsuarioRepository usuarioRepository,
            PerfilCuidadorRepository perfilCuidadorRepository) {
        this.usuarioRepository = usuarioRepository;
        this.perfilCuidadorRepository = perfilCuidadorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DatosContactoCuidadorResponse obtenerPerfil(Integer idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "idUsuario", idUsuario));
        PerfilCuidador perfil = perfilCuidadorRepository.findByIdUsuario(idUsuario).orElse(null);
        return DatosContactoCuidadorResponse.from(usuario, perfil);
    }

    @Override
    @Transactional
    public DatosContactoCuidadorResponse actualizarPerfil(Integer idUsuario, ActualizarDatosContactoCuidadorRequest request) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "idUsuario", idUsuario));

        String nuevoCorreo = request.getCorreo();
        usuarioRepository.findByCorreo(nuevoCorreo).ifPresent(otro -> {
            if (!otro.getIdUsuario().equals(idUsuario)) {
                throw new BadRequestException("El correo ya está registrado por otro usuario");
            }
        });

        usuario.setCorreo(nuevoCorreo);
        usuarioRepository.save(usuario);

        PerfilCuidador perfil = perfilCuidadorRepository.findByIdUsuario(idUsuario)
                .orElseGet(() -> new PerfilCuidador(idUsuario));

        perfil.setTelefono(request.getTelefono());
        perfil.setCiudad(request.getCiudad());
        perfil.setTarifaHora(request.getTarifaHora());
        perfil.setDisponibilidad(request.getDisponibilidad());

        perfilCuidadorRepository.save(perfil);

        return DatosContactoCuidadorResponse.from(usuario, perfil);
    }
}
