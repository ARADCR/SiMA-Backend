package com.sima.backend.repository;

import com.sima.backend.entity.AuditoriaLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditoriaLogRepository extends JpaRepository<AuditoriaLog, Integer> {

    /*
     * Repositorio de solo escritura en la práctica.
     * No se exponen métodos de UPDATE ni DELETE.
     * Solo se usa save() para INSERT y consultas de lectura.
     */

    // Historial de acciones de un usuario
    List<AuditoriaLog> findByUsuario_IdUsuarioOrderByFechaHoraDesc(Integer idUsuario);

    // Historial de cambios sobre una tabla específica
    List<AuditoriaLog> findByTablaAfectadaOrderByFechaHoraDesc(String tablaAfectada);

    // Logs en un rango de fechas
    @Query("""
            SELECT al FROM AuditoriaLog al
            WHERE al.fechaHora BETWEEN :desde AND :hasta
            ORDER BY al.fechaHora DESC
            """)
    List<AuditoriaLog> findByRangoFechas(@Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    // Logs de un usuario sobre una tabla específica
    List<AuditoriaLog> findByUsuario_IdUsuarioAndTablaAfectadaOrderByFechaHoraDesc(
            Integer idUsuario, String tablaAfectada);
}