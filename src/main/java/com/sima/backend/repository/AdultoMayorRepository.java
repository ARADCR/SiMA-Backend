package com.sima.backend.repository;

import com.sima.backend.entity.AdultoMayor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdultoMayorRepository extends JpaRepository<AdultoMayor, Integer> {

    // Listar adultos activos
    List<AdultoMayor> findByActivoTrue();

    // Buscar adulto activo por ID (para validaciones)
    Optional<AdultoMayor> findByIdAdultoAndActivoTrue(Integer idAdulto);

    // Obtener todos los adultos vinculados a un usuario (familiar/cuidador)
    @Query("""
            SELECT am FROM AdultoMayor am
            JOIN am.relaciones r
            WHERE r.usuario.idUsuario = :idUsuario
              AND am.activo = true
            """)
    List<AdultoMayor> findByUsuarioId(@Param("idUsuario") Integer idUsuario);

    // Obtener adultos de un usuario filtrado por tipo de relación
    @Query("""
            SELECT am FROM AdultoMayor am
            JOIN am.relaciones r
            WHERE r.usuario.idUsuario = :idUsuario
              AND r.tipoRelacion = :tipoRelacion
              AND am.activo = true
            """)
    List<AdultoMayor> findByUsuarioIdAndTipoRelacion(@Param("idUsuario") Integer idUsuario,
            @Param("tipoRelacion") String tipoRelacion);

    // Listar todos los adultos con relaciones (eager fetch para admin)
    @Query("""
            SELECT DISTINCT am FROM AdultoMayor am
            LEFT JOIN FETCH am.relaciones r
            LEFT JOIN FETCH r.usuario
            """)
    List<AdultoMayor> findAllWithRelaciones();

    // Desactivar adulto (soft delete)
    @Modifying
    @Query("UPDATE AdultoMayor am SET am.activo = false WHERE am.idAdulto = :id")
    void desactivarAdulto(@Param("id") Integer idAdulto);
}