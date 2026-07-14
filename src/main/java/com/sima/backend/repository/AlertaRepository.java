package com.sima.backend.repository;

import com.sima.backend.entity.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Integer> {

    // Alertas activas (no resueltas) de un adulto
    List<Alerta> findByAdulto_IdAdultoAndResueltaFalseOrderByCreadoEnDesc(Integer idAdulto);

    // Todas las alertas activas del sistema (para el admin)
    List<Alerta> findByResueltaFalseOrderByCreadoEnDesc();

    // Historial de alertas de un adulto
    List<Alerta> findByAdulto_IdAdultoOrderByCreadoEnDesc(Integer idAdulto);

    // Historial de alertas de un adulto en un rango de fechas
    @Query("""
            SELECT a FROM Alerta a
            WHERE a.adulto.idAdulto = :idAdulto
              AND a.creadoEn BETWEEN :desde AND :hasta
            ORDER BY a.creadoEn DESC
            """)
    List<Alerta> findByAdultoAndRango(@Param("idAdulto") Integer idAdulto,
                                     @Param("desde") java.time.LocalDateTime desde,
                                     @Param("hasta") java.time.LocalDateTime hasta);

    // Alertas activas por tipo para un adulto
    List<Alerta> findByAdulto_IdAdultoAndTipoAlertaAndResueltaFalse(
            Integer idAdulto, String tipoAlerta);

    // Verificar si ya existe una alerta activa del mismo tipo para ese adulto
    // (evita duplicados por el mismo evento)
    boolean existsByAdulto_IdAdultoAndTipoAlertaAndResueltaFalse(
            Integer idAdulto, String tipoAlerta);

    // Verificar si ya existe alerta activa para un registro de toma específico
    boolean existsByRegistro_IdRegistroAndResueltaFalse(Integer idRegistro);

    // Marcar alerta como resuelta
    @Modifying
    @Query("UPDATE Alerta a SET a.resuelta = true WHERE a.idAlerta = :id")
    void resolverAlerta(@Param("id") Integer idAlerta);

    // Contar alertas activas de un adulto
    long countByAdulto_IdAdultoAndResueltaFalse(Integer idAdulto);
}