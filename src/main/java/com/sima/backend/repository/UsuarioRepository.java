package com.sima.backend.repository;

import com.sima.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    // Buscar por correo (login)
    Optional<Usuario> findByCorreo(String correo);

    // Verificar si correo ya existe (validación en registro)
    boolean existsByCorreo(String correo);

    // Listar usuarios activos por rol
    List<Usuario> findByRol_NombreRolAndActivoTrue(String nombreRol);

    // Listar todos los usuarios activos
    List<Usuario> findByActivoTrue();

    // Buscar por wechat_openid (notificaciones)
    Optional<Usuario> findByWechatOpenid(String wechatOpenid);

    // Actualizar último acceso al hacer login
    @Modifying
    @Query("UPDATE Usuario u SET u.ultimoAcceso = :fecha WHERE u.idUsuario = :id")
    void actualizarUltimoAcceso(@Param("id") Integer idUsuario,
            @Param("fecha") LocalDateTime fecha);

    // Desactivar usuario (soft delete)
    @Modifying
    @Query("UPDATE Usuario u SET u.activo = false WHERE u.idUsuario = :id")
    void desactivarUsuario(@Param("id") Integer idUsuario);

    // Activar/Reactivar usuario
    @Modifying
    @Query("UPDATE Usuario u SET u.activo = true WHERE u.idUsuario = :id")
    void activarUsuario(@Param("id") Integer idUsuario);
}