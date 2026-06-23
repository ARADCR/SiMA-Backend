package com.sima.backend.service;

import com.sima.backend.dto.request.UsuarioCreateRequest;
import com.sima.backend.dto.request.UsuarioUpdateRequest;
import com.sima.backend.dto.response.UsuarioResponse;
import com.sima.backend.entity.Rol;
import com.sima.backend.entity.Usuario;
import com.sima.backend.exception.BadRequestException;
import com.sima.backend.exception.ResourceNotFoundException;
import com.sima.backend.repository.RolRepository;
import com.sima.backend.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * HU-17: Crear, editar y desactivar cuentas de familiares/cuidadores.
 * Solo el Administrador puede ejecutar estas operaciones.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Listar todos los usuarios activos
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarActivos() {
        return usuarioRepository.findByActivoTrue()
                .stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    // Buscar un usuario por ID
    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Integer idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario", "id", idUsuario));
        return UsuarioResponse.from(usuario);
    }

    // Crear nuevo usuario (HU-17)
    @Transactional
    public UsuarioResponse crear(UsuarioCreateRequest request) {
        // Validar correo único
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new BadRequestException(
                    "Ya existe un usuario con el correo: " + request.getCorreo());
        }

        // Validar que el rol exista
        Rol rol = rolRepository.findById(request.getIdRol())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rol", "id", request.getIdRol()));

        // Construir entidad
        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setCorreo(request.getCorreo());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(rol);
        usuario.setWechatOpenid(request.getWechatOpenid());
        usuario.setActivo(true);

        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    // Editar usuario (HU-17)
    @Transactional
    public UsuarioResponse actualizar(Integer idUsuario, UsuarioUpdateRequest request) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario", "id", idUsuario));

        // Validar correo único si cambió
        if (!usuario.getCorreo().equals(request.getCorreo()) &&
                usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new BadRequestException(
                    "Ya existe un usuario con el correo: " + request.getCorreo());
        }

        // Actualizar campos
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setCorreo(request.getCorreo());
        usuario.setWechatOpenid(request.getWechatOpenid());

        // Solo actualizar contraseña si viene en el request
        if (StringUtils.hasText(request.getPassword())) {
            usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        // Solo actualizar rol si viene en el request
        if (request.getIdRol() != null) {
            Rol rol = rolRepository.findById(request.getIdRol())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Rol", "id", request.getIdRol()));
            usuario.setRol(rol);
        }

        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    // Desactivar usuario - soft delete (HU-17)
    @Transactional
    public void desactivar(Integer idUsuario) {
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new ResourceNotFoundException("Usuario", "id", idUsuario);
        }
        usuarioRepository.desactivarUsuario(idUsuario);
    }
}