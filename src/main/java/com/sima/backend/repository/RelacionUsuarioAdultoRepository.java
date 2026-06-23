package com.sima.backend.repository;

import com.sima.backend.entity.RelacionUsuarioAdulto;
import com.sima.backend.entity.RelacionUsuarioAdulto.RelacionUsuarioAdultoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RelacionUsuarioAdultoRepository
        extends JpaRepository<RelacionUsuarioAdulto, RelacionUsuarioAdultoId> {

    // Verificar si ya existe la relación entre usuario y adulto
    boolean existsByUsuario_IdUsuarioAndAdulto_IdAdulto(Integer idUsuario, Integer idAdulto);

    // Obtener la relación específica entre un usuario y un adulto
    Optional<RelacionUsuarioAdulto> findByUsuario_IdUsuarioAndAdulto_IdAdulto(
            Integer idUsuario, Integer idAdulto);

    // Listar todas las relaciones de un usuario
    List<RelacionUsuarioAdulto> findByUsuario_IdUsuario(Integer idUsuario);

    // Listar todos los contactos de emergencia de un adulto mayor
    @Query("""
            SELECT r FROM RelacionUsuarioAdulto r
            WHERE r.adulto.idAdulto = :idAdulto
              AND r.esContactoEmergencia = true
            """)
    List<RelacionUsuarioAdulto> findContactosEmergencia(@Param("idAdulto") Integer idAdulto);

    // Listar relaciones por adulto y tipo
    List<RelacionUsuarioAdulto> findByAdulto_IdAdultoAndTipoRelacion(
            Integer idAdulto, String tipoRelacion);

    // Validar que un usuario tenga acceso a un adulto específico
    @Query("""
            SELECT COUNT(r) > 0 FROM RelacionUsuarioAdulto r
            WHERE r.usuario.idUsuario = :idUsuario
              AND r.adulto.idAdulto = :idAdulto
            """)
    boolean validarAccesoUsuarioAdulto(@Param("idUsuario") Integer idUsuario,
            @Param("idAdulto") Integer idAdulto);
}